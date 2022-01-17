package org.gbif.registry.domain.ws;

import java.util.Objects;
import java.util.StringJoiner;

import javax.validation.constraints.NotNull;

public class IptNetworkBriefResponse {

  private String key;
  private String name;

  @NotNull
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  @NotNull
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IptNetworkBriefResponse that = (IptNetworkBriefResponse) o;
    return Objects.equals(key, that.key) && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, name);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", IptNetworkBriefResponse.class.getSimpleName() + "[", "]")
        .add("key='" + key + "'")
        .add("name='" + name + "'")
        .toString();
  }
}
