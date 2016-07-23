package com.technostart.playmate.frame_reader;

import javafx.scene.image.Image;
import org.opencv.core.Mat;


public class Mat2ImgReader implements FrameReader<Image> {
    private FrameReader<Mat> frameReader;
    private FrameHandler<Image, Mat> handler;

    public Mat2ImgReader(CvFrameReader frameReader, FrameHandler<Image, Mat> frameHandler) {
        this.frameReader = frameReader;
        this.handler = frameHandler;
    }

    @Override
    public Image read() {
        return handler.process(frameReader.read());
    }

    @Override
    public Image next() {
        return handler.process(frameReader.next());
    }

    @Override
    public Image prev() {
        return handler.process(frameReader.prev());
    }

    @Override
    public Image get(int index) {
        return handler.process(frameReader.get(index));
    }

    @Override
    public int getFramesNumber() {
        return frameReader.getFramesNumber();
    }

    @Override
    public int getCurrentFrameNumber() {
        return frameReader.getCurrentFrameNumber();
    }
}
