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
package org.gbif.registry.test.mocks;

import org.gbif.registry.domain.mail.BaseEmailModel;
import org.gbif.registry.mail.EmailSender;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple {@link EmailSender} implementation that keep the {@link BaseEmailModel} into memory. - For
 * testing only - 1 {@link BaseEmailModel} is stored per email address - no automatic cleanup
 */
public class InMemoryEmailSender implements EmailSender {

  private static final Logger LOG = LoggerFactory.getLogger(InMemoryEmailSender.class);

  private final Map<String, BaseEmailModel> emails = new HashMap<>();

  public InMemoryEmailSender() {
    LOG.debug("Use InMemoryEmailSender");
  }

  @Override
  public void send(BaseEmailModel baseEmailModel) {
    emails.put(baseEmailModel.getEmailAddress(), baseEmailModel);
  }

  public BaseEmailModel getEmail(String emailAddress) {
    return emails.get(emailAddress);
  }

  /** Clear all emails in memory */
  public void clear() {
    emails.clear();
  }

  @Override
  public String toString() {
    return emails.toString();
  }
}
