package org.swiftbotsample.cqrs.annotations;

import org.swiftbotsample.app.ButtonName;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandAttribute {
    Class<? extends Enum<? extends MenuState>> menu();
    int ordinal();
    int priority();
    ButtonName[] buttons() default {};
}