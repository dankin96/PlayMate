package com.technostart.playmate.core.sessions;

import com.technostart.playmate.core.cv.background_subtractor.BackgroundExtractor;
import com.technostart.playmate.core.cv.background_subtractor.BgSubtractorFactory;
import com.technostart.playmate.core.cv.field_detector.FieldDetector;
import com.technostart.playmate.core.cv.field_detector.TableDetector;
import com.technostart.playmate.core.cv.tracker.Hit;
import com.technostart.playmate.core.cv.tracker.HitDetectorFilter;
import com.technostart.playmate.core.cv.tracker.HitDetectorInterface;
import com.technostart.playmate.core.cv.tracker.Tracker;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

public class Session implements HitDetectorInterface {
    protected HitDetectorInterface hitDetectorListener;

    protected Tracker tracker;
    protected FieldDetector fieldDetector;

    protected Mat lastFieldMask;

    public Session(Size size) {
        hitDetectorListener = (hit) -> {};
        BackgroundExtractor bgExtractor = BgSubtractorFactory.createMOG2(3, 20, false);
        tracker = new Tracker(size, bgExtractor);
        tracker.setHitDetectorListener(this);
        fieldDetector = new TableDetector(size);
        lastFieldMask = Mat.zeros(size, CvType.CV_8UC3);
    }

    public void setHitDetectorListener(HitDetectorInterface hitDetectorListener) {
        this.hitDetectorListener = hitDetectorListener;
    }

    public void update(Mat frame) {
        tracker.process(System.currentTimeMillis(), frame.clone());
        // Обновляем маску стола.
        Mat newFieldMask = fieldDetector.getField(frame.clone());
        if (newFieldMask != null) {
            lastFieldMask = newFieldMask;
        }
    }

    @Override
    public void onHitDetect(Hit hit) {
//        if (lastFieldMask == null) return;
//        if (HitDetectorFilter.check(hitPoint, lastFieldMask)) {
            // ...
            hitDetectorListener.onHitDetect(hit);
//        }
    }
}
