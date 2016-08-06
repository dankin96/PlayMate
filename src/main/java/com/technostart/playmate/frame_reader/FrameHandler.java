package com.technostart.playmate.frame_reader;

@FunctionalInterface
public interface FrameHandler<T1, T2> {
    T1 process(T2 inputFrame);
}
