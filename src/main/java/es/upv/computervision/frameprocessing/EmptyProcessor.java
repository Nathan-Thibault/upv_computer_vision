package es.upv.computervision.frameprocessing;

import org.opencv.core.Mat;

public class EmptyProcessor implements FrameProcessor{
    @Override
    public void process(Mat frame) {
        // Does nothing
    }
}
