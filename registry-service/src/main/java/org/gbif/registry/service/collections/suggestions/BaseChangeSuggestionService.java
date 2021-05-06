package org.gbif.registry.service.collections.suggestions;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.EntityType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.OccurrenceMappeable;
import org.gbif.api.model.collections.suggestions.Change;
import org.gbif.api.model.collections.suggestions.ChangeSuggestion;
import org.gbif.api.model.collections.suggestions.ChangeSuggestionService;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.CrudService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.persistence.mapper.collections.ChangeSuggestionMapper;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeDto;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeSuggestionDto;
import org.gbif.registry.service.collections.merge.MergeService;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;

public abstract class BaseChangeSuggestionService<
        T extends
            CollectionEntity & Taggable & Identifiable & MachineTaggable & Commentable & Contactable
                & OccurrenceMappeable,
        R extends ChangeSuggestion<T>>
    implements ChangeSuggestionService<T, R> {

  private static final Logger LOG = LoggerFactory.getLogger(BaseChangeSuggestionService.class);

  private static final Set<String> FIELDS_TO_IGNORE =
      new HashSet<>(
          Arrays.asList(
              "tags",
              "identifiers",
              "contacts",
              "machineTags",
              "comments",
              "occurrenceMappings",
              "replacedBy",
              "createdBy",
              "modifiedBy",
              "created",
              "modified",
              "key",
              "convertedToCollection"));

  private final ChangeSuggestionMapper changeSuggestionMapper;
  private final MergeService<T> mergeService;
  private final CrudService<T> crudService;
  private final Class<T> clazz;
  private final ObjectMapper objectMapper;
  private EntityType entityType;

  protected BaseChangeSuggestionService(
      ChangeSuggestionMapper changeSuggestionMapper,
      MergeService<T> mergeService,
      CrudService<T> crudService,
      Class<T> clazz,
      ObjectMapper objectMapper) {
    this.changeSuggestionMapper = changeSuggestionMapper;
    this.mergeService = mergeService;
    this.crudService = crudService;
    this.clazz = clazz;
    this.objectMapper = objectMapper;

    if (clazz == Institution.class) {
      entityType = EntityType.INSTITUTION;
    } else if (clazz == Collection.class) {
      entityType = EntityType.COLLECTION;
    }
  }

  @Override
  public int createChangeSuggestion(R changeSuggestion) {
    checkArgument(!changeSuggestion.getComments().isEmpty(), "A comment is required");

    if (changeSuggestion.getType() == Type.CREATE) {
      return createNewEntitySuggestion(changeSuggestion);
    }
    if (changeSuggestion.getType() == Type.UPDATE) {
      return createUpdateSuggestion(changeSuggestion);
    }
    if (changeSuggestion.getType() == Type.DELETE) {
      return createDeleteSuggestion(changeSuggestion);
    }
    if (changeSuggestion.getType() == Type.MERGE) {
      return createMergeSuggestion(changeSuggestion);
    }
    if (changeSuggestion.getType() == Type.CONVERSION_TO_COLLECTION) {
      return createConvertToCollectionSuggestion(changeSuggestion);
    }

    throw new IllegalArgumentException("Invalid suggestion type: " + changeSuggestion.getType());
  }

  protected int createUpdateSuggestion(R changeSuggestion) {
    checkArgument(changeSuggestion.getEntityKey() != null);
    checkArgument(changeSuggestion.getSuggestedEntity() != null);

    ChangeSuggestionDto dto = createBaseChangeSuggestionDto(changeSuggestion);
    dto.setCountry(getCountry(changeSuggestion.getSuggestedEntity()));
    dto.setSuggestedEntity(toJson(changeSuggestion.getSuggestedEntity()));

    T currentEntity = crudService.get(changeSuggestion.getEntityKey());
    dto.setChanges(extractChanges(changeSuggestion.getSuggestedEntity(), currentEntity));

    changeSuggestionMapper.create(dto);

    return dto.getKey();
  }

  protected int createNewEntitySuggestion(R changeSuggestion) {
    checkArgument(changeSuggestion.getSuggestedEntity() != null);

    ChangeSuggestionDto dto = createBaseChangeSuggestionDto(changeSuggestion);
    dto.setCountry(getCountry(changeSuggestion.getSuggestedEntity()));
    dto.setSuggestedEntity(toJson(changeSuggestion.getSuggestedEntity()));

    changeSuggestionMapper.create(dto);
    return dto.getKey();
  }

  protected int createDeleteSuggestion(R changeSuggestion) {
    checkArgument(changeSuggestion.getEntityKey() != null);

    ChangeSuggestionDto dto = createBaseChangeSuggestionDto(changeSuggestion);

    T currentEntity = crudService.get(changeSuggestion.getEntityKey());
    dto.setCountry(getCountry(currentEntity));

    changeSuggestionMapper.create(dto);
    return dto.getKey();
  }

  protected int createMergeSuggestion(R changeSuggestion) {
    checkArgument(changeSuggestion.getEntityKey() != null);
    checkArgument(changeSuggestion.getMergeTargetKey() != null);

    ChangeSuggestionDto dto = createBaseChangeSuggestionDto(changeSuggestion);
    dto.setMergeTargetKey(changeSuggestion.getMergeTargetKey());

    T currentEntity = crudService.get(changeSuggestion.getEntityKey());
    dto.setCountry(getCountry(currentEntity));

    changeSuggestionMapper.create(dto);
    return dto.getKey();
  }

  // TODO: suggestions roles
  @Secured({GRSCICOLL_ADMIN_ROLE})
  @Override
  public void updateChangeSuggestion(R updatedChangeSuggestion) {
    ChangeSuggestionDto dto = changeSuggestionMapper.get(updatedChangeSuggestion.getKey());

    checkArgument(
        updatedChangeSuggestion.getComments().size() > dto.getComments().size(),
        "A comment is required");

    if (dto.getType() == Type.CREATE || dto.getType() == Type.UPDATE) {
      // we do this to update the suggested entity with the current state and minimize the risk of
      // having race conditions
      R changeSuggestion = dtoToChangeSuggestion(dto);

      Set<ChangeDto> newChanges =
          extractChanges(
              updatedChangeSuggestion.getSuggestedEntity(), changeSuggestion.getSuggestedEntity());
      dto.getChanges().addAll(newChanges);
      dto.setSuggestedEntity(toJson(updatedChangeSuggestion.getSuggestedEntity()));
    }

    dto.setComments(updatedChangeSuggestion.getComments());
    dto.setModifiedBy(getUsername());
    changeSuggestionMapper.update(dto);
  }

  // TODO: suggestions roles
  @Secured({GRSCICOLL_ADMIN_ROLE})
  @Override
  public void discardChangeSuggestion(int key) {
    ChangeSuggestionDto dto = changeSuggestionMapper.get(key);
    dto.setStatus(Status.DISCARDED);
    dto.setDiscarded(new Date());
    dto.setDiscardedBy(getUsername());
    dto.setModifiedBy(getUsername());
    changeSuggestionMapper.update(dto);
  }

  // TODO: suggestions roles
  @Secured({GRSCICOLL_ADMIN_ROLE})
  @Override
  public UUID applyChangeSuggestion(int suggestionKey) {
    ChangeSuggestionDto dto = changeSuggestionMapper.get(suggestionKey);
    R changeSuggestion = dtoToChangeSuggestion(dto);
    UUID createdEntity = null;
    if (dto.getType() == Type.CREATE) {
      createdEntity = crudService.create(changeSuggestion.getSuggestedEntity());
      dto.setEntityKey(createdEntity);
    } else if (dto.getType() == Type.UPDATE) {
      crudService.update(changeSuggestion.getSuggestedEntity());
    } else if (dto.getType() == Type.DELETE) {
      crudService.delete(changeSuggestion.getEntityKey());
    } else if (dto.getType() == Type.MERGE) {
      mergeService.merge(changeSuggestion.getEntityKey(), changeSuggestion.getMergeTargetKey());
    } else if (dto.getType() == Type.CONVERSION_TO_COLLECTION) {
      createdEntity = applyConversionToCollection(dto);
    }

    dto.setStatus(Status.APPLIED);
    dto.setModifiedBy(getUsername());
    dto.setApplied(new Date());
    dto.setAppliedBy(getUsername());
    changeSuggestionMapper.update(dto);

    return createdEntity;
  }

  @Override
  public PagingResponse<R> list(
      @Nullable Status status,
      @Nullable Type type,
      @Nullable Country country,
      @Nullable String proposedBy,
      @Nullable UUID entityKey,
      @Nullable Pageable pageable) {
    Pageable page = pageable == null ? new PagingRequest() : pageable;

    List<ChangeSuggestionDto> dtos =
        changeSuggestionMapper.list(
            status,
            type,
            entityType,
            country,
            newEmptyChangeSuggestion().getProposerEmail(),
            entityKey,
            page);

    long count =
        changeSuggestionMapper.count(
            status,
            type,
            entityType,
            country,
            newEmptyChangeSuggestion().getProposerEmail(),
            entityKey);

    List<R> changeSuggestions =
        dtos.stream().map(this::dtoToChangeSuggestion).collect(Collectors.toList());

    return new PagingResponse<>(page, count, changeSuggestions);
  }

  private Set<ChangeDto> extractChanges(T suggestedEntity, T currentEntity) {
    Set<ChangeDto> changes = new HashSet<>();
    for (Field field : clazz.getDeclaredFields()) {
      if (FIELDS_TO_IGNORE.contains(field.getName()) || field.isSynthetic()) {
        continue;
      }
      try {
        String methodPrefix = field.getType().isAssignableFrom(Boolean.TYPE) ? "is" : "get";
        Object suggestedValue =
            clazz
                .getMethod(
                    methodPrefix
                        + field.getName().substring(0, 1).toUpperCase()
                        + field.getName().substring(1))
                .invoke(suggestedEntity);

        Object previousValue =
            clazz
                .getMethod(
                    methodPrefix
                        + field.getName().substring(0, 1).toUpperCase()
                        + field.getName().substring(1))
                .invoke(currentEntity);

        if (!Objects.equals(suggestedValue, previousValue)) {
          ChangeDto changeDto = new ChangeDto();
          changeDto.setFieldName(field.getName());
          changeDto.setFieldType(field.getType());

          if (field.getGenericType() instanceof ParameterizedType) {
            changeDto.setFieldGenericTypeName(
                ((ParameterizedType) field.getGenericType())
                    .getActualTypeArguments()[0].getTypeName());
          }

          changeDto.setPrevious(previousValue);
          changeDto.setSuggested(suggestedValue);
          changeDto.setAuthor(getUsername());
          changeDto.setCreated(new Date());
          changes.add(changeDto);
        }
      } catch (Exception e) {
        throw new IllegalStateException("Error while comparing field values", e);
      }
    }
    return changes;
  }

  protected ChangeSuggestionDto createBaseChangeSuggestionDto(R changeSuggestion) {
    ChangeSuggestionDto dto = new ChangeSuggestionDto();
    dto.setEntityKey(changeSuggestion.getEntityKey());
    dto.setStatus(Status.PENDING);
    dto.setType(changeSuggestion.getType());
    dto.setComments(changeSuggestion.getComments());
    dto.setEntityType(entityType);
    dto.setProposedBy(changeSuggestion.getProposerEmail());
    dto.setModifiedBy(getUsername());
    return dto;
  }

  protected Country getCountry(T entity) {
    Address physicalAddress = null;
    Address mailingAddress = null;
    if (entity instanceof Institution) {
      Institution institution = (Institution) entity;
      physicalAddress = institution.getAddress();
      mailingAddress = institution.getMailingAddress();
    } else if (entity instanceof Collection) {
      Collection collection = (Collection) entity;
      physicalAddress = collection.getAddress();
      mailingAddress = collection.getMailingAddress();
    }

    if (physicalAddress != null && physicalAddress.getCountry() != null) {
      return physicalAddress.getCountry();
    } else if (mailingAddress != null) {
      return mailingAddress.getCountry();
    } else {
      return null;
    }
  }

  protected R dtoToChangeSuggestion(ChangeSuggestionDto dto) {
    R suggestion = newEmptyChangeSuggestion();
    suggestion.setKey(dto.getKey());
    suggestion.setStatus(dto.getStatus());
    suggestion.setType(dto.getType());
    suggestion.setCountry(dto.getCountry());
    suggestion.setAppliedBy(dto.getAppliedBy());
    suggestion.setApplied(dto.getApplied());
    suggestion.setDiscarded(dto.getDiscarded());
    suggestion.setDiscardedBy(dto.getDiscardedBy());
    suggestion.setEntityKey(dto.getEntityKey());
    suggestion.setComments(dto.getComments());
    suggestion.setModified(dto.getModified());
    suggestion.setModifiedBy(dto.getModifiedBy());
    suggestion.setProposed(dto.getProposed());
    suggestion.setProposerEmail(dto.getProposedBy());
    suggestion.setMergeTargetKey(dto.getMergeTargetKey());

    // changes conversion
    suggestion.setChanges(
        dto.getChanges().stream()
            .map(
                ch -> {
                  Change change = new Change();
                  change.setField(ch.getFieldName());
                  change.setPrevious(ch.getPrevious());
                  change.setSuggested(ch.getSuggested());
                  change.setAuthor(ch.getAuthor());
                  change.setCreated(ch.getCreated());
                  return change;
                })
            .collect(Collectors.toList()));

    // merge view
    try {
      if (dto.getType() == Type.CREATE) {
        suggestion.setSuggestedEntity(objectMapper.readValue(dto.getSuggestedEntity(), clazz));
      } else if (dto.getType() == Type.UPDATE) {
        T entity = crudService.get(dto.getEntityKey());

        // we sort the changes because we can have multiple changes in the same field. We are only
        // interested in the last change so we want to apply them in order (older changes could be
        // discarded but it doesn't matter much since we're not expecting too many changes in the
        // same field - the original and another one if the reviewer wants to change it)
        List<ChangeDto> changesSorted =
            dto.getChanges().stream()
                .sorted(Comparator.comparing(ChangeDto::getCreated))
                .collect(Collectors.toList());
        for (ChangeDto changeDto : changesSorted) {
          clazz
              .getMethod(
                  "set"
                      + changeDto.getFieldName().substring(0, 1).toUpperCase()
                      + changeDto.getFieldName().substring(1),
                  changeDto.getFieldType())
              .invoke(entity, changeDto.getSuggested());
        }
        suggestion.setSuggestedEntity(entity);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Error while applying suggested change to the merge view", e);
    }

    return suggestion;
  }

  protected String getUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication.getName();
  }

  protected String toJson(T entity) {
    try {
      return objectMapper.writeValueAsString(entity);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Cannot serialize entity", e);
    }
  }

  protected abstract R newEmptyChangeSuggestion();

  protected abstract int createConvertToCollectionSuggestion(R changeSuggestion);

  protected abstract UUID applyConversionToCollection(ChangeSuggestionDto dto);
}
