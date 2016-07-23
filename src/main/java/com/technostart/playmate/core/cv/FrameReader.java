package com.technostart.playmate.core.cv;


public interface FrameReader<T> {
    T read();

    T next();

    T prev();

    T get(int index);

    int getFramesNumber();

    int getCurrentFrameNumber();
}
