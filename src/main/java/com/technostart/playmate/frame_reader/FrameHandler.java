package com.technostart.playmate.frame_reader;

public interface FrameHandler<T1, T2> {
    public T1 process(T2 inputFrame);
}
