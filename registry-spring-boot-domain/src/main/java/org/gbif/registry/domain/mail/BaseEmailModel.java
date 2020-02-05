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
package org.gbif.registry.domain.mail;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.google.common.base.MoreObjects;

/** Very basic email model that holds the main components of an email to send. */
public class BaseEmailModel {

  @NotNull private final String emailAddress;
  private final String subject;
  private final String body;

  private final List<String> ccAddress;

  public BaseEmailModel(String emailAddress, String subject, String body) {
    this(emailAddress, subject, body, Collections.emptyList());
  }

  public BaseEmailModel(String emailAddress, String subject, String body, List<String> ccAddress) {
    this.emailAddress = emailAddress;
    this.subject = subject;
    this.body = body;
    this.ccAddress = ccAddress;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public List<String> getCcAddress() {
    return ccAddress;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("emailAddress", emailAddress)
        .add("subject", subject)
        .add("body", body)
        .add("ccAddress", ccAddress)
        .toString();
  }
}
