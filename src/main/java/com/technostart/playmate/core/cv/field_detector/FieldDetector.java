package com.technostart.playmate.core.cv.field_detector;

import org.opencv.core.Mat;
import org.opencv.core.Size;

public abstract class FieldDetector {
    protected Size frameSize;

    public FieldDetector(Size frameSize) {
        this.frameSize = frameSize;
    }

    public abstract Mat getField(Mat inputFrame);

    public abstract Mat getFrame(Mat inputFrame);

    public double getWidth() {
        return frameSize.width;
    }

    public double getHeight() {
        return frameSize.height;
    }
}