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

import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.domain.mail.DescriptorChangeSuggestionDataModel;
import org.gbif.registry.domain.mail.GrscicollChangeSuggestionDataModel;
import org.gbif.registry.domain.mail.GrscicollMasterSourceDeletedDataModel;
import org.gbif.registry.mail.BaseEmailModel;
import org.gbif.registry.mail.EmailTemplateProcessor;
import org.gbif.registry.mail.config.CollectionsMailConfigurationProperties;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

import freemarker.template.TemplateException;

import static com.google.common.base.Preconditions.checkArgument;

@Service
public class CollectionsEmailManager {

  private final EmailTemplateProcessor emailTemplateProcessors;
  private final CollectionsMailConfigurationProperties collectionsMailConfigurationProperties;
  private final String grscicollRegistryPortalUrl;

  public CollectionsEmailManager(
      @Qualifier("collectionsEmailTemplateProcessor")
          EmailTemplateProcessor emailTemplateProcessors,
      @Value("${grscicoll.registryPortal.url}") String grscicollRegistryPortalUrl,
      CollectionsMailConfigurationProperties collectionsMailConfigurationProperties) {
    Objects.requireNonNull(emailTemplateProcessors, "emailTemplateProcessors shall be provided");
    this.emailTemplateProcessors = emailTemplateProcessors;
    this.grscicollRegistryPortalUrl = grscicollRegistryPortalUrl;
    this.collectionsMailConfigurationProperties = collectionsMailConfigurationProperties;
  }

  public BaseEmailModel generateNewChangeSuggestionEmailModel(
      int suggestionKey,
      CollectionEntityType collectionEntityType,
      String entityName,
      Country entityCountry,
      @Nullable UUID entityKey,
      Type suggestionType,
      Set<String> recipients)
      throws IOException {
    return buildChangeSuggestionBaseEmailModel(
        suggestionKey,
        collectionEntityType,
        entityName,
        entityCountry,
        CollectionsEmailType.NEW_CHANGE_SUGGESTION,
        entityKey,
        suggestionType,
        recipients);
  }

  public BaseEmailModel generateAppliedChangeSuggestionEmailModel(
      int suggestionKey,
      CollectionEntityType collectionEntityType,
      String entityName,
      Country entityCountry,
      @Nullable UUID entityKey,
      Type suggestionType,
      Set<String> recipients)
      throws IOException {
    return buildChangeSuggestionBaseEmailModel(
        suggestionKey,
        collectionEntityType,
        entityName,
        entityCountry,
        CollectionsEmailType.APPLIED_CHANGE_SUGGESTION,
        entityKey,
        suggestionType,
        recipients);
  }

  public BaseEmailModel generateDiscardedChangeSuggestionEmailModel(
      int suggestionKey,
      CollectionEntityType collectionEntityType,
      String entityName,
      Country entityCountry,
      @Nullable UUID entityKey,
      Type suggestionType,
      Set<String> recipients)
      throws IOException {
    return buildChangeSuggestionBaseEmailModel(
        suggestionKey,
        collectionEntityType,
        entityName,
        entityCountry,
        CollectionsEmailType.DISCARDED_CHANGE_SUGGESTION,
        entityKey,
        suggestionType,
        recipients);
  }

  public BaseEmailModel generateNewDescriptorSuggestionEmailModel(
      long suggestionKey,
      UUID collectionKey,
      String collectionName,
      Country collectionCountry,
      Type suggestionType,
      String title,
      String description,
      String format,
      List<String> comments,
      Set<String> tags,
      String proposerEmail,
      Set<String> recipients)
      throws IOException {
    return buildDescriptorSuggestionBaseEmailModel(
        suggestionKey,
        collectionKey,
        collectionName,
        collectionCountry,
        CollectionsEmailType.NEW_DESCRIPTOR_SUGGESTION,
        suggestionType,
        title,
        description,
        format,
        comments,
        tags,
        proposerEmail,
        recipients);
  }

  public BaseEmailModel generateAppliedDescriptorSuggestionEmailModel(
      long suggestionKey,
      UUID collectionKey,
      String collectionName,
      Country collectionCountry,
      Type suggestionType,
      String title,
      String description,
      String format,
      List<String> comments,
      Set<String> tags,
      String proposerEmail,
      Set<String> recipients)
      throws IOException {
    return buildDescriptorSuggestionBaseEmailModel(
        suggestionKey,
        collectionKey,
        collectionName,
        collectionCountry,
        CollectionsEmailType.APPLIED_DESCRIPTOR_SUGGESTION,
        suggestionType,
        title,
        description,
        format,
        comments,
        tags,
        proposerEmail,
        recipients);
  }

  public BaseEmailModel generateDiscardedDescriptorSuggestionEmailModel(
      long suggestionKey,
      UUID collectionKey,
      String collectionName,
      Country collectionCountry,
      Type suggestionType,
      String title,
      String description,
      String format,
      List<String> comments,
      Set<String> tags,
      String proposerEmail,
      Set<String> recipients)
      throws IOException {
    return buildDescriptorSuggestionBaseEmailModel(
        suggestionKey,
        collectionKey,
        collectionName,
        collectionCountry,
        CollectionsEmailType.DISCARDED_DESCRIPTOR_SUGGESTION,
        suggestionType,
        title,
        description,
        format,
        comments,
        tags,
        proposerEmail,
        recipients);
  }

