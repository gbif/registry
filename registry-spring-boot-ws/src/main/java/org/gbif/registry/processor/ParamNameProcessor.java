package org.gbif.registry.processor;

import org.gbif.registry.ws.annotation.ParamName;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ServletModelAttributeMethodProcessor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process {@link ParamName}.
 */
public class ParamNameProcessor extends ServletModelAttributeMethodProcessor {

  @Autowired
  private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

  private static final Map<Class<?>, Map<String, String>> PARAM_MAPPINGS_CACHE = new ConcurrentHashMap<>(256);

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
    ParamNameDataBinder paramNameDataBinder = new ParamNameDataBinder(target, binder.getObjectName(), paramMappings);
    requestMappingHandlerAdapter.getWebBindingInitializer().initBinder(paramNameDataBinder, nativeWebRequest);
    super.bindRequestParameters(paramNameDataBinder, nativeWebRequest);
  }

  /**
   * Get param mappings. It creates a simple mapping: parameter name -> field name.
   * Cache param mappings in memory.
   *
   * @return {@link Map}
   */
  private Map<String, String> getParamMappings(Class<?> targetClass) {
    // first check cache
    if (PARAM_MAPPINGS_CACHE.containsKey(targetClass)) {
      return PARAM_MAPPINGS_CACHE.get(targetClass);
    }

    // process fields
    Field[] fields = targetClass.getDeclaredFields();
    Map<String, String> paramMappings = new HashMap<>(32);
    for (Field field : fields) {
      ParamName paramName = field.getAnnotation(ParamName.class);
      if (paramName != null && !paramName.value().isEmpty()) {
        paramMappings.put(paramName.value(), field.getName());
      }
    }

    // process methods
    final Method[] methods = targetClass.getDeclaredMethods();
    for (Method method : methods) {
      final ParamName paramName = method.getAnnotation(ParamName.class);
      if (paramName != null && !paramName.value().isEmpty()) {
        final String s = paramName.fieldName();
        if (s.isEmpty()) {
          throw new IllegalStateException("Field name should be specified (only for setter annotation)");
        }
        paramMappings.put(paramName.value(), paramName.fieldName());
      }
    }

    // put them to cache
    PARAM_MAPPINGS_CACHE.put(targetClass, paramMappings);
    return paramMappings;
  }
}
