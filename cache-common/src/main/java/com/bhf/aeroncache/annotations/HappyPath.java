package com.bhf.aeroncache.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for Happy Path tests.
 * Inspired by <a href="https://github.com/bhf/agni-annotations"> Agni Annotations </a>.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface HappyPath {
}

