package es.upv.computervision.frameprocessing;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LKOpticalFlowProcessor implements FrameProcessor {
    private final float mergeThreshold; // distance below which two tracked points are merged
    private final float discardThreshold; // distance below which a point is considered not moving compared to the previous point
    private final int newPointsRate; // number of frames between every search of new tracking points
    private final int nbPoints; // max number of simultaneous tracking points
    private final List<Scalar> colors = new ArrayList<>();
    private Mat previousGray;
    private List<Point> previousMergedPoints = new ArrayList<>();
    private MatOfPoint2f previousPoints;
    private Mat mask;
    private int frameCount;
    private int[] frameCountPerPoints;

    public LKOpticalFlowProcessor(int nbPoints, int newPointsRate, float discardThreshold, float mergeThreshold){
        this.nbPoints = nbPoints; // 15
        this.newPointsRate = newPointsRate; // 20
        this.discardThreshold = discardThreshold; // 3
        this.mergeThreshold = mergeThreshold; // 40
    }

    private void initialize(Mat frame) {
        previousGray = new Mat();
        Imgproc.cvtColor(frame, previousGray, Imgproc.COLOR_BGR2GRAY);

        // Generate random colors and store them in the list
        for (int i = 0; i < nbPoints; i++) {
            int r = ThreadLocalRandom.current().nextInt(0, 256);
            int g = ThreadLocalRandom.current().nextInt(0, 256);
            int b = ThreadLocalRandom.current().nextInt(0, 256);
            Scalar color = new Scalar(r, g, b);
            colors.add(color);
        }

        // Create a mask image for drawing purposes
        mask = Mat.zeros(frame.size(), frame.type());

        // Find corners in the first frame
        MatOfPoint corners = new MatOfPoint();
        Imgproc.goodFeaturesToTrack(previousGray, corners, nbPoints, 0.3, 7.0);
        previousPoints = new MatOfPoint2f(corners.toArray());

        // Initialize the list of frame counts
        frameCountPerPoints = new int[nbPoints];
        Arrays.fill(frameCountPerPoints, 1);
    }

    @Override
    public void process(Mat frame) {
        frameCount++;

        if (frameCount == 1) {
            initialize(frame);
            return;
        }

        // Convert the current frame to grayscale
        Mat currentGray = new Mat();
        Imgproc.cvtColor(frame, currentGray, Imgproc.COLOR_BGR2GRAY);

        // Calculate the optical flow
        MatOfPoint2f currentPoints = new MatOfPoint2f();
        MatOfByte status = new MatOfByte();
        MatOfFloat err = new MatOfFloat();
        TermCriteria criteria = new TermCriteria(TermCriteria.COUNT + TermCriteria.EPS, 10, 0.03);
        Video.calcOpticalFlowPyrLK(previousGray, currentGray, previousPoints, currentPoints, status, err, new Size(15, 15), 2, criteria);


        List<Point> mergedPoints = new ArrayList<>();
        // Track points in mostly one direction and find new ones
        List<Point> goodPoints = new ArrayList<>();
        for (int i = 0; i < previousPoints.rows(); i++) {
            if (status.get(i, 0)[0] != 1) {
                frameCountPerPoints[i] = 1; // Stop tracking point
                continue;
            }

            Point pt0 = new Point(previousPoints.get(i, 0));
            Point pt1 = new Point(currentPoints.get(i, 0));

            // Calculate Euclidean distance between point and merged point
            double distance = Math.sqrt(Math.pow(pt1.x - pt0.x, 2) + Math.pow(pt1.y - pt0.y, 2));

            // Calculate the displacement vector
            double dx = pt1.x - pt0.x;
            double dy = pt1.y - pt0.y;

            if (frameCountPerPoints[i] > 1) {
                // Check if point is going 2 times more vertically than horizontally
                // Or if it's going "up" (dy < 0)
                // Or if it hasn't moved
                if (Math.abs(dx) * 0.5 > Math.abs(dy) || dy < 0 || distance < discardThreshold) {
                    frameCountPerPoints[i] = 1; // Stop tracking point
                    continue;
                }

                // Draw the track line and point
                Imgproc.line(mask, pt1, pt0, colors.get(i), 1);
                Imgproc.circle(frame, pt1, 5, colors.get(i), -1);

                // Merge similar points using ICP algorithm
                boolean merged = false;

                // Compare the current point with the points in the merged list
                for (Point mP : mergedPoints) {
                    // Calculate Euclidean distance between current and merged point
                    double distanceToMerged = Math.sqrt(Math.pow(pt1.x - mP.x, 2) + Math.pow(pt1.y - mP.y, 2));
                    if (distanceToMerged < mergeThreshold) {
                        // Merge the similar points together
                        mP = new Point((pt1.x + mP.x) / 2.0, (pt1.y + mP.y) / 2.0);
                        merged = true;
                        break;
                    }
                }

                // Add the current point as a new point if it is not similar to any existing points
                if (!merged) {
                    mergedPoints.add(pt1);
                }
            }

            goodPoints.add(pt1);
            frameCountPerPoints[i]++;
        }

        // Draw merged points
        for (int i = 0; i < mergedPoints.size(); i++) {
            Point cur = mergedPoints.get(i);
            Imgproc.circle(frame, cur, Math.round(mergeThreshold), new Scalar(0, 255, 0), 2);
            if(previousMergedPoints.size() > i) {
                Point prev = previousMergedPoints.get(i);
                Imgproc.line(mask, cur, prev, new Scalar(255, 0, 0), 4);
            }
        }

        previousMergedPoints = mergedPoints;

        // Every x frames, re-add tracking points
        if (frameCount % newPointsRate == 0) {
            // Find new corners to replace "untracked" points
            MatOfPoint corners = new MatOfPoint();
            Imgproc.goodFeaturesToTrack(currentGray, corners, nbPoints - goodPoints.size(), 0.3, 7.0);
            goodPoints.addAll(corners.toList());
        }

        if (frameCount % 120 == 0) {
            // Refresh mask to "erase" old traces
            mask = Mat.zeros(frame.size(), frame.type());
        }

        // Add the mask to the result image
        Core.add(frame, mask, frame);

        // Update the previous frame and points
        previousGray = currentGray.clone();
        previousPoints.fromList(goodPoints);
    }
}
