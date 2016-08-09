package com.technostart.playmate.core.cv.settings;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;


// TODO: объединить с SettingsManager-ом.
public class SettingsParser {
    public void fromSettings(Class clazz, SettingsManager manager) {

    }

    /**
     * Добавляет поля с настройками из аннотированных полей объекта.
     */
    public static void toSettings(Object obj, SettingsManager manager) throws IllegalAccessException {
        Class clazz = obj.getClass();
        List<Field> fields = Arrays.asList(clazz.getDeclaredFields());
        for (Field field : fields) {
            field.setAccessible(true);
            if (!field.isAnnotationPresent(Cfg.class)) continue;

            Type type = field.getType();
            switch (type.getTypeName()) {
                case "int":
                case "java.lang.Integer":
                    manager.putInt(field.getName(), (int) field.get(obj));
                    break;
                case "double":
                case "java.lang.Double":
                    manager.putDouble(field.getName(), (double) field.get(obj));
                    break;
                case "java.lang.String":
                    manager.putString(field.getName(), (String) field.get(obj));
                    break;
                case "boolean":
                case "java.lang.Boolean":
                    manager.putBoolean(field.getName(), (boolean) field.get(obj));
                    break;
            }
        }
    }

    /**
     * Заполняет поля класса соответствующими значениями из менеджера.
     */
    public static <T> T fromSettings(SettingsManager manager, T obj) throws IllegalAccessException {
        Class clazz = obj.getClass();
        List<Field> fields = Arrays.asList(clazz.getDeclaredFields());
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Cfg.class)) {
                Type type = field.getType();
                String key = field.getName();
                switch (type.getTypeName()) {
                    case "int":
                    case "java.lang.Integer":
                        field.setInt(obj, manager.getInt(key));
                        break;
                    case "double":
                    case "java.lang.Double":
                        field.setDouble(obj, manager.getDouble(key));
                        break;
                    case "java.lang.String":
                        field.set(obj, manager.getString(key));
                        break;
                    case "boolean":
                    case "java.lang.Boolean":
                        field.setBoolean(obj, manager.getBoolean(key));
                        break;
                }
            }
        }
        return obj;
    }
}
