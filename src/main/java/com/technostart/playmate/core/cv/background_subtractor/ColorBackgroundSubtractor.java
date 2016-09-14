package com.technostart.playmate.core.cv.background_subtractor;

import com.technostart.playmate.core.settings.Cfg;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ColorBackgroundSubtractor implements BackgroundExtractor {
    @Cfg
    double lowerColor = 0;
    @Cfg
    double upperColor = 38;

    Scalar lowerB = new Scalar(0, 10, 6);
    Scalar upperB = new Scalar(38, 255, 255);

    public ColorBackgroundSubtractor() {}

    public ColorBackgroundSubtractor(double lowerColor, double upperColor) {
        this.lowerColor = lowerColor;
        this.upperColor = upperColor;
        lowerB = new Scalar(lowerColor, 70, 6);
        upperB = new Scalar(upperColor, 255, 255);
    }

    public ColorBackgroundSubtractor(Scalar lowerB, Scalar upperB) {
        this.lowerB = lowerB;
        this.upperB = upperB;
    }

    @Override
    public void apply(Mat image, Mat fgMask) {
        Mat hsvImage = image.clone();
        Imgproc.cvtColor(image.clone(), hsvImage, Imgproc.COLOR_BGR2HSV);
        Core.inRange(hsvImage, lowerB, upperB, fgMask);
    }
}
