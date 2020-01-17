package org.gbif.registry.mail.config;

public class DevemailProperties {

  private Boolean enabled = false;

  private String address;

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }
}
