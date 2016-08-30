package com.technostart.playmate.core.cv.tracker;

import com.technostart.playmate.core.cv.Utils;
import com.technostart.playmate.core.settings.Cfg;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("WeakerAccess")
public class TrackerCorrector {
    TrackerInterface trackerListener;

    @Cfg
    int maxTrackBufferSize = 4;
    @Cfg
    double downAngleThreshold = 30;

    private List<Point> trackBuffer;
    private List<Point> correctedPoints;

    Point prevPoint;

    public TrackerCorrector() {
        trackBuffer = new ArrayList<>(maxTrackBufferSize);
        correctedPoints = new ArrayList<>(maxTrackBufferSize);
    }

    public TrackerCorrector(TrackerInterface trackerListener) {
        this();
        setTrackerListener(trackerListener);
    }

    public void addNewPoint(Point newPoint) {
        if (trackBuffer.size() >= maxTrackBufferSize) {
            trackBuffer.remove(0);
        }
        trackBuffer.add(newPoint);
        correct();
    }

    public void setTrackerListener(TrackerInterface trackerListener) {
        this.trackerListener = trackerListener;
    }

    private void correct() {
        int size = trackBuffer.size();
        if (size < maxTrackBufferSize) {
            return;
        }

        Point p1 = trackBuffer.get(0);
        Point p2 = trackBuffer.get(1);
        Point p3 = trackBuffer.get(2);
        Point p4 = trackBuffer.get(3);

        if (!p2.equals(prevPoint)) {
            trackerListener.onTrackPoint(p2);
            prevPoint = p2;
        }
        boolean isTopDown = p2.y < p1.y && p2.y < p4.y && p3.y < p4.y && p3.y < p1.y;
        if (!isTopDown) {
            Point newPoint = Utils.intersection(p1, p2, p3, p4);
            if (newPoint != null) {
                if (!newPoint.equals(prevPoint)) {
                    trackerListener.onTrackPoint(newPoint);
                    prevPoint = newPoint;
                }
            }
        }

    }

}
