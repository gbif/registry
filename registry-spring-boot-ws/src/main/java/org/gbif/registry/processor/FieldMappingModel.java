package org.gbif.registry.processor;

/**
 * It's used by ParamName processor.
 */
public class FieldMappingModel {

  private String methodName;

  private String fieldName;

  public FieldMappingModel(String methodName, String fieldName) {
    this.methodName = methodName;
    this.fieldName = fieldName;
  }

  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }
}
