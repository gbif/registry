package org.gbif.registry.processor;

import org.gbif.registry.ws.annotation.ParamName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;

import javax.servlet.ServletRequest;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * ServletRequestDataBinder which supports fields renaming using {@link ParamName}
 */
public class ParamNameDataBinder extends ExtendedServletRequestDataBinder {

  private static final Logger LOG = LoggerFactory.getLogger(ParamNameDataBinder.class);

  private final Map<String, String> paramMappings;
  private final Map<String, FieldMappingModel> methodMappings;

  public ParamNameDataBinder(Object target, String objectName, Map<String, String> paramMappings,
                             Map<String, FieldMappingModel> methodMappings) {
    super(target, objectName);
    this.paramMappings = paramMappings;
    this.methodMappings = methodMappings;
  }

  /**
   * Using a mapping which was created by {@link ParamNameProcessor} this method binds actual values to the parameters.
   */
  @Override
  protected void addBindValues(MutablePropertyValues mutablePropertyValues, ServletRequest request) {
    super.addBindValues(mutablePropertyValues, request);
    for (Map.Entry<String, String> entry : paramMappings.entrySet()) {
      String paramName = entry.getKey();
      String fieldName = entry.getValue();
      if (mutablePropertyValues.contains(paramName)) {
        mutablePropertyValues.add(fieldName, mutablePropertyValues.getPropertyValue(paramName).getValue());
      }
    }

    for (Map.Entry<String, FieldMappingModel> entry : methodMappings.entrySet()) {
      String paramName = entry.getKey();
      String methodName = entry.getValue().getMethodName();
      if (mutablePropertyValues.contains(paramName)) {
        try {
          final Method declaredMethod = getTarget().getClass().getDeclaredMethod(methodName, String.class);
          final String result = (String) declaredMethod
              .invoke(getTarget(), mutablePropertyValues.getPropertyValue(paramName).getValue());
          mutablePropertyValues.add(entry.getValue().getFieldName(), result);
        } catch (Exception e) {
          LOG.error("There was a problem to invoke a method {}", methodName);
        }
      }
    }
  }
}
