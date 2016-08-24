package com.technostart.playmate.core.cv.tracker;

import com.technostart.playmate.core.cv.Utils;
import com.technostart.playmate.core.settings.Cfg;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class HitDetector {
    public enum Status {UNDEFINED, TABLE_HIT, PLAYER_HIT}

    @Cfg
    double upperAngleThreshold = 120;
    int bufferTrackSize = 3;

    List<Point> track;
    HitDetectorInterface hitDetectorListener;

    public HitDetector() {
        track = new ArrayList<>(bufferTrackSize);
    }

    public HitDetector(HitDetectorInterface hitDetectorListener) {
        this();
        setHitDetectorListener(hitDetectorListener);
    }

    public void setAngleThreshold(double angle) {
        upperAngleThreshold = angle;
    }

    public void addNewPoint(Point point) {
        if (track.size() > bufferTrackSize) {
            track.remove(0);
        }
        track.add(point);
        Status status = getStatus();
        switch (status) {
            case UNDEFINED:
                break;
            case TABLE_HIT:
                Point hitPoint = track.get(1);
                hitDetectorListener.onHitDetect(hitPoint);
                break;
            case PLAYER_HIT:
                break;
        }

    }

    public void setHitDetectorListener(HitDetectorInterface hitDetectorListener) {
        this.hitDetectorListener = hitDetectorListener;
    }

    public Status getStatus() {
        if (track.size() < bufferTrackSize) return Status.UNDEFINED;
        if (!checkOrientation()) return Status.UNDEFINED;
        if (checkAngle()) return Status.TABLE_HIT;
        return Status.UNDEFINED;
    }

    private boolean checkOrientation() {
        Point p1 = track.get(0);
        Point p2 = track.get(1);
        Point p3 = track.get(2);
        boolean s1 = p1.x < p2.x && p2.x < p3.x;
        boolean s2 = p1.x > p2.x && p2.x > p3.x;
        boolean s3 = p1.y > p2.y && p2.y < p3.y;
        return (s1 || s2) && s3;
    }

    private boolean checkAngle() {
        Point p1 = track.get(0);
        Point p2 = track.get(1);
        Point p3 = track.get(2);
        double angle = Utils.getAngle(p1, p2, p2, p3);
        return angle < upperAngleThreshold;
    }


}