  public BaseEmailModel generateMasterSourceDeletedEmailModel(
      UUID collectionEntityKey,
      String collectionEntityName,
      CollectionEntityType collectionEntityType,
      UUID masterSourceKey,
      String masterSourceName,
      String masterSourceType)
      throws IOException {
    Objects.requireNonNull(collectionEntityKey);
    checkArgument(!Strings.isNullOrEmpty(collectionEntityName));
    Objects.requireNonNull(collectionEntityType);
    Objects.requireNonNull(masterSourceKey);
    checkArgument(!Strings.isNullOrEmpty(masterSourceName));
    Objects.requireNonNull(masterSourceType);

    try {
      URL collectionEntityLink =
          new URL(
              grscicollRegistryPortalUrl
                  + collectionEntityType.name().toLowerCase()
                  + "/"
                  + collectionEntityKey);

      URL masterSourceLink =
          new URL(
              grscicollRegistryPortalUrl + masterSourceType.toLowerCase() + "/" + masterSourceKey);

      GrscicollMasterSourceDeletedDataModel templateDataModel =
          new GrscicollMasterSourceDeletedDataModel();
      templateDataModel.setCollectionEntityLink(collectionEntityLink);
      templateDataModel.setCollectionEntityName(collectionEntityName);
      templateDataModel.setMasterSourceLink(masterSourceLink);
      templateDataModel.setMasterSourceName(masterSourceName);

      return emailTemplateProcessors.buildEmail(
          CollectionsEmailType.MASTER_SOURCE_DELETED,
          Collections.singleton(collectionsMailConfigurationProperties.getRecipient()),
          collectionsMailConfigurationProperties.getFrom(),
          templateDataModel,
          Locale.ENGLISH,
          Collections.emptySet());

    } catch (TemplateException e) {
      throw new IOException(e);
    }
  }

  private BaseEmailModel buildChangeSuggestionBaseEmailModel(
      int suggestionKey,
      CollectionEntityType entityType,
      String entityName,
      Country entityCountry,
      CollectionsEmailType emailType,
      @Nullable UUID entityKey,
      Type suggestionType,
      Set<String> recipients)
      throws IOException {
    BaseEmailModel baseEmailModel;
    try {
      URL suggestionUrl = null;
      if (suggestionType == Type.CREATE) {
        suggestionUrl =
            new URL(
                grscicollRegistryPortalUrl
                    + entityType.name().toLowerCase()
                    + "/create?suggestionId="
                    + suggestionKey);
      } else {
        suggestionUrl =
            new URL(
                grscicollRegistryPortalUrl
                    + entityType.name().toLowerCase()
                    + "/"
                    + entityKey
                    + "?suggestionId="
                    + suggestionKey);
      }

      GrscicollChangeSuggestionDataModel templateDataModel =
          new GrscicollChangeSuggestionDataModel();
      templateDataModel.setChangeSuggestionUrl(suggestionUrl);
      templateDataModel.setSuggestionType(suggestionType);
      templateDataModel.setEntityType(entityType);
      templateDataModel.setEntityName(entityName);
      templateDataModel.setEntityCountry(entityCountry != null ? entityCountry.name() : "");

      Set<String> finalRecipients = new HashSet<>();
      Set<String> ccRecipients = new HashSet<>();
      if (recipients != null && !recipients.isEmpty()) {
        finalRecipients = recipients;
        ccRecipients = Collections.singleton(collectionsMailConfigurationProperties.getRecipient());
      } else {
        finalRecipients.add(collectionsMailConfigurationProperties.getRecipient());
      }

      baseEmailModel =
          emailTemplateProcessors.buildEmail(
              emailType,
              finalRecipients,
              collectionsMailConfigurationProperties.getFrom(),
              templateDataModel,
              Locale.ENGLISH,
              ccRecipients);
    } catch (TemplateException e) {
      throw new IOException(e);
    }

    return baseEmailModel;
  }

  private BaseEmailModel buildDescriptorSuggestionBaseEmailModel(
      long suggestionKey,
      UUID collectionKey,
      String collectionName,
      Country collectionCountry,
      CollectionsEmailType emailType,
      Type suggestionType,
      String title,
      String description,
      String format,
      List<String> comments,
      Set<String> tags,
      String proposerEmail,
      Set<String> recipients)
      throws IOException {
    BaseEmailModel baseEmailModel;
    try {
      URL suggestionUrl = new URL(
          grscicollRegistryPortalUrl
              + "collection/"
              + collectionKey
              + "?descriptorSuggestionId="
              + suggestionKey);

      DescriptorChangeSuggestionDataModel templateDataModel =
          new DescriptorChangeSuggestionDataModel();
      templateDataModel.setChangeSuggestionUrl(suggestionUrl);
      templateDataModel.setSuggestionType(suggestionType);
      templateDataModel.setCollectionName(collectionName);
      templateDataModel.setCollectionCountry(collectionCountry != null ? collectionCountry.name() : "");
      templateDataModel.setTitle(title);
      templateDataModel.setDescription(description);
      templateDataModel.setFormat(format);
      templateDataModel.setComments(comments);
      templateDataModel.setTags(tags);
      templateDataModel.setProposerEmail(proposerEmail);

      Set<String> finalRecipients = new HashSet<>();
      Set<String> ccRecipients = new HashSet<>();
      if (recipients != null && !recipients.isEmpty()) {
        finalRecipients = recipients;
        ccRecipients = Collections.singleton(collectionsMailConfigurationProperties.getRecipient());
      } else {
        finalRecipients.add(collectionsMailConfigurationProperties.getRecipient());
      }

      baseEmailModel =
          emailTemplateProcessors.buildEmail(
              emailType,
              finalRecipients,
              collectionsMailConfigurationProperties.getFrom(),
              templateDataModel,
              Locale.ENGLISH,
              ccRecipients);
    } catch (TemplateException e) {
      throw new IOException(e);
    }

    return baseEmailModel;
  }
}
