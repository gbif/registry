package org.gbif.registry.mail.collections;

import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.registry.domain.mail.GrscicollChangeSuggestionDataModel;
import org.gbif.registry.mail.BaseEmailModel;
import org.gbif.registry.mail.EmailTemplateProcessor;
import org.gbif.registry.mail.config.CollectionsMailConfigurationProperties;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import freemarker.template.TemplateException;

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
      @Nullable UUID entityKey,
      Type suggestionType)
      throws IOException {
    return buildBaseEmailModel(
        suggestionKey,
        collectionEntityType,
        CollectionsEmailType.NEW_CHANGE_SUGGESTION,
        entityKey,
        suggestionType,
        null);
  }

  public BaseEmailModel generateAppliedChangeSuggestionEmailModel(
      int suggestionKey,
      CollectionEntityType collectionEntityType,
      @Nullable UUID entityKey,
      Type suggestionType,
      Set<String> recipients)
      throws IOException {
    return buildBaseEmailModel(
        suggestionKey,
        collectionEntityType,
        CollectionsEmailType.APPLIED_CHANGE_SUGGESTION,
        entityKey,
        suggestionType,
        recipients);
  }

  public BaseEmailModel generateDiscardedChangeSuggestionEmailModel(
      int suggestionKey,
      CollectionEntityType collectionEntityType,
      @Nullable UUID entityKey,
      Type suggestionType,
      Set<String> recipients)
      throws IOException {
    return buildBaseEmailModel(
        suggestionKey,
        collectionEntityType,
        CollectionsEmailType.DISCARDED_CHANGE_SUGGESTION,
        entityKey,
        suggestionType,
        recipients);
  }

  private BaseEmailModel buildBaseEmailModel(
      int suggestionKey,
      CollectionEntityType entityType,
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

      Set<String> allRecipients = new HashSet<>();
      allRecipients.add(collectionsMailConfigurationProperties.getRecipient());
      if (recipients != null && !recipients.isEmpty()) {
        allRecipients.addAll(recipients);
      }

      baseEmailModel =
          emailTemplateProcessors.buildEmail(
              emailType,
              allRecipients,
              collectionsMailConfigurationProperties.getFrom(),
              templateDataModel,
              Locale.ENGLISH,
              Collections.emptySet());
    } catch (TemplateException e) {
      throw new IOException(e);
    }

    return baseEmailModel;
  }
}
