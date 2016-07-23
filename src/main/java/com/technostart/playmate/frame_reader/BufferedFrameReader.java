package com.technostart.playmate.frame_reader;

import java.util.HashMap;
import java.util.Map;

public class BufferedFrameReader<T> implements FrameReader<T> {
    FrameReader<T> frameReader;
    Map<Integer, T> buffer;
    int cursor;
    int interval;

    public BufferedFrameReader(FrameReader<T> frameReader, int interval) {
        this.frameReader = frameReader;
        this.interval = interval;
        buffer = new HashMap<>();
        cursor = 0;
        load();
    }

    @Override
    public T read() {
        if (!buffer.containsKey(cursor)) {
            frameReader.get(cursor);
            load();
        }
        return buffer.get(cursor);
    }

    @Override
    public T next() {
        if (cursor + 1 < getFramesNumber()) {
            cursor++;
        }
        return read();
    }

    @Override
    public T prev() {
        if (0 < cursor - 1) {
            cursor--;
        }
        return read();
    }

    @Override
    public T get(int index) {
        if (0 <= index && index < getFramesNumber()) {
            cursor = index;
        }
        return read();
    }

    @Override
    public int getFramesNumber() {
        return frameReader.getFramesNumber();
    }

    @Override
    public int getCurrentFrameNumber() {
        return cursor;
    }

    protected void load() {
        for (int i = cursor; i < cursor + interval; i++) {
            if (!buffer.containsKey(i)) {
                buffer.put(i, frameReader.read());
            }
        }
    }

}
