package com.technostart.playmate.core.cv.background_subtractor;

import com.technostart.playmate.core.settings.Cfg;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class SimpleBackgroundSubtractor implements BackgroundExtractor {
    @Cfg(name = "simpleBgSubstrThreshold")
    private double threshold = 80;
    private Mat prevFrame;


    public SimpleBackgroundSubtractor() {
    }


    @Override
    public void apply(Mat image, Mat fgMask) {
        if (prevFrame == null) {
            prevFrame = image;
            return;
        }

        Core.absdiff(prevFrame, image, fgMask);
        prevFrame = image.clone();
        Imgproc.threshold(fgMask, fgMask, threshold, 255, Imgproc.THRESH_BINARY);
        if (fgMask.type() == CvType.CV_8UC3) {
            Imgproc.cvtColor(fgMask, fgMask, Imgproc.COLOR_BGR2GRAY);
        }
        Imgproc.threshold(fgMask, fgMask, 1, 255, Imgproc.THRESH_BINARY);
    }
}
