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
package org.gbif.registry.directory.client.config;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import feign.MethodMetadata;
import feign.Util;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;
import static org.springframework.core.annotation.AnnotationUtils.synthesizeAnnotation;

class HierarchicalContract extends SpringMvcContract {

  private ResourceLoader resourceLoader;

  @Override
  public List<MethodMetadata> parseAndValidatateMetadata(final Class<?> targetType) {
    checkState(
        targetType.getTypeParameters().length == 0,
        "Parameterized types unsupported: %s",
        targetType.getSimpleName());
    final Map<String, MethodMetadata> result = new LinkedHashMap<>();

    for (final Method method : targetType.getMethods()) {
      if (method.getDeclaringClass() == Object.class
          || (method.getModifiers() & Modifier.STATIC) != 0
          || Util.isDefault(method)
          // skip default methods which related to generic inheritance
          // also default methods are considered as "unsupported operations"
          || method.toString().startsWith("public default")) {
        continue;
      }
      final MethodMetadata metadata = this.parseAndValidateMetadata(targetType, method);
      checkState(
          !result.containsKey(metadata.configKey()),
          "Overrides unsupported: %s",
          metadata.configKey());
      result.put(metadata.configKey(), metadata);
    }

    return new ArrayList<>(result.values());
  }

  @Override
  public MethodMetadata parseAndValidateMetadata(final Class<?> targetType, final Method method) {
    final MethodMetadata methodMetadata = super.parseAndValidateMetadata(targetType, method);

    final ArrayDeque<Class<?>> classHierarchy = new ArrayDeque<>();
    classHierarchy.add(targetType);
    this.findClass(targetType, method.getDeclaringClass(), classHierarchy);
    classHierarchy.stream()
        .map(this::processPathValue)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .ifPresent(path -> methodMetadata.template().target(path));
    return methodMetadata;
  }

  private Optional<String> processPathValue(final Class<?> clz) {
    Optional<String> result = Optional.empty();
    final RequestMapping classAnnotation = clz.getAnnotation(RequestMapping.class);
    if (classAnnotation != null) {
      final RequestMapping synthesizeAnnotation = synthesizeAnnotation(classAnnotation, clz);
      // Prepend path from class annotation if specified
      if (synthesizeAnnotation.value().length > 0) {
        String pathValue = emptyToNull(synthesizeAnnotation.value()[0]);
        pathValue = this.resolveValue(pathValue);
        if (!pathValue.startsWith("/")) {
          pathValue = "/" + pathValue;
        }
        result = Optional.of(pathValue);
      }
    }
    return result;
  }

  private String resolveValue(final String value) {
    if (StringUtils.hasText(value)
        && this.resourceLoader instanceof ConfigurableApplicationContext) {
      return ((ConfigurableApplicationContext) this.resourceLoader)
          .getEnvironment()
          .resolvePlaceholders(value);
    }
    return value;
  }

  @Override
  protected void processAnnotationOnClass(final MethodMetadata data, final Class<?> clz) {
    // skip this step
  }

  private boolean findClass(
      final Class<?> currentClass,
      final Class<?> searchClass,
      final ArrayDeque<Class<?>> classHierarchy) {
    if (currentClass == searchClass) {
      return true;
    }
    final Class<?>[] interfaces = currentClass.getInterfaces();
    for (final Class<?> currentInterface : interfaces) {
      classHierarchy.add(currentInterface);
      final boolean findClass = this.findClass(currentInterface, searchClass, classHierarchy);
      if (findClass) {
        return true;
      }
      classHierarchy.removeLast();
    }
    return false;
  }

  @Override
  public void setResourceLoader(final ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
    super.setResourceLoader(resourceLoader);
  }
}
