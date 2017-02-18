package com.technostart.playmate.core.cv.tracker;

import com.technostart.playmate.core.cv.Palette;
import com.technostart.playmate.core.cv.Utils;
import com.technostart.playmate.core.cv.background_subtractor.BackgroundExtractor;
import com.technostart.playmate.core.cv.background_subtractor.BgSubtractorFactory;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OpticalFlow {
    private Mat bgMask = new Mat();
    private BackgroundExtractor bgSubtractor;

    private Mat prevFrame;
    private MatOfPoint2f prevPoints = new MatOfPoint2f();
    private MatOfPoint2f nextPoints = new MatOfPoint2f();

    public OpticalFlow() {
        bgSubtractor = BgSubtractorFactory.createMOG2(3, 300, true);
    }

    public void process(Mat inputFrame) {
        if (prevFrame == null) {
            Utils.setResizeHeight((int) inputFrame.size().height);
            Utils.setResizeWidth((int) inputFrame.size().width);
            // Выделение фона.
            bgSubtractor.apply(inputFrame, bgMask);
            // Шумодав.
            bgMask = Utils.filterNoise(bgMask);

            // Выделение контуров.
            List<MatOfPoint> contours = new ArrayList<>();
            Imgproc.findContours(bgMask.clone(), contours, new Mat(),
                    Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            // todo массив точек
            List<Point> prevPointsList = new ArrayList<>();
            for (MatOfPoint contour : contours) {
                prevPointsList.add(Utils.getCentroid(contour));
            }
            prevPoints.fromList(prevPointsList);
            prevFrame = inputFrame;
            nextPoints = prevPoints;
            return;
        }

        prevPoints = nextPoints;
        // TODO: выбор обсчета по маске или исходному кадру.
        Video.calcOpticalFlowPyrLK(prevFrame, inputFrame, prevPoints, nextPoints, new MatOfByte(), new MatOfFloat());
        prevFrame = inputFrame;

    }

    public Mat getFrame(Mat inputFrame) {
        process(inputFrame);
        // Рисуем треки.
        List<Point> prevPointsList = prevPoints.toList();
        List<Point> nextPointsList = nextPoints.toList();
        for (int i = 0, pSize = prevPointsList.size(), nSize = nextPointsList.size(); i < pSize && i < nSize; i++) {
            Point prevPoint = prevPointsList.get(i);
            Point nextPoint = nextPointsList.get(i);
            Utils.drawLine(Arrays.asList(prevPoint, nextPoint), inputFrame, Palette.getRandomColor(), 1);
        }
        return inputFrame;
    }

    public void setBgSubstr(BackgroundExtractor newBgExtr) {
        bgSubtractor = newBgExtr;
    }


}
