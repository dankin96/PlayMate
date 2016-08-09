package com.technostart.playmate.core.cv.tracker;

import com.technostart.playmate.core.cv.Utils;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private static final int COLOR_NUMBER = 3;
    private Scalar medianColor;
    private Point lastCoord;

    // Кол-во итераций без добавления новых элементов.
    private int idle;
    // Максимальное кол-во итераций простоя.
    public static final int MAX_IDLE = 3;

    private List<Scalar> colors;
    private List<MatOfPoint> contours;
    private List<Point> track;

    //

    public Group(Scalar medianColor, Point lastCoord) {
        init();
        this.medianColor = medianColor;
        this.lastCoord = lastCoord;
    }

    public Group(MatOfPoint contour) {
        init();
        add(contour);
    }

    private void init() {
        track = new ArrayList<>();
        contours = new ArrayList<>();
    }

    public void add(MatOfPoint contour) {
        contours.add(contour);
        Point centroid = Utils.getCentroid(contour);
        add(centroid);
    }

    public void add(List<MatOfPoint> newContours, List<Double> weights) {
        int size = newContours.size();
        contours.addAll(newContours);
        Point centroid = null;
        if (size == 0) {
            return;
        } else if (size == 1) {
            add(newContours.get(0));
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
            add(centroid);
        }
    }

    private void add(Point centroid) {
        lastCoord = centroid;
        track.add(centroid);

        idle = idle > 0 ? idle - 1 : 0;
    }

    public Point getLastCoord() {
        return lastCoord;
    }

    public int getIdle() {
        return idle;
    }

    public MatOfPoint getTrackContour() {
        MatOfPoint contour = new MatOfPoint();
        contour.fromList(track);
        return contour;
    }

    public List<Point> getTrack() {
        return track;
    }

    public List<MatOfPoint> getContourList() {
        return contours;
    }

    public void idle() {
        idle++;
    }
}