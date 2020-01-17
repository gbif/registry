package org.gbif.registry.mail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "mail")
public class MailConfigurationProperties {

  private DevemailProperties devemail;

  private List<String> cc = Collections.emptyList();

  private List<String> bcc = Collections.emptyList();

  public DevemailProperties getDevemail() {
    return devemail;
  }

  public void setDevemail(DevemailProperties devemail) {
    this.devemail = devemail;
  }

  public List<String> getCc() {
    return cc;
  }

  public void setCc(List<String> cc) {
    this.cc = cc;
  }

  public List<String> getBcc() {
    return bcc;
  }

  public void setBcc(List<String> bcc) {
    this.bcc = bcc;
  }

  public static class DevemailProperties {

    private Boolean enabled = Boolean.FALSE;

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
}
