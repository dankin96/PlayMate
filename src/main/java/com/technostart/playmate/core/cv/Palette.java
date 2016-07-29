package com.technostart.playmate.core.cv;

import org.opencv.core.Scalar;

import java.util.Arrays;
import java.util.List;

public class Palette {
    public static final Scalar BLUE = new Scalar(255, 0, 0);
    public static final Scalar GREEN = new Scalar(0, 255, 0);
    public static final Scalar RED = new Scalar(0, 0, 255);
    public static final Scalar YELLOW = new Scalar(0, 255, 255);
    public static final Scalar PURPLE = new Scalar(255, 0, 255);
    public static final Scalar BG = new Scalar(255, 255, 0);
    public static final Scalar ORANGE = new Scalar(0, 69, 255);
    public static final Scalar PINK = new Scalar(147, 20, 255);
    public static final Scalar BROWN = new Scalar(19, 69, 139);
    public static final Scalar BLACK = new Scalar(0, 0, 0);
    public static final Scalar WHITE = new Scalar(255, 255, 255);

    public static final List<Scalar> palette = Arrays.asList(BLUE, GREEN, RED, YELLOW, PURPLE, BG, ORANGE, PINK, BROWN);
    private static int colorIdx = 0;

    public static Scalar getNextColor() {
        colorIdx++;
        if (colorIdx >= palette.size()) colorIdx = 0;
        return palette.get(colorIdx);
    }

}