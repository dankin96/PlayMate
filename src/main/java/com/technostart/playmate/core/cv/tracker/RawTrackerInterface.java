package com.technostart.playmate.core.cv.tracker;

import org.opencv.core.MatOfPoint;

import java.util.List;

@FunctionalInterface
public interface RawTrackerInterface {
    void onTrackContour(int groupId, List<MatOfPoint> contours);
}
