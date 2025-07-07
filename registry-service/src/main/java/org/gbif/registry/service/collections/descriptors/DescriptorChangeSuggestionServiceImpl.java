package org.gbif.registry.service.collections.descriptors;

import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_EDITOR_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_MEDIATOR_ROLE;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.descriptors.DescriptorChangeSuggestion;
import org.gbif.api.model.collections.descriptors.DescriptorChangeSuggestionRequest;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.DescriptorChangeSuggestionService;
import org.gbif.api.service.collections.DescriptorsService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.EventType;
import org.gbif.registry.events.collections.SubEntityCollectionEvent;
import org.gbif.registry.mail.BaseEmailModel;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.collections.CollectionsEmailManager;
import org.gbif.registry.mail.config.CollectionsMailConfigurationProperties;
import org.gbif.registry.persistence.mapper.UserMapper;
import org.gbif.registry.persistence.mapper.collections.DescriptorChangeSuggestionMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DescriptorChangeSuggestionServiceImpl implements DescriptorChangeSuggestionService {

  @Value("${grscicoll.descriptorSuggestions.storage}")
  private String descriptorSuggestionsStoragePath;

  private final DescriptorChangeSuggestionMapper suggestionMapper;
  private final DescriptorsService descriptorsService;
  private final EventManager eventManager;
  private final CollectionService collectionService;
  private final EmailSender emailSender;
  private final CollectionsEmailManager emailManager;
  private final CollectionsMailConfigurationProperties collectionsMailConfigurationProperties;
  private final UserMapper userMapper;

  @Autowired
  public DescriptorChangeSuggestionServiceImpl(DescriptorChangeSuggestionMapper suggestionMapper,
    DescriptorsService descriptorsService,
    EventManager eventManager,
    CollectionService collectionService,
    EmailSender emailSender,
    CollectionsEmailManager emailManager,
    CollectionsMailConfigurationProperties collectionsMailConfigurationProperties,
    UserMapper userMapper) {
    this.suggestionMapper = suggestionMapper;
    this.descriptorsService = descriptorsService;
    this.eventManager = eventManager;
    this.collectionService = collectionService;
    this.emailSender = emailSender;
    this.emailManager = emailManager;
    this.collectionsMailConfigurationProperties = collectionsMailConfigurationProperties;
    this.userMapper = userMapper;
  }

  @Override
  public DescriptorChangeSuggestion createSuggestion(
    InputStream fileStream,
    String fileName,
    DescriptorChangeSuggestionRequest request)
    throws IOException {
    Preconditions.checkArgument(!request.getComments().isEmpty(), "Comment is required");

    DescriptorChangeSuggestion suggestion = buildSuggestion(request);

    // Set countryIsoCode from collection address
    Collection collection = collectionService.get(request.getCollectionKey());
    if (collection != null) {
      Country country = null;
      if (collection.getAddress() != null && collection.getAddress().getCountry() != null) {
        country = collection.getAddress().getCountry();
      } else if (collection.getMailingAddress() != null && collection.getMailingAddress().getCountry() != null) {
        country = collection.getMailingAddress().getCountry();
      }
      suggestion.setCountry(country);
    }

    if (fileName != null || fileStream != null) {
      String filename = generateFilename(String.valueOf(request.getCollectionKey()), fileName);
      Path filePath = saveFile(fileStream, filename);
      suggestion.setSuggestedFile(filePath.toString());
    }

    suggestionMapper.createSuggestion(suggestion);

    eventManager.post(
      SubEntityCollectionEvent.newInstance(
        request.getCollectionKey(),
        Collection.class,
        DescriptorChangeSuggestion.class,
        suggestion.getKey(),
        EventType.CREATE));

    // send email
    if (Boolean.TRUE.equals(collectionsMailConfigurationProperties.getEnabled())) {
      try {
        BaseEmailModel emailModel =
            emailManager.generateNewDescriptorSuggestionEmailModel(
                suggestion.getKey(),
                request.getCollectionKey(),
                collection.getName(),
                suggestion.getCountry(),
                suggestion.getType(),
                suggestion.getTitle(),
                suggestion.getDescription(),
                suggestion.getFormat() != null ? suggestion.getFormat().name() : "",
                suggestion.getComments(),
                suggestion.getTags(),
                suggestion.getProposerEmail(),
                findRecipientsWithPermissions(request.getCollectionKey(), suggestion.getCountry()));
        emailSender.send(emailModel);
      } catch (Exception e) {
        log.error("Couldn't send email for new descriptor suggestion", e);
      }
    }

    return suggestion;
  }

  @Override
  public DescriptorChangeSuggestion updateSuggestion(
    long key,
    DescriptorChangeSuggestionRequest request,
    InputStream fileStream,
    String fileName) throws IOException {

    // Retrieve the existing suggestion
    DescriptorChangeSuggestion suggestion = getSuggestion(key);

    if (suggestion == null) {
      throw new IllegalArgumentException("Suggestion not found with key: " + key);
    }

    // Ensure suggestion is in PENDING status to allow update
    if (!Status.PENDING.equals(suggestion.getStatus())) {
      throw new IllegalStateException("Only pending suggestions can be updated");
    }

    // Update the fields of the suggestion
    if (request.getTitle() != null) {
      suggestion.setTitle(request.getTitle());
    }
    if (request.getDescription() != null) {
      suggestion.setDescription(request.getDescription());
    }
    if (request.getComments() != null && !request.getComments().isEmpty()) {
      suggestion.setComments(request.getComments());
    }
    if (request.getTags() != null) {
      suggestion.setTags(request.getTags());
    }

    // Update the file if a new file is provided
    if (fileStream != null) {
      deleteSuggestionFile(suggestion);
      String filename = generateFilename(String.valueOf(request.getCollectionKey()), fileName);
      Path filePath = saveFile(fileStream, filename);
      suggestion.setSuggestedFile(filePath.toString());
    }

    suggestion.setModifiedBy(getUsername());

    // Persist the updated suggestion in the database
    suggestionMapper.updateSuggestion(suggestion);

    eventManager.post(
      SubEntityCollectionEvent.newInstance(
        request.getCollectionKey(),
        Collection.class,
        DescriptorChangeSuggestion.class,
        suggestion.getKey(),
        EventType.UPDATE));

    log.info("Updated descriptor suggestion with key: {}", key);

    return suggestion;
  }

  @Override
  public DescriptorChangeSuggestion getSuggestion(long key) {
    return suggestionMapper.findByKey(key);
  }

  @Override
  public InputStream getSuggestionFile(long key) throws IOException {
    DescriptorChangeSuggestion suggestion = getSuggestion(key);

    if (suggestion == null || suggestion.getSuggestedFile() == null) {
      return null;
    }

    Path filePath = Paths.get(suggestion.getSuggestedFile());
    PathResource resource = new PathResource((filePath));

    if (!resource.exists()) {
      throw new IllegalStateException("Suggested file for descriptor change suggestion with key "
        + key + " was not found at the expected location: " + filePath);
    }

    return resource.getInputStream();
  }

  @Override
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  public void applySuggestion(long key) throws IOException {
    DescriptorChangeSuggestion suggestion = getSuggestion(key);
    if (suggestion == null) {
      throw new IllegalArgumentException("Descriptor suggestion was not found with the key: " + key);
    }

    if (suggestion.getStatus() != Status.PENDING) {
      throw new IllegalStateException(
        "Descriptor suggestion is not in PENDING status");
    }

    byte[] fileBytes = null;
    try (InputStream inputStream = getSuggestionFile(key)) {
      if (inputStream != null) {
        fileBytes = inputStream.readAllBytes();
      }
    }

    // Apply the suggestion based on type
    if (Type.CREATE.equals(suggestion.getType())) {
      long descriptorGroupKey = descriptorsService.createDescriptorGroup(
          fileBytes,
          suggestion.getFormat(),
          suggestion.getTitle(),
          suggestion.getDescription(),
          suggestion.getTags(),
          suggestion.getCollectionKey());
      suggestion.setDescriptorGroupKey(descriptorGroupKey);
    } else if (Type.UPDATE.equals(suggestion.getType())) {
      descriptorsService.updateDescriptorGroup(
          suggestion.getDescriptorGroupKey(),
          fileBytes,
          suggestion.getFormat(),
          suggestion.getTitle(),
          suggestion.getTags(),
          suggestion.getDescription());
    } else if (Type.DELETE.equals(suggestion.getType())) {
      descriptorsService.deleteDescriptorGroup(suggestion.getDescriptorGroupKey());
    }

    // Update suggestion status
    suggestion.setStatus(Status.APPLIED);
    suggestion.setModified(new Date());
    suggestion.setAppliedBy(getUsername());
    suggestion.setApplied(new Date());
    suggestionMapper.updateSuggestion(suggestion);

    // Delete the suggestion file
    deleteSuggestionFile(suggestion);

    eventManager.post(
      SubEntityCollectionEvent.newInstance(
        suggestion.getCollectionKey(),
        Collection.class,
        DescriptorChangeSuggestion.class,
        suggestion.getKey(),
        EventType.APPLY_SUGGESTION));

    // send email
    if (Boolean.TRUE.equals(collectionsMailConfigurationProperties.getEnabled())) {
      try {
        Collection collection = collectionService.get(suggestion.getCollectionKey());
        BaseEmailModel emailModel =
            emailManager.generateAppliedDescriptorSuggestionEmailModel(
                suggestion.getKey(),
                suggestion.getCollectionKey(),
                collection.getName(),
                suggestion.getCountry(),
                suggestion.getType(),
                suggestion.getTitle(),
                suggestion.getDescription(),
                suggestion.getFormat() != null ? suggestion.getFormat().name() : "",
                suggestion.getComments(),
                suggestion.getTags(),
                suggestion.getProposerEmail(),
                Collections.singleton(suggestion.getProposerEmail()));
        emailSender.send(emailModel);
      } catch (Exception e) {
        log.error("Couldn't send email for applied descriptor suggestion", e);
      }
    }

    log.info("Applied descriptor suggestion with key: {}", key);
  }

  @Override
  @Transactional
  public void discardSuggestion(long key) {
    DescriptorChangeSuggestion suggestion = getSuggestion(key);
    if (suggestion == null) {
      throw new IllegalArgumentException(
          "Descriptor suggestion was not found");
    }

    if (suggestion.getStatus() != Status.PENDING) {
      throw new IllegalStateException(
          "Descriptor suggestion is not in PENDING status");
    }

    // Update suggestion status
    suggestion.setStatus(Status.DISCARDED);
    suggestion.setModified(new Date());
    suggestion.setDiscarded(new Date());
    suggestion.setDiscardedBy(getUsername());
    suggestion.setModifiedBy(getUsername());
    suggestionMapper.updateSuggestion(suggestion);

    // Delete the suggestion file
    deleteSuggestionFile(suggestion);

    eventManager.post(
      SubEntityCollectionEvent.newInstance(
        suggestion.getCollectionKey(),
        Collection.class,
        DescriptorChangeSuggestion.class,
        suggestion.getKey(),
        EventType.DISCARD_SUGGESTION));

    // send email
    if (Boolean.TRUE.equals(collectionsMailConfigurationProperties.getEnabled())) {
      try {
        Collection collection = collectionService.get(suggestion.getCollectionKey());
        BaseEmailModel emailModel =
            emailManager.generateDiscardedDescriptorSuggestionEmailModel(
                suggestion.getKey(),
                suggestion.getCollectionKey(),
                collection.getName(),
                suggestion.getCountry(),
                suggestion.getType(),
                suggestion.getTitle(),
                suggestion.getDescription(),
                suggestion.getFormat() != null ? suggestion.getFormat().name() : "",
                suggestion.getComments(),
                suggestion.getTags(),
                suggestion.getProposerEmail(),
                Collections.singleton(suggestion.getProposerEmail()));
        emailSender.send(emailModel);
      } catch (Exception e) {
        log.error("Couldn't send email for discarded descriptor suggestion", e);
      }
    }

    log.info("Discarded descriptor suggestion with key: {}", key);
  }

  @Override
  public PagingResponse<DescriptorChangeSuggestion> list(Pageable pageable,
    Status status,
    Type type,
    String proposerEmail,
    UUID collectionKey,
    Country country) {
    Pageable page = pageable == null ? new PagingRequest() : pageable;
    List<DescriptorChangeSuggestion> suggestions = suggestionMapper.list(page, status, type, proposerEmail, collectionKey, country);
    long total = suggestionMapper.count(status, type, proposerEmail, collectionKey, country);
    return new PagingResponse<>(page, total, suggestions);
  }

  @Override
  public long count(Status status, Type type, String proposerEmail, UUID collectionKey, Country country) {
    return suggestionMapper.count(status, type, proposerEmail, collectionKey, country);
  }

  private String generateFilename(String collectionKey, String originalFilename) {
    String extension = getFileExtension(originalFilename);
    String timestamp = String.valueOf(System.currentTimeMillis());
    return collectionKey.substring(0, Math.min(8, collectionKey.length())) + "_" + timestamp + extension;
  }

  private String getFileExtension(String filename) {
    if (filename != null && filename.contains(".")) {
      return filename.substring(filename.lastIndexOf("."));
    }
    return "csv";
  }

  private String getUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication.getName();
  }

  private DescriptorChangeSuggestion buildSuggestion(
    DescriptorChangeSuggestionRequest request) {
    return DescriptorChangeSuggestion.builder()
      .collectionKey(request.getCollectionKey())
      .descriptorGroupKey(request.getDescriptorGroupKey())
      .title(request.getTitle())
      .type(request.getType())
      .description(request.getDescription())
      .format(request.getFormat())
      .comments(request.getComments())
      .tags(request.getTags())
      .proposedBy(getUsername())
      .proposerEmail(request.getProposerEmail())
      .proposed(new Date())
      .status(Status.PENDING).build();
  }

  private Path saveFile(InputStream fileStream, String fileName) throws IOException {
    Path filePath = Paths.get(descriptorSuggestionsStoragePath, fileName);
    Files.createDirectories(filePath.getParent());
    Files.copy(fileStream, filePath, StandardCopyOption.REPLACE_EXISTING);
    log.info("Saved suggestion file to {}", filePath);
    return filePath;
  }

  private void deleteSuggestionFile(DescriptorChangeSuggestion suggestion) {
    if (suggestion.getSuggestedFile() != null) {
      try {
        Path filePath = Paths.get(suggestion.getSuggestedFile());
        if (Files.exists(filePath)) {
          Files.delete(filePath);
          log.info("Deleted suggestion file: {}", filePath);
        }
        // Set suggestedFile to null after successful deletion
        suggestion.setSuggestedFile(null);
        suggestionMapper.updateSuggestion(suggestion);
      } catch (IOException e) {
        log.error("Failed to delete suggestion file for key: {}", suggestion.getKey(), e);
      }
    }
  }

  private Set<String> findRecipientsWithPermissions(UUID collectionKey, Country country) {
    // first we try to find users that has permissions on the collection
    if (collectionKey != null) {
      for (UserRole role : Arrays.asList(UserRole.GRSCICOLL_EDITOR, UserRole.GRSCICOLL_MEDIATOR)) {
        List<GbifUser> users =
            userMapper.search(
                null,
                Collections.singleton(role),
                Collections.singleton(collectionKey),
                null,
                null,
                new PagingRequest());

        if (!users.isEmpty()) {
          return users.stream().map(GbifUser::getEmail).collect(Collectors.toSet());
        }
      }
    }

    if (country != null) {
      for (UserRole role : Arrays.asList(UserRole.GRSCICOLL_EDITOR, UserRole.GRSCICOLL_MEDIATOR)) {
        List<GbifUser> users =
            userMapper.search(
                null,
                Collections.singleton(role),
                null,
                null,
                Collections.singleton(country),
                new PagingRequest());

        if (!users.isEmpty()) {
          return users.stream().map(GbifUser::getEmail).collect(Collectors.toSet());
        }
      }
    }

    return Collections.emptySet();
  }
}
