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
package org.gbif.registry.mail;

import java.util.Collections;
import java.util.Set;

import com.google.common.base.MoreObjects;

import lombok.Getter;

/** Very basic email model that holds the main components of an email to send. */
@Getter
public class BaseEmailModel {

  private final Set<String> emailAddresses;
  private final String subject;
  private final String body;
  private final Set<String> ccAddresses;
  private final String from;

  public BaseEmailModel(Set<String> emailAddresses, String subject, String body) {
    this(emailAddresses, subject, body, Collections.emptySet());
  }

  public BaseEmailModel(
      Set<String> emailAddresses, String subject, String body, Set<String> ccAddresses) {
    this(emailAddresses, subject, body, ccAddresses, null);
  }

  public BaseEmailModel(
      Set<String> emailAddresses,
      String subject,
      String body,
      Set<String> ccAddresses,
      String from) {
    this.emailAddresses = emailAddresses != null ? emailAddresses : Collections.emptySet();
    this.subject = subject;
    this.body = body;
    this.ccAddresses = ccAddresses != null ? ccAddresses : Collections.emptySet();
    this.from = from;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("emailAddresses", emailAddresses)
        .add("subject", subject)
        .add("body", body)
        .add("ccAddresses", ccAddresses)
        .add("from", from)
        .toString();
  }
}
