package com.technostart.playmate.core.cv.background_subtractor;

import org.opencv.core.Mat;

public interface BackgroundExtractor {
    void apply(Mat image, Mat fgMask);
}
