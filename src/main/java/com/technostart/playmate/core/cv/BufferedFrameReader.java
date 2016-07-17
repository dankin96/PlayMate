package com.technostart.playmate.core.cv;

import java.util.ArrayList;
import java.util.List;

public class BufferedFrameReader<T> implements FrameReader<T> {
    private FrameReader<T> frameReader;
    private List<T> buffer;
    private int capacity;
    private int cursor;
    private int interval;

    public BufferedFrameReader(FrameReader<T> frameReader, int capacity, int interval) {
        this.frameReader = frameReader;
        this.interval = interval;
        buffer = new ArrayList<>(capacity);
        cursor = 0;
        load();
    }

    @Override
    public T read() {
        return buffer.get(cursor);
    }

    @Override
    public T next() {
        cursor++;
        if (cursor >= buffer.size()) {
            load();
        }
        return read();
    }

    @Override
    public T prev() {
        cursor--;
        return read();
    }

    @Override
    public T get(int index) {
        if (index >= buffer.size()) {
            buffer.clear();
            frameReader.get(index);
            load();
        }
        return frameReader.get(index);
    }

    @Override
    public int getFrameNumber() {
        return frameReader.getFrameNumber();
    }

    @Override
    public int getCurrentFrameNumber() {
        return frameReader.getCurrentFrameNumber();
    }

    private void load() {
        for (int i = 0; i < interval; i++) {
            buffer.add(frameReader.read());
        }
    }

    private void addToBuffer(T value) {
        if (cursor > interval) {
            buffer.remove(0);
        }
        buffer.add(value);
    }
}
