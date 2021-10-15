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
package org.gbif.registry.domain.mail;

import java.net.URL;

public class AccountChangeEmailTemplateDataModel extends ConfirmableTemplateDataModel {

  private final String currentEmail;
  private final String newEmail;

  public AccountChangeEmailTemplateDataModel(
      String name, URL url, String currentEmail, String newEmail) {
    super(name, url);
    this.currentEmail = currentEmail;
    this.newEmail = newEmail;
  }

  public String getCurrentEmail() {
    return currentEmail;
  }

  public String getNewEmail() {
    return newEmail;
  }
}
