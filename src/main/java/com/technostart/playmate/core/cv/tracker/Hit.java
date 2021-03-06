package com.technostart.playmate.core.cv.tracker;

import org.opencv.core.Point;

public class Hit {
    public enum Direction {UNDEFINED, LEFT_TO_RIGHT, RIGHT_TO_LEFT}

    public Hit(Point point, Direction direction) {
        this.point = point;
        this.direction = direction;
    }

    public Hit(Point point, Direction direction, long timestamp) {
        this(point, direction);
        this.timestamp = timestamp;
    }

    public Point point;
    public Direction direction;
    public long timestamp;
}
