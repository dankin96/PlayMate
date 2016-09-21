package com.technostart.playmate.core.cv.background_subtractor;

import com.technostart.playmate.core.settings.Cfg;

import org.opencv.core.Mat;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.Video;

public class BgSubtractorFactory {
    @Cfg(name = "mog2history")
    private int history = 5;
    @Cfg(name = "mog2threshold")
    private double threshold = 10;
    @Cfg(name = "mog2shadows")
    private boolean detectShadows = false;

    public static BackgroundExtractor createMOG2(int history, double varThreshold, boolean detectShadows) {
        return new BackgroundExtractor() {
            BackgroundSubtractor backgroundSubtractor = Video.createBackgroundSubtractorMOG2(history, varThreshold, detectShadows);

            @Override
            public void apply(Mat image, Mat fgMask) {
                backgroundSubtractor.apply(image, fgMask);
            }
        };
    }

    public BackgroundExtractor createMOG2() {
        return createMOG2(history, threshold, detectShadows);
    }

    public static BackgroundExtractor createSimpleBS() {
        return new SimpleBackgroundSubtractor();
    }

}
