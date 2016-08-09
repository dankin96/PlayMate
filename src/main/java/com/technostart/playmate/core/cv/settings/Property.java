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

    public String getType() {
        return type;
    }

    public T getValue() {
        return value;
    }

    public static Property<Integer> newIntProperty(int value) {
        return new Property<Integer>(Property.INTEGER, value);
    }

    public static Property<Boolean> newBoolProperty(boolean value) {
        return new Property<Boolean>(Property.BOOLEAN, value);
    }

    public static Property<Double> newDoubleProperty(double value) {
        return new Property<Double>(Property.DOUBLE, value);
    }

    public static Property<String> newStringProperty(String value) {
        return new Property<String>(Property.STRING, value);
    }

}
