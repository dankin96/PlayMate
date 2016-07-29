package com.technostart.playmate.core.cv;

import org.opencv.core.Mat;

public abstract class TableDetector {
    public TableDetector() {
    }
    public abstract Mat getTable(Mat inputFrame);
}
