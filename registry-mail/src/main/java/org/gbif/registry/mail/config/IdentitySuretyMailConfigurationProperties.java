/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    private String changeEmail;

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

    public String getChangeEmail() {
      return changeEmail;
    }

    public void setChangeEmail(String changeEmail) {
      this.changeEmail = changeEmail;
    }
  }
}
