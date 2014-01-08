package org.gbif.registry.metasync.protocols.biocase.model;

import com.google.common.base.Objects;
import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetProperty;

@ObjectCreate(pattern = "response/content/capabilities/SupportedSchemas/Concept")
public class BiocaseConcept {

  @SetProperty(pattern = "response/content/capabilities/SupportedSchemas/Concept", attributeName = "searchable")
  private boolean searchable = true;

  @SetProperty(pattern = "response/content/capabilities/SupportedSchemas/Concept", attributeName = "dataType")
  private String dataType;

  @BeanPropertySetter(pattern = "response/content/capabilities/SupportedSchemas/Concept")
  private String name;

  public boolean isSearchable() {
    return searchable;
  }

  public void setSearchable(boolean searchable) {
    this.searchable = searchable;
  }

  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("searchable", searchable)
      .add("dataType", dataType)
      .add("name", name)
      .toString();
  }

}
