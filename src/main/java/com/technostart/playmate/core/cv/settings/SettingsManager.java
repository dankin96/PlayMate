package com.technostart.playmate.core.cv.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class SettingsManager {
    Map<String, Property> properties;

    public SettingsManager() {
        this.properties = new HashMap<>();
    }

    public void fromJson(String json) {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Property>>() {
        }.getType();
        properties = gson.fromJson(json, type);
    }

    public void mockProperty() {
        properties.put("i1", new Property<Integer>(Property.INTEGER, 1));
        properties.put("i2", new Property<Integer>(Property.INTEGER, 2));
        properties.put("d1", new Property<Double>(Property.DOUBLE, 1.5));
        properties.put("s1", new Property<String>(Property.STRING, "hello"));
        properties.put("b1", new Property<Boolean>(Property.BOOLEAN, true));
    }

    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(properties);
    }

    public Map<String, Property> getProperties() {
        return properties;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Получение настроек.
    ///////////////////////////////////////////////////////////////////////////

    public int getInt(String key) {
        Property property = properties.get(key);
        assert property.type.equals(Property.INTEGER);
        // FIXME:
        Object value = property.value;
        if (value instanceof Double) return ((Double) value).intValue();
        return (int) value;
    }

    public double getDouble(String key) {
        Property property = properties.get(key);
        assert property.type.equals(Property.DOUBLE);
        return (double) property.value;
    }

    public String getString(String key) {
        Property property = properties.get(key);
        assert property.type.equals(Property.STRING);
        return (String) property.value;
    }

    public boolean getBoolean(String key) {
        Property property = properties.get(key);
        assert property.type.equals(Property.BOOLEAN);
        return (boolean) property.value;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Установка настроек.
    ///////////////////////////////////////////////////////////////////////////

    public void putInt(String key, int value) {
        properties.put(key, Property.newIntProperty(value));
    }

    public void putBoolean(String key, boolean value) {
        properties.put(key, Property.newBoolProperty(value));
    }

    public void putDouble(String key, double value) {
        properties.put(key, Property.newDoubleProperty(value));
    }

    public void putString(String key, String value) {
        properties.put(key, Property.newStringProperty(value));
    }

}
