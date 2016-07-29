package com.technostart.playmate.core.cv;

import org.opencv.core.Scalar;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@SuppressWarnings("WeakerAccess")
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

    public static Scalar getRandomColor() {
        return getRandomColor(0);
    }

    /**
     * @param offset чем меньше тем ярче цвета и больше коллизий.
     * @return случайный цвет в формате BGR.
     */
    public static Scalar getRandomColor(int offset) {
        Random random = new Random();
        int delta = 255 - offset;

        int blue = random.nextInt(delta) + offset;
        int green = random.nextInt(delta) + offset;
        int red = random.nextInt(delta) + offset;
        return new Scalar(blue, green, red);
    }

}
