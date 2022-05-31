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
package org.gbif.registry.mail.pipelines;

import org.gbif.registry.domain.mail.PipelinesIdentifierIssueDataModel;
import org.gbif.registry.mail.BaseEmailModel;
import org.gbif.registry.mail.EmailTemplateProcessor;
import org.gbif.registry.mail.FreemarkerEmailTemplateProcessor;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import freemarker.template.TemplateException;

/**
 * Manager handling the different types of email related to pipelines workflow. Responsibilities
 * (with the help of (via {@link FreemarkerEmailTemplateProcessor}): - decide where to send the
 * email (which address) - generate the body of the email
 */
@Service
public class PipelinesEmailManager {

  private final EmailTemplateProcessor emailTemplateProcessors;
  private final String registryUrl;
  private final String mailFrom;
  private final String mailCc;

  public PipelinesEmailManager(
      @Qualifier("pipelinesEmailTemplateProcessor") EmailTemplateProcessor emailTemplateProcessors,
      @Value("${pipelines.registryUrl}") String registryUrl,
      @Value("${pipelines.mail.from}") String mailFrom,
      @Value("${pipelines.mail.cc}") String mailCc) {
    Objects.requireNonNull(emailTemplateProcessors, "emailTemplateProcessors shall be provided");
    this.emailTemplateProcessors = emailTemplateProcessors;
    this.registryUrl = registryUrl;
    this.mailFrom = mailFrom;
    this.mailCc = mailCc;
  }

  /**
   * @param datasetKey registry dataset identifier
   * @param attempt of crawler
   * @param datasetName title of a dataset
   * @param message info message from pipelines
   * @return the {@link BaseEmailModel} or null if the model can not be generated
   */
  public BaseEmailModel generateIdentifierIssueEmailModel(
      String datasetKey, int attempt, String datasetName, String message)
      throws IOException, TemplateException {

    PipelinesIdentifierIssueDataModel templateDataModel =
        PipelinesIdentifierIssueDataModel.build(
            registryUrl, datasetKey, attempt, datasetName, message);

    return emailTemplateProcessors.buildEmail(
        PipelinesEmailType.IDENTIFIER_FAILED,
        Collections.singleton(mailCc),
        mailFrom,
        templateDataModel,
        Locale.ENGLISH,
        Collections.emptySet(),
        datasetName);
  }
}
