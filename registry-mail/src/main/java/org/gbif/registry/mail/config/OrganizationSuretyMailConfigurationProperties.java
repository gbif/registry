/*
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
