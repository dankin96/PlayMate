package com.technostart.playmate.core.cv.settings;

@SuppressWarnings("WeakerAccess")
public class Property<T> {
    public static final String BOOLEAN = "Boolean";
    public static final String INTEGER = "Integer";
    public static final String DOUBLE = "Double";
    public static final String STRING = "String";

    String type;
    T value;

    public Property(String type, T value) {
        this.type = type;
        this.value = value;
    }
}
