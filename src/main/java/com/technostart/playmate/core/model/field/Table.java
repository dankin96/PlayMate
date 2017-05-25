package com.technostart.playmate.core.model.field;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

public class Table {
    // Размеры в метрах.
    public static final double width = 1.525;
    public static final double length = 2.74;
    public static List<Point> getBorderPoint() {
        ArrayList<Point> points = new ArrayList<>();
        points.add(new Point(0, 0));
        points.add(new Point(0, width));
        points.add(new Point(length, width));
        points.add(new Point(length, 0));
        return points;
    }
}
