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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ServletModelAttributeMethodProcessor;

/** Process {@link ParamName}. */
public class ParamNameProcessor extends ServletModelAttributeMethodProcessor {

  @Autowired private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

  private static final Map<Class<?>, Map<String, String>> PARAM_MAPPINGS_CACHE =
      new ConcurrentHashMap<>(256);
  private static final Map<Class<?>, Map<String, String>> METHODS_MAPPINGS_CACHE =
      new ConcurrentHashMap<>(256);

  public ParamNameProcessor() {
    super(false);
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(RequestParam.class)
        && !BeanUtils.isSimpleProperty(parameter.getParameterType())
        && (Arrays.stream(parameter.getParameterType().getDeclaredMethods())
                .anyMatch(method -> method.getAnnotation(ParamName.class) != null)
            || Arrays.stream(parameter.getParameterType().getDeclaredFields())
                .anyMatch(field -> field.getAnnotation(ParamName.class) != null));
  }

  @Override
  protected void bindRequestParameters(WebDataBinder binder, NativeWebRequest nativeWebRequest) {
    Object target = binder.getTarget();
    Map<String, String> paramMappings = this.getParamMappings(target.getClass());
    Map<String, String> methodMappings = this.getMethodMappings(target.getClass());
    ParamNameDataBinder paramNameDataBinder =
        new ParamNameDataBinder(target, binder.getObjectName(), paramMappings, methodMappings);
    requestMappingHandlerAdapter.getWebBindingInitializer().initBinder(paramNameDataBinder);
    super.bindRequestParameters(paramNameDataBinder, nativeWebRequest);
  }

  /**
   * Get param mappings. It creates a simple mapping: parameter name -> field name. Cache param
   * mappings in memory.
   */
  private Map<String, String> getParamMappings(Class<?> targetClass) {
    // first check cache
    if (PARAM_MAPPINGS_CACHE.containsKey(targetClass)) {
      return PARAM_MAPPINGS_CACHE.get(targetClass);
    }

    Map<String, String> paramMappings = new HashMap<>(32);

    // process fields
    Field[] fields = targetClass.getDeclaredFields();
    for (Field field : fields) {
      ParamName paramName = field.getAnnotation(ParamName.class);
      if (paramName != null && !paramName.value().isEmpty()) {
        paramMappings.put(paramName.value(), field.getName());
      }
    }

    // put them to cache
    PARAM_MAPPINGS_CACHE.put(targetClass, paramMappings);
    return paramMappings;
  }

  /**
   * Get param mappings. It creates a simple mapping: parameter name -> method name. Cache param
   * mappings in memory.
   */
  private Map<String, String> getMethodMappings(Class<?> targetClass) {
    // first check cache
    if (METHODS_MAPPINGS_CACHE.containsKey(targetClass)) {
      return METHODS_MAPPINGS_CACHE.get(targetClass);
    }

    Map<String, String> methodMappings = new HashMap<>(32);

    // process methods
    final Method[] methods = targetClass.getDeclaredMethods();
    for (Method method : methods) {
      final ParamName paramName = method.getAnnotation(ParamName.class);
      if (paramName != null && !paramName.value().isEmpty()) {
        methodMappings.put(paramName.value(), method.getName());
      }
    }

    // put them to cache
    METHODS_MAPPINGS_CACHE.put(targetClass, methodMappings);
    return methodMappings;
  }
}
