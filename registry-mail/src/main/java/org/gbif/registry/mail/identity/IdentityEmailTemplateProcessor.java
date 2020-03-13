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
package org.gbif.registry.mail.identity;

import org.gbif.registry.mail.EmailDataProvider;
import org.gbif.registry.mail.FreemarkerEmailTemplateProcessor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("identityEmailTemplateProcessor")
public class IdentityEmailTemplateProcessor extends FreemarkerEmailTemplateProcessor {

  private EmailDataProvider emailDataProvider;

  /** @param emailDataProvider provides subject and template (depends on locale). */
  public IdentityEmailTemplateProcessor(
      @Qualifier("identityEmailDataProvider") EmailDataProvider emailDataProvider) {
    this.emailDataProvider = emailDataProvider;
  }

  @Override
  public EmailDataProvider getEmailDataProvider() {
    return emailDataProvider;
  }
}
