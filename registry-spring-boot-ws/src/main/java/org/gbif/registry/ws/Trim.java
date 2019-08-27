package org.gbif.registry.ws;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TODO: 2019-08-20 add an interceptor (are there two trims? check)
/**
 * Indicates that the field is suitable for String trimming.
 * To use this annotate the method with Trim AND the fields intended for trimming.
 * The field must be:
 * <ul>
 * <li>A mutable POJO with getters and setters</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Inherited
public @interface Trim {
}