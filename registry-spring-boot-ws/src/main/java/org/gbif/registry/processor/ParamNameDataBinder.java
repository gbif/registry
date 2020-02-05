/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.processor;

import org.gbif.registry.domain.ws.annotation.ParamName;

import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;

/** ServletRequestDataBinder which supports fields renaming using {@link ParamName} */
public class ParamNameDataBinder extends ExtendedServletRequestDataBinder {

  private static final Logger LOG = LoggerFactory.getLogger(ParamNameDataBinder.class);

  private final Map<String, String> paramMappings;
  private final Map<String, String> methodMappings;

  public ParamNameDataBinder(
      Object target,
      String objectName,
      Map<String, String> paramMappings,
      Map<String, String> methodMappings) {
    super(target, objectName);
    this.paramMappings = paramMappings;
    this.methodMappings = methodMappings;
  }

  /**
   * Using a mapping which was created by {@link ParamNameProcessor} this method binds actual values
   * to the parameters.
   */
  @Override
  protected void addBindValues(
      MutablePropertyValues mutablePropertyValues, ServletRequest request) {
    super.addBindValues(mutablePropertyValues, request);
    for (Map.Entry<String, String> entry : paramMappings.entrySet()) {
      String paramName = entry.getKey();
      String fieldName = entry.getValue();
      if (mutablePropertyValues.contains(paramName)) {
        mutablePropertyValues.add(
            fieldName, mutablePropertyValues.getPropertyValue(paramName).getValue());
      }
    }

    for (Map.Entry<String, String> entry : methodMappings.entrySet()) {
      String paramName = entry.getKey();
      String methodName = entry.getValue();
      if (mutablePropertyValues.contains(paramName)) {
        try {
          final Method declaredMethod =
              getTarget().getClass().getDeclaredMethod(methodName, String.class);
          declaredMethod.invoke(
              getTarget(), mutablePropertyValues.getPropertyValue(paramName).getValue());
        } catch (Exception e) {
          LOG.error("There was a problem to invoke a method {}", methodName);
        }
      }
    }
  }
}
