package com.technostart.playmate.core.cv;

import java.util.ArrayList;
import java.util.List;

public class Buffer<T> {

    private static final float RATE = 0.5f;
    private int capacity;
    private int cursor;
    List<T> body;

    public Buffer(int capacity) {
        body = new ArrayList<>(capacity);
    }

    public void put(T value) {
        if (body.size() + 1 > capacity) {
            body.remove(0);
            cursor--;
        }
        body.add(value);
    }

    public void putAll(List<T> values) {
        for (T v : values) {
            this.put(v);
        }
    }

    public void clear() {
        body.clear();
    }

    public T getNext() {
        if (cursor + 1 != capacity) cursor++;
        return body.get(cursor);
    }

}
