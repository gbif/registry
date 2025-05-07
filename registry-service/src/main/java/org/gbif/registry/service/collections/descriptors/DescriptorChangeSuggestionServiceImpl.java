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
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.descriptors.DescriptorChangeSuggestion;
import org.gbif.api.model.collections.descriptors.DescriptorChangeSuggestionRequest;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.DescriptorChangeSuggestionService;
import org.gbif.api.service.collections.DescriptorsService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.EventType;
import org.gbif.registry.events.collections.SubEntityCollectionEvent;
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

  @Autowired
  public DescriptorChangeSuggestionServiceImpl(DescriptorChangeSuggestionMapper suggestionMapper,
    DescriptorsService descriptorsService,
    EventManager eventManager) {
    this.suggestionMapper = suggestionMapper;
    this.descriptorsService = descriptorsService;
    this.eventManager = eventManager;
  }

  @Override
  public DescriptorChangeSuggestion createSuggestion(
    InputStream fileStream,
    String fileName,
    DescriptorChangeSuggestionRequest request)
    throws IOException {
    Preconditions.checkArgument(!request.getComments().isEmpty(), "Comment is required");

    DescriptorChangeSuggestion suggestion = buildSuggestion(request);

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

    // Validate the request details (e.g., ensure comments are provided)
    Preconditions.checkArgument(!request.getComments().isEmpty(), "Comment is required");

    // Update the fields of the suggestion
    if (request.getTitle() != null) {
      suggestion.setTitle(request.getTitle());
    }
    if (request.getDescription() != null) {
      suggestion.setDescription(request.getDescription());
    }
    if (request.getComments() != null) {
      suggestion.setComments(request.getComments());
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
          suggestion.getCollectionKey());
      suggestion.setDescriptorGroupKey(descriptorGroupKey);
    } else if (Type.UPDATE.equals(suggestion.getType())) {
      descriptorsService.updateDescriptorGroup(
          suggestion.getDescriptorGroupKey(),
          fileBytes,
          suggestion.getFormat(),
          suggestion.getTitle(),
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

    log.info("Discarded descriptor suggestion with key: {}", key);
  }

  @Override
  public PagingResponse<DescriptorChangeSuggestion> list(Pageable pageable,
    Status status,
    Type type,
    String proposerEmail,
    UUID collectionKey) {
    Pageable page = pageable == null ? new PagingRequest() : pageable;
    List<DescriptorChangeSuggestion> suggestions = suggestionMapper.list(page, status, type, proposerEmail, collectionKey);
    long total = suggestionMapper.count(status, type, proposerEmail, collectionKey);
    return new PagingResponse<>(page, total, suggestions);
  }

  @Override
  public long count(Status status, Type type, String proposerEmail, UUID collectionKey) {
    return suggestionMapper.count(status, type, proposerEmail, collectionKey);
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
}
