package com.technostart.playmate.core.settings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cfg {
    // Пробела заведомо нет в имени поля класса.
    String name() default " defaultName";
}
