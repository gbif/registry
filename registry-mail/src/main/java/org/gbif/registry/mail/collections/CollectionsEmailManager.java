package org.gbif.registry.mail.collections;

import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.registry.domain.mail.GrscicollChangeSuggestionDataModel;
import org.gbif.registry.mail.BaseEmailModel;
import org.gbif.registry.mail.EmailTemplateProcessor;
import org.gbif.registry.mail.config.CollectionsMailConfigurationProperties;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

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
      int suggestionKey, CollectionEntityType collectionEntityType) throws IOException {
    return buildBaseEmailModel(
        suggestionKey, collectionEntityType, CollectionsEmailType.NEW_CHANGE_SUGGESTION);
  }

  public BaseEmailModel generateAppliedChangeSuggestionEmailModel(
      int suggestionKey, CollectionEntityType collectionEntityType) throws IOException {
    return buildBaseEmailModel(
        suggestionKey, collectionEntityType, CollectionsEmailType.APPLIED_CHANGE_SUGGESTION);
  }

  public BaseEmailModel generateDiscardedChangeSuggestionEmailModel(
      int suggestionKey, CollectionEntityType collectionEntityType) throws IOException {
    return buildBaseEmailModel(
        suggestionKey, collectionEntityType, CollectionsEmailType.DISCARDED_CHANGE_SUGGESTION);
  }

  private BaseEmailModel buildBaseEmailModel(
      int suggestionKey, CollectionEntityType entityType, CollectionsEmailType emailType)
      throws IOException {
    BaseEmailModel baseEmailModel;
    try {
      URL suggestionUrl =
          new URL(
              grscicollRegistryPortalUrl
                  + entityType.name().toLowerCase()
                  + "/changeSuggestion/"
                  + suggestionKey);

      GrscicollChangeSuggestionDataModel templateDataModel =
          new GrscicollChangeSuggestionDataModel();
      templateDataModel.setChangeSuggestionUrl(suggestionUrl);

      baseEmailModel =
          emailTemplateProcessors.buildEmail(
              emailType,
              Collections.singleton(collectionsMailConfigurationProperties.getRecipient()),
              templateDataModel,
              Locale.ENGLISH);
    } catch (TemplateException e) {
      throw new IOException(e);
    }

    return baseEmailModel;
  }
}
