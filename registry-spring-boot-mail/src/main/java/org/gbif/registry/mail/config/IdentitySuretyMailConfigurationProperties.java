package org.gbif.registry.mail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "identity.surety.mail")
public class IdentitySuretyMailConfigurationProperties {

  private UrlTemplateProperties urlTemplate;

  public UrlTemplateProperties getUrlTemplate() {
    return urlTemplate;
  }

  public void setUrlTemplate(UrlTemplateProperties urlTemplate) {
    this.urlTemplate = urlTemplate;
  }

  public static class UrlTemplateProperties {

    private String confirmUser;

    private String resetPassword;

    public String getConfirmUser() {
      return confirmUser;
    }

    public void setConfirmUser(String confirmUser) {
      this.confirmUser = confirmUser;
    }

    public String getResetPassword() {
      return resetPassword;
    }

    public void setResetPassword(String resetPassword) {
      this.resetPassword = resetPassword;
    }
  }
}
