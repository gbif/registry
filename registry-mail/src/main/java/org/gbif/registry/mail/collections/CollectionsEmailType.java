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
package org.gbif.registry.mail.collections;

import org.gbif.registry.mail.EmailType;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.context.support.ResourceBundleMessageSource;

/** Type of emails related to organization endorsement */
public enum CollectionsEmailType implements EmailType {
  NEW_CHANGE_SUGGESTION("newChangeSuggestion", "grscicoll_suggestion_created.ftl"),
  APPLIED_CHANGE_SUGGESTION("appliedChangeSuggestion", "grscicoll_suggestion_applied.ftl"),
  DISCARDED_CHANGE_SUGGESTION("discardedChangeSuggestion", "grscicoll_suggestion_discarded.ftl"),
  MASTER_SOURCE_DELETED("masterSourceDeleted", "grscicoll_master_source_deleted.ftl"),
  NEW_DESCRIPTOR_SUGGESTION("newDescriptorSuggestion", "descriptor_suggestion_created.ftl"),
  APPLIED_DESCRIPTOR_SUGGESTION("appliedDescriptorSuggestion", "descriptor_suggestion_applied.ftl"),
  DISCARDED_DESCRIPTOR_SUGGESTION("discardedDescriptorSuggestion", "descriptor_suggestion_discarded.ftl");

  private static final ResourceBundleMessageSource MESSAGE_SOURCE;

  private final String key;
  private final String template;

  static {
    MESSAGE_SOURCE = new ResourceBundleMessageSource();
    MESSAGE_SOURCE.setBasename("email/subjects/collections_email_subjects");
    MESSAGE_SOURCE.setDefaultEncoding(StandardCharsets.UTF_8.displayName());
  }

  CollectionsEmailType(String key, String template) {
    this.key = key;
    this.template = template;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String getTemplate() {
    return template;
  }

  @Override
  public String getSubject(Locale locale, String... subjectParams) {
    return MESSAGE_SOURCE.getMessage(this.getKey(), subjectParams, locale);
  }
}
