package com.technostart.playmate.core.cv.tracker;

import org.opencv.core.Point;

public interface HitDetectorInterface {
    void onHitDetect(Point hitPoint);
}
