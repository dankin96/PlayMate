package com.technostart.playmate.core.cv.field_detector;

import org.opencv.core.Mat;
import org.opencv.core.Size;

public abstract class FieldDetector {
    protected Size frameSize;
    public FieldDetector(Size frameSize) {
        this.frameSize = frameSize;
    }

    public abstract Mat getField(Mat frame);
}
