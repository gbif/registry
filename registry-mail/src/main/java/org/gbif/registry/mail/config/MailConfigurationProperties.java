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

import java.util.Collections;
import java.util.Set;

import lombok.Getter;

import lombok.Setter;

import org.gbif.registry.mail.EmailCategory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mail")
public class MailConfigurationProperties {

  private String from;

  private Set<String> cc = Collections.emptySet();

  private Set<String> bcc = Collections.emptySet();

  private Boolean enabled = Boolean.FALSE;

  private DevemailProperties devEmailForIdentity;
  private DevemailProperties devEmailForOrganizationsEndorsement;
  private DevemailProperties devEmailForCollections;
  private DevemailProperties devEmailForPipelines;

  public DevemailProperties getDevEmailForIdentity() {
    return devEmailForIdentity;
  }

  public void setDevEmailForIdentity(DevemailProperties devEmailForIdentity) {
    this.devEmailForIdentity = devEmailForIdentity;
  }

  public DevemailProperties getDevEmailForOrganizationsEndorsement() {
    return devEmailForOrganizationsEndorsement;
  }

  public void setDevEmailForOrganizationsEndorsement(
      DevemailProperties devEmailForOrganizationsEndorsement) {
    this.devEmailForOrganizationsEndorsement = devEmailForOrganizationsEndorsement;
  }

  public DevemailProperties getDevEmailForCollections() {
    return devEmailForCollections;
  }

  public void setDevEmailForCollections(DevemailProperties devEmailForCollections) {
    this.devEmailForCollections = devEmailForCollections;
  }

  public DevemailProperties getDevEmailForPipelines() {
    return devEmailForPipelines;
  }

  public void setDevEmailForPipelines(DevemailProperties devEmailForPipelines) {
    this.devEmailForPipelines = devEmailForPipelines;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public Set<String> getCc() {
    return cc;
  }

  public void setCc(Set<String> cc) {
    this.cc = cc;
  }

  public Set<String> getBcc() {
    return bcc;
  }

  public void setBcc(Set<String> bcc) {
    this.bcc = bcc;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns the address to redirect to for dev mode, or null if no redirect.
   * Uses the category-specific devEmailForX configuration.
   */
  public String getRedirectAddress(EmailCategory category) {
    if (category == null) {
      return null;
    }
    DevemailProperties props = null;
    switch (category) {
      case IDENTITY:
        props = getDevEmailForIdentity();
        break;
      case ORGANIZATION_ENDORSEMENT:
        props = getDevEmailForOrganizationsEndorsement();
        break;
      case COLLECTIONS:
        props = getDevEmailForCollections();
        break;
      case PIPELINES:
        props = getDevEmailForPipelines();
        break;
      default:
        return null;
    }
    if (props != null
        && Boolean.TRUE.equals(props.getEnabled())
        && props.getAddress() != null
        && !props.getAddress().isEmpty()) {
      return props.getAddress();
    }
    return null;
  }

  @Setter
  @Getter
  public static class DevemailProperties {

    private Boolean enabled = Boolean.FALSE;

    private String address;

  }
}
