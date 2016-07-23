package com.technostart.playmate.core.cv;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.io.Closeable;
import java.io.IOException;

public class CvFrameReader implements FrameReader<Mat>, Closeable {

    private VideoCapture capture;
    private int frameNumber;

    public CvFrameReader(String fileName) {
        capture = new VideoCapture(fileName);
        frameNumber = (int) capture.get(7);
    }

    @Override
    public Mat read() {
        Mat frame = new Mat();
        if (capture.isOpened()) {
            capture.read(frame);
        }
        return frame;
    }

    @Override
    public Mat next() {
        int index = getCurrentFrameNumber() + 1;
        if (index >= frameNumber) {
            index = frameNumber - 2;
            capture.set(1, index);
        }
        return read();
    }

    @Override
    public Mat prev() {
        int index = getCurrentFrameNumber() - 2;
        if (index < 0) index = 0;
        capture.set(1, index);
        return read();
    }

    @Override
    public Mat get(int index) {
        if (0 <= index && index < frameNumber) {
            capture.set(1, index);
        }
        return read();
    }

    @Override
    public int getCurrentFrameNumber() {
        return (int) capture.get(1);
    }

    @Override
    public int getFramesNumber() {
        return frameNumber;
    }

    @Override
    public void close() throws IOException {
        capture.release();
    }
}
