package com.technostart.playmate.core.cv.settings;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

public class SettingsParser {
    public void fromSettings(Class clazz, SettingsManager manager) {

    }

    public static void toSettings(Object obj, SettingsManager manager) throws IllegalAccessException {
        Class clazz = obj.getClass();
        List<Field> fields = Arrays.asList(clazz.getDeclaredFields());
        for (Field field : fields) {
            field.setAccessible(true);
            if (!field.isAnnotationPresent(Cfg.class)) return;

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
}
