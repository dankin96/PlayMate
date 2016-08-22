package com.technostart.playmate.core.common;

@FunctionalInterface
public interface Command<T1, T2> {
    T2 execute(T1 input);
}
