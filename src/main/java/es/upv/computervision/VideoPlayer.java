package es.upv.computervision;

import es.upv.computervision.frameprocessing.FrameProcessor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

public class VideoPlayer {
    private final VideoCapture videoCapture;
    private final Mat frame;
    private final Runnable readVideo;
    private boolean playing = false;

    public VideoPlayer(String videoPath, ImageView imageView, FrameProcessor frameProcessor, Runnable onEnd) {
        this.videoCapture = new VideoCapture(videoPath);
        this.frame = new Mat();

        readVideo = () -> {
            while (true) {
                if (videoCapture.read(frame)) {
                    frameProcessor.process(frame);
                    Image image = matToJavaFXImage(frame);
                    imageView.setImage(image);
                } else {
                    videoCapture.release();
                    playing = false;
                    Objects.requireNonNullElse(onEnd, () -> {}).run();
                    break;
                }
            }
        };
    }

    public void play() {
        if (playing || !videoCapture.isOpened()) return;
        AircraftDetection.executorService.submit(readVideo);
        playing = true;
    }

    private Image matToJavaFXImage(Mat mat) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", mat, buffer);
        byte[] bytes = buffer.toArray();
        InputStream inputStream = new ByteArrayInputStream(bytes);
        return new Image(inputStream);
    }
}
