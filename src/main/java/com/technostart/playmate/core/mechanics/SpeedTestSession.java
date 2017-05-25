package com.technostart.playmate.core.mechanics;

import com.technostart.playmate.core.cv.Palette;
import com.technostart.playmate.core.cv.Utils;
import com.technostart.playmate.core.cv.field_detector.TableDetector;
import com.technostart.playmate.core.cv.tracker.Group;
import com.technostart.playmate.core.cv.tracker.Hit;
import com.technostart.playmate.core.cv.tracker.HitDetectorInterface;
import com.technostart.playmate.core.cv.tracker.Tracker;
import com.technostart.playmate.core.model.field.Table;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpeedTestSession implements HitDetectorInterface {
    private static final int MAX_FRAMES = 300;
    private int frameNumber;

    private SpeedTestSessionListener sessionListener;

    private Table table = new Table();

    private boolean isRecordEnable = false;

    private ExecutorService executor;

    private Tracker tracker;
    private TableDetector tableDetector;

    private List<Hit> hitList = new ArrayList<>();
    private List<Long> timestampList = new ArrayList<>();
    private Mat resultImg = new Mat();

    public SpeedTestSession(Mat resultImg, Tracker tracker, SpeedTestSessionListener sessionListener) {
        this.sessionListener = sessionListener;
        this.tracker = tracker;
        this.resultImg = resultImg;
        executor = Executors.newSingleThreadExecutor();
        tracker.setHitDetectorListener(this);
        frameNumber = 0;
    }


    public void setTable(Point... tablePoints) {

    }

    public void findTable() {
        // TODO:
        List<MatOfPoint> tableContours = null;
        sessionListener.onTableFound(tableContours);
    }

    public void startRecord() {
        isRecordEnable = true;
        frameNumber = 0;
    }

    public void stopRecord() {
        isRecordEnable = false;
    }

    public void close() {
        executor.shutdownNow();
        frameNumber = 0;
    }

    public void submitNewFrame(Mat newFrame) {
        if (!isRecordEnable || (frameNumber > MAX_FRAMES)) return;
        frameNumber++;
        // Отмечаем время.
        long timestamp = System.currentTimeMillis();
        timestampList.add(timestamp);
        // Отправляем кадр на обработку.
        executor.submit(() -> tracker.process(timestamp, newFrame));
    }

    public static double calcSpeed(Hit hit1, Hit hit2) {
        // todo: perspective transform
        // todo: distance
        double distance = Utils.getDistance(hit1.point, hit2.point);
        // todo: timediff
        double timeDiff = hit2.timestamp - hit1.timestamp;
        return Math.abs(1000 * distance / timeDiff);
    }

    @Override
    public void onHitDetect(Hit hit) {
        if (!checkHit(hit)) return;

        hitList.add(hit);
        if (hitList.size() == 2) {
            double speed = calcSpeed(hitList.get(0), hitList.get(1));
            resultImg = drawResultImg();
            isRecordEnable = false;
            close();
            sessionListener.onTestComplete(resultImg, hitList, speed);
        }
    }

    private Mat drawResultImg() {
        List<Group> allGroups = tracker.getAllGroups();
        for (Group group : allGroups) {
            // Контуры.
            Imgproc.drawContours(resultImg, group.getAllContours(), -1, Palette.getRandomColor(10));
            // Треки.
            List<Point> trackPoints = group.getAllTrackPoints();
            Utils.drawLine(trackPoints, resultImg, Palette.getRandomColor(10), 2);
        }
        for (Hit hit : hitList) {
            Imgproc.circle(resultImg, hit.point, 4, Palette.RED);
        }
        return resultImg;
    }

    public boolean checkHit(Hit hit) {
        // todo: проверка попадания
        return true;
    }

    public interface SpeedTestSessionListener {
        void onTestComplete(Mat resultImg, List<Hit> hitList, double speed);

        void onTableFound(List<MatOfPoint> tableContours);
    }
}
