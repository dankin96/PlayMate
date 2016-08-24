package com.technostart.playmate.core.cv.tracker;

import org.opencv.core.Point;

public interface TrackerInterface {
    void onTrackPointDetect(Point newPoint);
}
