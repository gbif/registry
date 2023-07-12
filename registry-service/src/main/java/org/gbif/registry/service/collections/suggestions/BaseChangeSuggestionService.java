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
package org.gbif.registry.service.collections.suggestions;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.OccurrenceMappeable;
import org.gbif.api.model.collections.suggestions.Change;
import org.gbif.api.model.collections.suggestions.ChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.ChangeSuggestionService;
import org.gbif.api.service.collections.ContactService;
import org.gbif.api.service.collections.CrudService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.EventType;
import org.gbif.registry.events.collections.SubEntityCollectionEvent;
import org.gbif.registry.mail.BaseEmailModel;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.collections.CollectionsEmailManager;
import org.gbif.registry.mail.config.CollectionsMailConfigurationProperties;
import org.gbif.registry.persistence.mapper.UserMapper;
import org.gbif.registry.persistence.mapper.collections.ChangeSuggestionMapper;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeDto;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeSuggestionDto;
import org.gbif.registry.security.grscicoll.GrSciCollAuthorizationService;
import org.gbif.registry.service.collections.merge.MergeService;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.elasticsearch.common.Strings;
import org.jetbrains.annotations.NotNull;
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
import static org.gbif.registry.security.UserRoles.GRSCICOLL_EDITOR_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_MEDIATOR_ROLE;
import static org.gbif.registry.service.collections.utils.MasterSourceUtils.hasExternalMasterSource;

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
              "deleted",
              "key",
              "convertedToCollection",
              "masterSource",
              "occurrenceCount",
              "typeSpecimenCount"));

  private static final String CONTACTS_FIELD_NAME = "contactPersons";

  private final ChangeSuggestionMapper changeSuggestionMapper;
  private final MergeService<T> mergeService;
  private final CrudService<T> crudService;
  private final ContactService contactService;
  private final UserMapper userMapper;
  private final Class<T> clazz;
  private final ObjectMapper objectMapper;
  private final EmailSender emailSender;
  private final CollectionsEmailManager emailManager;
  private final EventManager eventManager;
  private final GrSciCollAuthorizationService grSciCollAuthorizationService;
  private final CollectionsMailConfigurationProperties collectionsMailConfigurationProperties;
  private CollectionEntityType collectionEntityType;

  protected BaseChangeSuggestionService(
      ChangeSuggestionMapper changeSuggestionMapper,
      MergeService<T> mergeService,
      CrudService<T> crudService,
      ContactService contactService,
      UserMapper userMapper,
      Class<T> clazz,
      ObjectMapper objectMapper,
      EmailSender emailSender,
      CollectionsEmailManager emailManager,
      EventManager eventManager,
      GrSciCollAuthorizationService grSciCollAuthorizationService,
      CollectionsMailConfigurationProperties collectionsMailConfigurationProperties) {
    this.changeSuggestionMapper = changeSuggestionMapper;
    this.mergeService = mergeService;
    this.crudService = crudService;
    this.contactService = contactService;
    this.userMapper = userMapper;
    this.clazz = clazz;
    this.objectMapper = objectMapper;
    this.emailSender = emailSender;
    this.emailManager = emailManager;
    this.eventManager = eventManager;
    this.grSciCollAuthorizationService = grSciCollAuthorizationService;
    this.collectionsMailConfigurationProperties = collectionsMailConfigurationProperties;

    if (clazz == Institution.class) {
      collectionEntityType = CollectionEntityType.INSTITUTION;
    } else if (clazz == Collection.class) {
      collectionEntityType = CollectionEntityType.COLLECTION;
    }
  }

  @Override
  public int createChangeSuggestion(R changeSuggestion) {
    checkArgument(!changeSuggestion.getComments().isEmpty(), "A comment is required");

    ChangeSuggestionDto dto = null;
    if (changeSuggestion.getType() == Type.CREATE) {
      dto = createNewEntitySuggestionDto(changeSuggestion);
    } else if (changeSuggestion.getType() == Type.UPDATE) {
      dto = createUpdateSuggestionDto(changeSuggestion);
    } else if (changeSuggestion.getType() == Type.DELETE) {
      dto = createDeleteSuggestionDto(changeSuggestion);
    } else if (changeSuggestion.getType() == Type.MERGE) {
      dto = createMergeSuggestionDto(changeSuggestion);
    } else if (changeSuggestion.getType() == Type.CONVERSION_TO_COLLECTION) {
      dto = createConvertToCollectionSuggestionDto(changeSuggestion);
    } else {
      throw new IllegalArgumentException("Invalid suggestion type: " + changeSuggestion.getType());
    }

    changeSuggestionMapper.create(dto);

    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            dto.getEntityKey(), clazz, dto, dto.getKey(), EventType.CREATE));

    // send email
    if (Boolean.TRUE.equals(collectionsMailConfigurationProperties.getEnabled())) {
      try {
        T suggestedEntity = readJson(dto.getSuggestedEntity(), clazz);

        BaseEmailModel emailModel =
            emailManager.generateNewChangeSuggestionEmailModel(
                dto.getKey(),
                dto.getEntityType(),
                suggestedEntity.getName(),
                dto.getCountryScope(),
                dto.getEntityKey(),
                dto.getType(),
                findRecipientsWithPermissions(dto.getEntityKey(), dto.getCountryScope()));
        emailSender.send(emailModel);
      } catch (Exception e) {
        LOG.error("Couldn't send email for new change suggestion", e);
      }
    }

    return dto.getKey();
  }

  private Set<String> findRecipientsWithPermissions(UUID entityKey, Country country) {

    // first we try to find users that has permissions on the entity
    if (entityKey != null) {
      for (UserRole role : Arrays.asList(UserRole.GRSCICOLL_EDITOR, UserRole.GRSCICOLL_MEDIATOR)) {
        List<GbifUser> users =
            userMapper.search(
                null,
                Collections.singleton(role),
                Collections.singleton(entityKey),
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

  protected ChangeSuggestionDto createUpdateSuggestionDto(R changeSuggestion) {
    checkArgument(changeSuggestion.getEntityKey() != null);
    checkArgument(changeSuggestion.getSuggestedEntity() != null);

    ChangeSuggestionDto dto = createBaseChangeSuggestionDto(changeSuggestion);
    dto.setSuggestedEntity(toJson(changeSuggestion.getSuggestedEntity()));

    T currentEntity = crudService.get(changeSuggestion.getEntityKey());
    dto.setChanges(extractChanges(changeSuggestion.getSuggestedEntity(), currentEntity));
    dto.setCountryScope(getCountry(currentEntity));

    return dto;
  }

  protected ChangeSuggestionDto createNewEntitySuggestionDto(R changeSuggestion) {
    checkArgument(changeSuggestion.getSuggestedEntity() != null);

    ChangeSuggestionDto dto = createBaseChangeSuggestionDto(changeSuggestion);
    dto.setSuggestedEntity(toJson(changeSuggestion.getSuggestedEntity()));
    dto.setChanges(
        extractChanges(changeSuggestion.getSuggestedEntity(), createEmptyEntityInstance()));
    dto.setCountryScope(getCountry(changeSuggestion.getSuggestedEntity()));

    return dto;
  }

  protected ChangeSuggestionDto createDeleteSuggestionDto(R changeSuggestion) {
    checkArgument(changeSuggestion.getEntityKey() != null);

    // if the entity has an external master source we don't allow delete suggestions
    T currentEntity = crudService.get(changeSuggestion.getEntityKey());
    if (hasExternalMasterSource(currentEntity)) {
      throw new IllegalArgumentException(
          "Suggestions to delete entities whose master source is not GRSciColl are not allowed");
    }

    ChangeSuggestionDto dto = createBaseChangeSuggestionDto(changeSuggestion);
    dto.setCountryScope(getCountry(currentEntity));

    return dto;
  }

  protected ChangeSuggestionDto createMergeSuggestionDto(R changeSuggestion) {
    checkArgument(changeSuggestion.getEntityKey() != null);
    checkArgument(changeSuggestion.getMergeTargetKey() != null);

    ChangeSuggestionDto dto = createBaseChangeSuggestionDto(changeSuggestion);
    dto.setMergeTargetKey(changeSuggestion.getMergeTargetKey());

    T currentEntity = crudService.get(changeSuggestion.getEntityKey());
    dto.setCountryScope(getCountry(currentEntity));

    return dto;
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Override
  public void updateChangeSuggestion(R updatedChangeSuggestion) {
    ChangeSuggestionDto dto = changeSuggestionMapper.get(updatedChangeSuggestion.getKey());

    checkArgument(
        updatedChangeSuggestion.getComments().size() > dto.getComments().size(),
        "A comment is required");

    if (dto.getType() == Type.CREATE || dto.getType() == Type.UPDATE) {
      // we get the current entity from the DB to update the suggested entity with the current state
      // and minimize the risk of having race conditions
      R changeSuggestion = dtoToChangeSuggestion(dto);

      Set<ChangeDto> newChanges =
          extractChanges(
              updatedChangeSuggestion.getSuggestedEntity(), changeSuggestion.getSuggestedEntity());

      for (ChangeDto newChange : newChanges) {
        // update the overwritten flag
        dto.getChanges().stream()
            .filter(c -> c.getFieldName().equals(newChange.getFieldName()))
            .forEach(c -> c.setOverwritten(true));
        dto.getChanges().add(newChange);
      }

      dto.setSuggestedEntity(toJson(updatedChangeSuggestion.getSuggestedEntity()));
    }

    dto.setComments(updatedChangeSuggestion.getComments());
    dto.setModifiedBy(getUsername());

    // keep a copy of the old dto for the audit log
    ChangeSuggestionDto oldDto = changeSuggestionMapper.get(updatedChangeSuggestion.getKey());
    changeSuggestionMapper.update(dto);

    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            dto.getEntityKey(), clazz, dto, oldDto, dto.getKey(), EventType.UPDATE));
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Override
  public void discardChangeSuggestion(int key) {
    ChangeSuggestionDto dto = changeSuggestionMapper.get(key);
    dto.setStatus(Status.DISCARDED);
    dto.setDiscarded(new Date());
    dto.setDiscardedBy(getUsername());
    dto.setModifiedBy(getUsername());

    // keep a copy of the old dto for the audit log
    ChangeSuggestionDto oldDto = changeSuggestionMapper.get(key);
    changeSuggestionMapper.update(dto);

    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            dto.getEntityKey(), clazz, dto, oldDto, dto.getKey(), EventType.DISCARD_SUGGESTION));

    // send email
    if (Boolean.TRUE.equals(collectionsMailConfigurationProperties.getEnabled())) {
      try {
        R changeSuggestion = dtoToChangeSuggestion(dto);
        BaseEmailModel emailModel =
            emailManager.generateDiscardedChangeSuggestionEmailModel(
                dto.getKey(),
                dto.getEntityType(),
                changeSuggestion.getEntityName(),
                changeSuggestion.getEntityCountry(),
                dto.getEntityKey(),
                dto.getType(),
                Collections.singleton(dto.getProposerEmail()));
        emailSender.send(emailModel);
      } catch (Exception e) {
        LOG.error("Couldn't send email for discarded change suggestion", e);
      }
    }
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Override
  public UUID applyChangeSuggestion(int suggestionKey) {
    ChangeSuggestionDto dto = changeSuggestionMapper.get(suggestionKey);
    R changeSuggestion = dtoToChangeSuggestion(dto);
    UUID createdEntity = null;
    if (dto.getType() == Type.CREATE) {
      createdEntity = crudService.create(changeSuggestion.getSuggestedEntity());

      // create contacts
      createContacts(changeSuggestion, createdEntity);

      dto.setEntityKey(createdEntity);
    } else if (dto.getType() == Type.UPDATE) {
      T originalEntity = crudService.get(changeSuggestion.getEntityKey());

      // update contacts
      updateContacts(changeSuggestion, originalEntity);

      // update the entity
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

    // keep a copy of the old dto for the audit log
    ChangeSuggestionDto oldDto = changeSuggestionMapper.get(suggestionKey);
    changeSuggestionMapper.update(dto);

    eventManager.post(
        SubEntityCollectionEvent.newInstance(
            dto.getEntityKey(), clazz, dto, oldDto, dto.getKey(), EventType.APPLY_SUGGESTION));

    // send email
    if (Boolean.TRUE.equals(collectionsMailConfigurationProperties.getEnabled())) {
      try {
        BaseEmailModel emailModel =
            emailManager.generateAppliedChangeSuggestionEmailModel(
                dto.getKey(),
                dto.getEntityType(),
                changeSuggestion.getEntityName(),
                changeSuggestion.getEntityCountry(),
                dto.getEntityKey(),
                dto.getType(),
                Collections.singleton(dto.getProposerEmail()));
        emailSender.send(emailModel);
      } catch (Exception e) {
        LOG.error("Couldn't send email for applied change suggestion", e);
      }
    }

    return createdEntity;
  }

  private void updateContacts(R changeSuggestion, T originalEntity) {
    if (changeSuggestion.getSuggestedEntity().getContactPersons() == null) {
      return;
    }

    if (hasExternalMasterSource(originalEntity)) {
      // cannot modify the contacts of an entity whose master source is not GRSciColl
      changeSuggestion.getSuggestedEntity().setContactPersons(originalEntity.getContactPersons());
      return;
    }

    List<Contact> suggestedContacts = changeSuggestion.getSuggestedEntity().getContactPersons();
    if (!suggestedContacts.isEmpty()) {
      suggestedContacts.forEach(
          c -> {
            if (c.getKey() == null) {
              // create new contact
              contactService.addContactPerson(originalEntity.getKey(), c);
            } else if (originalEntity.getContactPersons().stream()
                .anyMatch(oc -> oc.getKey().equals(c.getKey()))) {
              // update current contact
              contactService.updateContactPerson(originalEntity.getKey(), c);
            }
          });
    }

    // remove the contacts that are not present in the suggestion
    originalEntity.getContactPersons().stream()
        .filter(c -> suggestedContacts.stream().noneMatch(cp -> cp.getKey().equals(c.getKey())))
        .forEach(c -> contactService.removeContactPerson(originalEntity.getKey(), c.getKey()));
  }

  private void createContacts(R changeSuggestion, UUID createdEntity) {
    if (changeSuggestion.getSuggestedEntity().getContactPersons() != null
        && !changeSuggestion.getSuggestedEntity().getContactPersons().isEmpty()) {
      changeSuggestion
          .getSuggestedEntity()
          .getContactPersons()
          .forEach(c -> contactService.addContactPerson(createdEntity, c));
    }
  }

  @Override
  public PagingResponse<R> list(
      @Nullable Status status,
      @Nullable Type type,
      @Nullable String proposerEmail,
      @Nullable UUID entityKey,
      @Nullable Pageable pageable) {
    Pageable page = pageable == null ? new PagingRequest() : pageable;

    List<ChangeSuggestionDto> dtos =
        changeSuggestionMapper.list(
            status, type, collectionEntityType, proposerEmail, entityKey, page);

    long count =
        changeSuggestionMapper.count(status, type, collectionEntityType, proposerEmail, entityKey);

    List<R> changeSuggestions =
        dtos.stream().map(this::dtoToChangeSuggestion).collect(Collectors.toList());

    return new PagingResponse<>(page, count, changeSuggestions);
  }

  private Set<ChangeDto> extractChanges(T suggestedEntity, T currentEntity) {
    checkArgument(suggestedEntity != null, "Suggested entity is required");
    checkArgument(currentEntity != null, "Current entity is required");

    Set<ChangeDto> changes = new HashSet<>();
    for (Field field : clazz.getDeclaredFields()) {
      if (FIELDS_TO_IGNORE.contains(field.getName()) || field.isSynthetic()) {
        continue;
      }

      if (field.getName().equals(CONTACTS_FIELD_NAME)) {
        Map<Integer, Contact> currentContactsMap =
            currentEntity.getContactPersons().stream()
                .collect(Collectors.toMap(Contact::getKey, c -> c));

        suggestedEntity
            .getContactPersons()
            .forEach(
                sugg -> {
                  Contact current = currentContactsMap.get(sugg.getKey());
                  if (!Objects.equals(current, sugg)) {
                    // contact has changed
                    changes.add(createChangeDto(field, sugg, current, Contact.class));
                  }
                  // remove from map
                  if (current != null) {
                    currentContactsMap.remove(current.getKey());
                  }
                });

        // contacts deleted
        if (!currentContactsMap.isEmpty()) {
          currentContactsMap
              .values()
              .forEach(c -> changes.add(createChangeDto(field, null, c, Contact.class)));
        }
      } else {
        try {
          String methodPrefix = getGetterPrefix(field.getType());
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

          if (isDifferentValue(suggestedValue, previousValue)) {
            changes.add(createChangeDto(field, suggestedValue, previousValue, field.getType()));
          }
        } catch (Exception e) {
          throw new IllegalStateException("Error while comparing field values", e);
        }
      }
    }
    return changes;
  }

  private static boolean isDifferentValue(Object suggestedValue, Object previousValue) {
    if (suggestedValue instanceof BigDecimal && previousValue instanceof BigDecimal) {
      return ((BigDecimal) suggestedValue).compareTo((BigDecimal) previousValue) != 0;
    }
    return !Objects.equals(suggestedValue, previousValue);
  }

  @NotNull
  private ChangeDto createChangeDto(
      Field field, Object suggested, Object previous, Class<?> fieldType) {
    ChangeDto changeDto = new ChangeDto();
    changeDto.setSuggested(suggested);
    changeDto.setPrevious(previous);
    changeDto.setFieldName(field.getName());
    changeDto.setFieldType(fieldType);
    changeDto.setCreated(new Date());

    String username = getUsername();
    if (!Strings.isNullOrEmpty(username)) {
      changeDto.setAuthor(username);
    }

    if (field.getGenericType() instanceof ParameterizedType) {
      changeDto.setFieldGenericTypeName(
          ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0].getTypeName());
    }

    return changeDto;
  }

  protected ChangeSuggestionDto createBaseChangeSuggestionDto(R changeSuggestion) {
    ChangeSuggestionDto dto = new ChangeSuggestionDto();
    dto.setEntityKey(changeSuggestion.getEntityKey());
    dto.setStatus(Status.PENDING);
    dto.setType(changeSuggestion.getType());
    dto.setComments(changeSuggestion.getComments());
    dto.setEntityType(collectionEntityType);
    dto.setProposerEmail(changeSuggestion.getProposerEmail());
    dto.setProposedBy(getUsername());
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
    suggestion.setAppliedBy(dto.getAppliedBy());
    suggestion.setApplied(dto.getApplied());
    suggestion.setDiscarded(dto.getDiscarded());
    suggestion.setDiscardedBy(dto.getDiscardedBy());
    suggestion.setEntityKey(dto.getEntityKey());
    suggestion.setComments(dto.getComments());
    suggestion.setModified(dto.getModified());
    suggestion.setModifiedBy(dto.getModifiedBy());
    suggestion.setProposed(dto.getProposed());
    suggestion.setProposedBy(dto.getProposedBy());
    suggestion.setMergeTargetKey(dto.getMergeTargetKey());

    // we only show the proposer email for users with the right permissions (data protection)
    if (hasRightsToSeeProposerEmail(dto)) {
      suggestion.setProposerEmail(dto.getProposerEmail());
    }

    if (dto.getEntityKey() != null) {
      // we take the country and the name from the current entity
      T currentEntity = crudService.get(dto.getEntityKey());
      suggestion.setEntityCountry(getCountry(currentEntity));
      suggestion.setEntityName(currentEntity.getName());
    }

    // merge view
    if (dto.getType() == Type.UPDATE
        && (dto.getStatus() != Status.DISCARDED && dto.getStatus() != Status.APPLIED)) {
      try {
        T currentEntity = crudService.get(dto.getEntityKey());
        List<Change> changes = new ArrayList<>();
        suggestion.setChanges(changes);
        for (ChangeDto changeDto : dto.getChanges()) {
          // set suggested contacts in the current entity
          if (changeDto.getFieldName().equals(CONTACTS_FIELD_NAME)
              && isContactPersonIndividualChange(changeDto)) {
            if (changeDto.getSuggested() == null) {
              // deleted contact
              int contactKey = ((Contact) changeDto.getPrevious()).getKey();
              currentEntity
                  .getContactPersons()
                  .removeIf(c -> Objects.equals(c.getKey(), contactKey));
            } else if (changeDto.getPrevious() == null) {
              // new contact
              currentEntity.getContactPersons().add((Contact) changeDto.getSuggested());
            } else {
              // update contact
              Integer contactKey = ((Contact) changeDto.getSuggested()).getKey();

              if (contactKey != null) { // it can be null if it's an update of a previous suggestion
                currentEntity
                    .getContactPersons()
                    .removeIf(c -> Objects.equals(c.getKey(), contactKey));
              }
              currentEntity.getContactPersons().add((Contact) changeDto.getSuggested());
            }

            changes.add(changeDtoToChange(changeDto));
          } else {
            // set changes in the current entity
            Change change = changeDtoToChange(changeDto);
            Object currentValue =
                clazz
                    .getMethod(
                        getGetterPrefix(changeDto.getFieldType())
                            + changeDto.getFieldName().substring(0, 1).toUpperCase()
                            + changeDto.getFieldName().substring(1))
                    .invoke(currentEntity);

            // if it's the same as the current it's considered outdated
            change.setOutdated(Objects.equals(currentValue, changeDto.getSuggested()));
            changes.add(change);

            if (!changeDto.isOverwritten()) {
              clazz
                  .getMethod(
                      "set"
                          + changeDto.getFieldName().substring(0, 1).toUpperCase()
                          + changeDto.getFieldName().substring(1),
                      changeDto.getFieldType())
                  .invoke(currentEntity, changeDto.getSuggested());
            }
          }
        }

        suggestion.setSuggestedEntity(currentEntity);
      } catch (Exception e) {
        throw new IllegalStateException(
            "Error while applying suggested change to the merge view", e);
      }
    } else {
      // if it's not an active update we don't do the merge and we don't calculate the stillRelevant
      // field for the changes
      suggestion.setChanges(
          dto.getChanges().stream().map(this::changeDtoToChange).collect(Collectors.toList()));

      if (dto.getSuggestedEntity() != null) {
        T suggestedEntity = readJson(dto.getSuggestedEntity(), clazz);
        suggestion.setSuggestedEntity(suggestedEntity);

        if (suggestion.getEntityName() == null) {
          suggestion.setEntityName(suggestedEntity.getName());
        }
        if (suggestion.getEntityCountry() == null) {
          suggestion.setEntityCountry(getCountry(suggestedEntity));
        }
      }
    }

    return suggestion;
  }

  /**
   * The way of handling changes in contacts changed and we need to differentiate between the old
   * and the new way.
   */
  private boolean isContactPersonIndividualChange(ChangeDto changeDto) {
    return (changeDto.getPrevious() != null && changeDto.getPrevious() instanceof Contact)
        || (changeDto.getSuggested() != null && changeDto.getSuggested() instanceof Contact);
  }

  private Change changeDtoToChange(ChangeDto dto) {
    Change change = new Change();
    change.setField(dto.getFieldName());
    change.setPrevious(dto.getPrevious());
    change.setSuggested(dto.getSuggested());
    change.setAuthor(dto.getAuthor());
    change.setCreated(dto.getCreated());
    change.setOverwritten(dto.isOverwritten());

    return change;
  }

  private String getGetterPrefix(Class<?> fieldType) {
    return fieldType.isAssignableFrom(Boolean.TYPE) ? "is" : "get";
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

  private <S> S readJson(String content, Class<S> clazz) {
    try {
      return objectMapper.readValue(content, clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Couldn't read json: " + content);
    }
  }

  private T createEmptyEntityInstance() {
    try {
      return clazz.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new IllegalStateException(
          "Couldn't create new instance of class " + clazz.getSimpleName());
    }
  }

  protected boolean hasRightsToSeeProposerEmail(ChangeSuggestionDto dto) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return grSciCollAuthorizationService.allowedToUpdateChangeSuggestion(
        dto.getKey(), dto.getEntityType().name().toLowerCase(), authentication);
  }

  protected abstract R newEmptyChangeSuggestion();

  protected abstract ChangeSuggestionDto createConvertToCollectionSuggestionDto(R changeSuggestion);

  protected abstract UUID applyConversionToCollection(ChangeSuggestionDto dto);
}
