package com.technostart.playmate.core.cv.tracker;

import org.opencv.core.Point;

public class Hit {
    // LEFT - слева на право
    enum Direction {UNDEFINED, LEFT_TO_RIGHT, RIGHT_TO_LEFT}
    Point point;
    Direction direction;
}
