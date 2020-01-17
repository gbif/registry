package org.gbif.registry.mail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "organization.surety.mail")
public class OrganizationSuretyMailConfigurationProperties {

  private String helpdesk;

  private Boolean enabled = Boolean.FALSE;

  private UrlTemplateProperties urlTemplate;

  public String getHelpdesk() {
    return helpdesk;
  }

  public void setHelpdesk(String helpdesk) {
    this.helpdesk = helpdesk;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public UrlTemplateProperties getUrlTemplate() {
    return urlTemplate;
  }

  public void setUrlTemplate(UrlTemplateProperties urlTemplate) {
    this.urlTemplate = urlTemplate;
  }

  public static class UrlTemplateProperties {

    private String confirmOrganization;

    private String organization;

    public String getConfirmOrganization() {
      return confirmOrganization;
    }

    public void setConfirmOrganization(String confirmOrganization) {
      this.confirmOrganization = confirmOrganization;
    }

    public String getOrganization() {
      return organization;
    }

    public void setOrganization(String organization) {
      this.organization = organization;
    }
  }
}
