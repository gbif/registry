package org.gbif.registry.processor;

import org.gbif.registry.ws.annotation.ParamName;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;

import javax.servlet.ServletRequest;
import java.util.Map;

/**
 * ServletRequestDataBinder which supports fields renaming using {@link ParamName}
 */
public class ParamNameDataBinder extends ExtendedServletRequestDataBinder {

  private final Map<String, String> paramMappings;

  public ParamNameDataBinder(Object target, String objectName, Map<String, String> paramMappings) {
    super(target, objectName);
    this.paramMappings = paramMappings;
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
  }
}
