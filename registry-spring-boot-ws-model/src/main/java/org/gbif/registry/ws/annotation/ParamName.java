package org.gbif.registry.ws.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides parameter name when a form param name does not match a method param name.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ParamName {

  /**
   * The name of the request parameter to bind to.
   */
  String value();

  /**
   * For methods in order to know exactly what field should be used.
   * Required only for setter annotation.
   */
  String fieldName() default "";
}
