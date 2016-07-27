package com.technostart.playmate.frame_reader;


public interface FrameReader<T> {
    T read();

    T next();

    T prev();

    T get(int index);

    int getFramesNumber();

    int getCurrentFrameNumber();

    void close();
}
