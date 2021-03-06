package com.technostart.playmate.frame_reader;

import com.technostart.playmate.core.settings.Cfg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BufferedFrameReader<T> implements FrameReader<T> {
    public static final int DEFAULT_INTERVAL = 1;
    public static final int DEFAULT_CAPACITY = 400;
    FrameReader<T> frameReader;
    Map<Integer, T> buffer;
    List<Integer> keyList;
    int cursor;
    @Cfg(name = "BuffFrameReaderInterval")
    int interval = DEFAULT_INTERVAL;
    @Cfg(name = "BuffFrameReaderCapacity")
    int capacity = DEFAULT_CAPACITY;

    public BufferedFrameReader(FrameReader<T> frameReader) {
        this(frameReader, DEFAULT_INTERVAL, DEFAULT_CAPACITY);
    }

    public BufferedFrameReader(FrameReader<T> frameReader, int interval, int capacity) {
        this.frameReader = frameReader;
        this.interval = interval;
        this.capacity = capacity;
        keyList = new ArrayList<>();
        buffer = new LinkedHashMap<>();
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

    @Override
    public void close() {
        buffer.clear();
        frameReader.close();
    }

    public void clear() {
        buffer.clear();
    }

    private void load() {
        for (int i = cursor; i < cursor + interval; i++) {
            if (!buffer.containsKey(i)) {
                if (buffer.size() > capacity) {
                    int removeIdx = buffer.keySet().iterator().next();
                    buffer.remove(removeIdx);
                }
                keyList.add(i);
                buffer.put(i, frameReader.read());
            }
        }
    }

}
