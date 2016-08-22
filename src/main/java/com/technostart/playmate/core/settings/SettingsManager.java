package com.technostart.playmate.core.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class SettingsManager {
    Map<String, Property> properties;

    public SettingsManager() {
        this.properties = new LinkedHashMap<>();
    }

    public void fromJson(String json) {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Property>>() {
        }.getType();
        properties = gson.fromJson(json, type);
    }

    public void fromJson(Reader reader) {
        JsonReader jsonReader = new JsonReader(reader);
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Property>>() {
        }.getType();
        properties = gson.fromJson(jsonReader, type);
    }

    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(properties);
    }

    public Map<String, Property> getProperties() {
        return properties;
    }

    public void mockProperty() {
        properties.put("int1", new Property<Integer>(Property.INTEGER, 1));
        properties.put("int2", new Property<Integer>(Property.INTEGER, 2));
        properties.put("double1", new Property<Double>(Property.DOUBLE, 1.5));
        properties.put("string1", new Property<String>(Property.STRING, "hello"));
        properties.put("bool1", new Property<Boolean>(Property.BOOLEAN, true));
        properties.put("bool0", new Property<Boolean>(Property.BOOLEAN, false));
        properties.put("SomeLongName_______________qwrty", new Property<Boolean>(Property.BOOLEAN, true));
        properties.put("Name1", new Property<String>(Property.STRING, "SomeString"));
        properties.put("Name2", new Property<Double>(Property.DOUBLE, 123.456));
        properties.put("Name3", new Property<Integer>(Property.INTEGER, -1234));
    }


    public boolean containKey(String key) {
        return properties.containsKey(key);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Получение настроек.
    ///////////////////////////////////////////////////////////////////////////

    public int getInt(String key, int defaultValue) {
        if (!properties.containsKey(key)) return defaultValue;
        Property property = properties.get(key);
        assert property.type.equals(Property.INTEGER);
        Object value = property.value;
        if (value instanceof Double) return ((Double) value).intValue();
        return (int) value;
    }

    public double getDouble(String key, double defaultValue) {
        if (!properties.containsKey(key)) return defaultValue;
        Property property = properties.get(key);
        assert property.type.equals(Property.DOUBLE);
        return (double) property.value;
    }

    public String getString(String key, String defaultValue) {
        if (!properties.containsKey(key)) return defaultValue;
        Property property = properties.get(key);
        assert property.type.equals(Property.STRING);
        return (String) property.value;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (!properties.containsKey(key)) return defaultValue;
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

    /**
     * Добавляет поля с настройками из аннотированных полей объекта.
     */
    public void toSettings(Object obj) {
        Class clazz = obj.getClass();
        List<Field> fields = Arrays.asList(clazz.getDeclaredFields());
        for (Field field : fields) {
            field.setAccessible(true);
            if (!field.isAnnotationPresent(Cfg.class)) continue;
            String annotateName = field.getAnnotation(Cfg.class).name();
            String name = annotateName.equals(" defaultName") ? field.getName() : annotateName;
            Type type = field.getType();
            String typeName = type.toString();
            typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
            try {
                switch (typeName) {
                    case "int":
                    case "Integer":
                        putInt(name, (int) field.get(obj));
                        break;
                    case "double":
                    case "Double":
                        putDouble(name, (double) field.get(obj));
                        break;
                    case "String":
                        putString(name, (String) field.get(obj));
                        break;
                    case "boolean":
                    case "Boolean":
                        putBoolean(name, (boolean) field.get(obj));
                        break;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Заполняет поля класса соответствующими значениями из менеджера.
     */
    public <T> T fromSettings(T obj) throws IllegalAccessException {
        Class clazz = obj.getClass();
        List<Field> fields = Arrays.asList(clazz.getDeclaredFields());
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Cfg.class)) {
                String annotateName = field.getAnnotation(Cfg.class).name();
                String key = annotateName.equals(" defaultName") ? field.getName() : annotateName;
                Type type = field.getType();
                String typeName = type.toString();
                typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
                switch (typeName) {
                    case "int":
                    case "Integer":
                        field.setInt(obj, getInt(key, 0));
                        break;
                    case "double":
                    case "Double":
                        field.setDouble(obj, getDouble(key, 0));
                        break;
                    case "String":
                        field.set(obj, getString(key, ""));
                        break;
                    case "boolean":
                    case "Boolean":
                        field.setBoolean(obj, getBoolean(key, false));
                        break;
                }
            }
        }
        return obj;
    }
}
