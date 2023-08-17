package es.upv.computervision.frameprocessing;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TestProcessor implements FrameProcessor {

    public TestProcessor() {
    }

    @Override
    public void process(Mat frame) {
        test1(frame);
    }

    private void test1(Mat frame) {
        // Convert the frame to the HSV color space
        Mat hsv = new Mat();
        Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_BGR2HSV);

        // Define the lower and upper boundaries of the "white" light in the HSV color space
        int sensitivity = 15;
        Scalar whiteLower = new Scalar(0, 0, 255 - sensitivity);
        Scalar whiteUpper = new Scalar(255, sensitivity, 255);

        // Create a binary mask for the white lights
        Mat mask = new Mat();
        Core.inRange(hsv, whiteLower, whiteUpper, mask);

        // Split the HSV image into separate channels
        List<Mat> hsvChannels = new ArrayList<>();
        Core.split(hsv, hsvChannels);

        // Apply brightness thresholding
        Mat brightness = hsvChannels.get(2); // Value channel
        double threshold = 245; // Adjust this threshold as needed
        Mat thresholdedBrightness = new Mat();
        Imgproc.threshold(brightness, thresholdedBrightness, threshold, 255, Imgproc.THRESH_BINARY);

        // Combine the color segmentation mask and the brightness threshold mask
        Mat finalMask = new Mat();
        Core.bitwise_and(mask, thresholdedBrightness, finalMask);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(finalMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        MatOfPoint2f[] contoursPoly = new MatOfPoint2f[contours.size()];
        Rect[] boundRect = new Rect[contours.size()];
        Point[] centers = new Point[contours.size()];
        float[][] radius = new float[contours.size()][1];
        for (int i = 0; i < contours.size(); i++) {
            contoursPoly[i] = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
            boundRect[i] = Imgproc.boundingRect(new MatOfPoint(contoursPoly[i].toArray()));
            centers[i] = new Point();
            Imgproc.minEnclosingCircle(contoursPoly[i], centers[i], radius[i]);
        }

        List<MatOfPoint> contoursPolyList = new ArrayList<>(contoursPoly.length);
        for (MatOfPoint2f poly : contoursPoly) {
            contoursPolyList.add(new MatOfPoint(poly.toArray()));
        }
        for (int i = 0; i < contours.size(); i++) {
            Scalar color = new Scalar(rng(), rng(), rng());
            Imgproc.drawContours(frame, contoursPolyList, i, color);
            Imgproc.rectangle(frame, boundRect[i].tl(), boundRect[i].br(), color, 2);
            Imgproc.circle(frame, centers[i], (int) radius[i][0], color, 2);
        }
    }

    private void test2(Mat frame) {
        // Convert the image to the HSL color space
        Mat hslImage = new Mat();
        Imgproc.cvtColor(frame, hslImage, Imgproc.COLOR_BGR2HLS);

        // Split the HSL image into separate channels
        List<Mat> hslChannels = new ArrayList<>();
        Core.split(hslImage, hslChannels);

        // Apply a threshold to the lightness channel
        Mat lightness = hslChannels.get(1); // Lightness channel
        double threshold = 250; // Adjust this threshold as needed
        Imgproc.threshold(lightness, frame, threshold, 255, Imgproc.THRESH_BINARY);
    }

    private int rng() { // Just to make code easier to read
        return ThreadLocalRandom.current().nextInt(0, 256);
    }
}
