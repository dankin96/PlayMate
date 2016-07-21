package com.technostart.playmate.core.cv;

import org.opencv.core.Scalar;

import java.util.Arrays;
import java.util.List;

public class Color {
    public static final Scalar BLUE = new Scalar(255, 0, 0);
    public static final Scalar GREEN = new Scalar(0, 255, 0);
    public static final Scalar RED = new Scalar(0, 0, 255);
    public static final Scalar BLACK = new Scalar(0, 0, 0);
    public static final Scalar WHITE = new Scalar(255, 255, 255);

    public static final List<Scalar> palette = Arrays.asList(BLUE, GREEN, RED);
}
