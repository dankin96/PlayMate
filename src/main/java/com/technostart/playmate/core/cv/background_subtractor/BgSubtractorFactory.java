package com.technostart.playmate.core.cv.background_subtractor;

import org.opencv.core.Mat;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.Video;

public class BgSubtractorFactory {
    public static BackgroundExtractor createMOG2(int history, double varThreshold, boolean detectShadows) {
        return new BackgroundExtractor() {
            BackgroundSubtractor backgroundSubtractor = Video.createBackgroundSubtractorMOG2(history, varThreshold, detectShadows);
            @Override
            public void apply(Mat image, Mat fgMask) {
                backgroundSubtractor.apply(image, fgMask);
            }
        };
    }

    public static BackgroundExtractor createSimpleBS() {
        return new SimpleBackgroundSubtractor();
    }

}
