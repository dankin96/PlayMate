package com.technostart.playmate.core.cv.background_subtractor;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class ColorBackgroundSubtractor implements BackgroundExtractor {

    private Scalar lowerB = new Scalar(0, 150, 200);
    private Scalar upperB = new Scalar(170, 255, 255);

    public ColorBackgroundSubtractor() {}

/*    public ColorBackgroundSubtractor(double lowerColor, double upperColor, double lowerSaturation) {
        lowerB = new Scalar(lowerColor, lowerSaturation, 6);
        upperB = new Scalar(upperColor, 255, 255);
    }*/

    public ColorBackgroundSubtractor(Scalar lowerB, Scalar upperB) {
        this.lowerB = lowerB;
        this.upperB = upperB;
    }

    @Override
    public void apply(Mat image, Mat fgMask) {
        Core.inRange(image.clone(), lowerB, upperB, fgMask);
    }
}
