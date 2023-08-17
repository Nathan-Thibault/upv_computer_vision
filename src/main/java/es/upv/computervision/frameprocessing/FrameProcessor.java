package es.upv.computervision.frameprocessing;

import org.opencv.core.Mat;

public interface FrameProcessor {
    void process(Mat frame);
}
