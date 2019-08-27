package org.gbif.registry.ws.provider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark Date parameter that can accept partial dates as input.
 * For example: 01-2018 or 01/2018 will be translated into 01-01-2018.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
public @interface PartialDate {}
