package com.technostart.playmate.core.cv.tracker;

import com.technostart.playmate.core.cv.Utils;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class Group {

    HitDetector hitDetector;

    private static final int COLOR_NUMBER = 3;
    private Scalar medianColor;
    private Point lastCoord;

    // Кол-во итераций без добавления новых элементов.
    private int idle;
    // Максимальное кол-во итераций простоя.
    public static final int MAX_IDLE = 3;

    private List<Scalar> colors;
    private LinkedHashMap<Long, List<MatOfPoint>> timeToContours;
    private LinkedHashMap<Long, Point> track;

    //

    public Group(Scalar medianColor, Point lastCoord) {
        init();
        this.medianColor = medianColor;
        this.lastCoord = lastCoord;
    }

    public Group(long timestamp, MatOfPoint contour, HitDetectorInterface hitDetectorListener) {
        this.hitDetector = new HitDetector(hitDetectorListener);
        init();
        add(timestamp, contour);
    }

    private void init() {
        track = new LinkedHashMap<>();
        timeToContours = new LinkedHashMap<>();
//        hitDetector = new HitDetector();
    }

    public void add(long timestamp, MatOfPoint contour) {
        timeToContours.put(timestamp, Arrays.asList(contour));
        Point centroid = Utils.getCentroid(contour);
        add(timestamp, centroid);
    }

    public void add(long timestamp, List<MatOfPoint> newContours, List<Double> weights) {
        int size = newContours.size();
        timeToContours.put(timestamp, newContours);
        Point centroid = null;
        if (size == 0) {
            return;
        } else if (size == 1) {
            add(timestamp, newContours.get(0));
            return;
        } else if (size == 2) {
            Point c1 = Utils.getCentroid(newContours.get(0));
            Point c2 = Utils.getCentroid(newContours.get(1));
            centroid = new Point((c1.x + c2.x) / 2, (c1.y + c2.y) / 2);
        } else if (size > 2) {
            // TODO: Центроид с весами.
            centroid = Utils.getContoursCentroid(newContours, weights);
//            centroid = Utils.getContoursCentroid(newContours);
        }

        if (centroid != null) {
            add(timestamp, centroid);
        }
    }

    private void add(long timestamp, Point centroid) {
        lastCoord = centroid;
        track.put(timestamp, centroid);
        hitDetector.addNewPoint(centroid);
        idle = idle > 0 ? idle - 1 : 0;
    }

    public Point getLastCoord() {
        return lastCoord;
    }

    public int getIdle() {
        return idle;
    }

/*    public MatOfPoint getTrackContour() {
        MatOfPoint contour = new MatOfPoint();
        contour.fromList(track);
        return contour;
    }*/

    public List<Point> getTrack() {
        return new ArrayList(track.values());
    }

    public List<MatOfPoint> getContourList() {
        List<MatOfPoint> allContours = new ArrayList<>();
        for (List<MatOfPoint> contours : timeToContours.values()) {
            allContours.addAll(contours);
        }
        return allContours;
    }

    public List<MatOfPoint> getContoursByTimestamp(List<Long> timestamps) {
        List<MatOfPoint> contours = new ArrayList<>();
        for (long timestamp : timestamps) {
            if (track.containsKey(timestamp)) {
                contours.addAll(timeToContours.get(timestamp));
            }
        }
        return contours;
    }

    public List<Point> getTrackPointsByTamstamp(List<Long> timestamps) {
        List<Point> points = new ArrayList<>();
        for (long timestamp : timestamps) {
            if (track.containsKey(timestamp)) {
                points.add(track.get(timestamp));
            }
        }
        return points;
    }

        public void idle () {
            idle++;
        }
    }