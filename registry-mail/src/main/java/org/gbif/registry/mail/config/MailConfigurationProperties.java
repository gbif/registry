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

import java.util.Collections;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mail")
public class MailConfigurationProperties {

  private DevemailProperties devemail;

  private String from;

  private Set<String> cc = Collections.emptySet();

  private Set<String> bcc = Collections.emptySet();

  private Boolean enabled = Boolean.FALSE;

  public DevemailProperties getDevemail() {
    return devemail;
  }

  public void setDevemail(DevemailProperties devemail) {
    this.devemail = devemail;
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
