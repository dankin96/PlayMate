package com.technostart.playmate.core.cv.tracker;

import com.technostart.playmate.core.cv.Utils;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private List<MatOfPoint> contours;
    private LinkedHashMap<Long, Point> track;

    //

    public Group(Scalar medianColor, Point lastCoord) {
        init();
        this.medianColor = medianColor;
        this.lastCoord = lastCoord;
    }

    public Group(MatOfPoint contour, HitDetectorInterface hitDetectorListener) {
        this.hitDetector = new HitDetector(hitDetectorListener);
        init();
        add(System.currentTimeMillis(), contour);
    }

    private void init() {
        track = new LinkedHashMap<>();
        contours = new ArrayList<>();
//        hitDetector = new HitDetector();
    }

    public void add(long timestamp, MatOfPoint contour) {
        contours.add(contour);
        Point centroid = Utils.getCentroid(contour);
        add(timestamp, centroid);
    }

    public void add(long timestamp, List<MatOfPoint> newContours, List<Double> weights) {
        int size = newContours.size();
        contours.addAll(newContours);
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
        return contours;
    }

    public void idle() {
        idle++;
    }
}