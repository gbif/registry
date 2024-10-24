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
package org.gbif.registry.service.collections;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.security.UserRoles.*;
import static org.gbif.registry.service.collections.utils.ParamUtils.parseGbifRegion;
import static org.gbif.registry.service.collections.utils.ParamUtils.parseIntegerRangeParameter;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import java.util.*;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.MasterSourceMetadata;
import org.gbif.api.model.collections.latimercore.ObjectGroup;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.CreateCollectionEntityEvent;
import org.gbif.registry.persistence.mapper.*;
import org.gbif.registry.persistence.mapper.collections.*;
import org.gbif.registry.persistence.mapper.collections.dto.CollectionDto;
import org.gbif.registry.persistence.mapper.collections.params.CollectionListParams;
import org.gbif.registry.service.WithMyBatis;
import org.gbif.registry.service.collections.converters.CollectionConverter;
import org.gbif.registry.service.collections.utils.LatimerCoreConverter;
import org.gbif.registry.service.collections.utils.Vocabularies;
import org.gbif.vocabulary.client.ConceptClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
@Service
public class DefaultCollectionService extends BaseCollectionEntityService<Collection>
    implements CollectionService {

  private final CollectionMapper collectionMapper;
  private final DatasetMapper datasetMapper;
  private final OrganizationMapper organizationMapper;
  private Validator validator;

  @Autowired
  protected DefaultCollectionService(
      CollectionMapper collectionMapper,
      AddressMapper addressMapper,
      MachineTagMapper machineTagMapper,
      TagMapper tagMapper,
      IdentifierMapper identifierMapper,
      CommentMapper commentMapper,
      OccurrenceMappingMapper occurrenceMappingMapper,
      CollectionContactMapper contactMapper,
      MasterSourceSyncMetadataMapper metadataMapper,
      DatasetMapper datasetMapper,
      OrganizationMapper organizationMapper,
      EventManager eventManager,
      WithMyBatis withMyBatis,
      Validator validator,
      ConceptClient conceptClient) {
    super(
        collectionMapper,
        addressMapper,
        contactMapper,
        tagMapper,
        machineTagMapper,
        identifierMapper,
        occurrenceMappingMapper,
        metadataMapper,
        datasetMapper,
        organizationMapper,
        commentMapper,
        Collection.class,
        eventManager,
        withMyBatis,
        conceptClient);
    this.collectionMapper = collectionMapper;
    this.datasetMapper = datasetMapper;
    this.organizationMapper = organizationMapper;
    this.validator = validator;
  }

  @Override
  public CollectionView getCollectionView(UUID key) {
    CollectionDto collectionDto = collectionMapper.getCollectionDto(key);

    if (collectionDto == null) {
      return null;
    }

    return convertToCollectionView(collectionDto);
  }

  @Override
  public PagingResponse<CollectionView> list(CollectionSearchRequest searchRequest) {
    return listInternal(searchRequest, false);
  }

  @Override
  public PagingResponse<ObjectGroup> listAsLatimerCore(CollectionSearchRequest searchRequest) {
    PagingResponse<CollectionView> results = listInternal(searchRequest, false);

    List<ObjectGroup> objectGroups =
        results.getResults().stream()
            .map(c -> LatimerCoreConverter.toObjectGroup(c, conceptClient))
            .collect(Collectors.toList());

    return new PagingResponse<>(
        results.getOffset(), results.getLimit(), results.getCount(), objectGroups);
  }

  @NotNull
  private PagingResponse<CollectionView> listInternal(
      CollectionSearchRequest searchRequest, boolean deleted) {
    if (searchRequest == null) {
      searchRequest = CollectionSearchRequest.builder().build();
    }

    Pageable page = searchRequest.getPage() == null ? new PagingRequest() : searchRequest.getPage();

    String query =
        searchRequest.getQ() != null
            ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(searchRequest.getQ()))
            : searchRequest.getQ();

    Set<UUID> institutionKeys = new HashSet<>();
    if (searchRequest.getInstitution() != null) {
      institutionKeys.addAll(searchRequest.getInstitution());
    }
    if (searchRequest.getInstitutionKeys() != null) {
      institutionKeys.addAll(searchRequest.getInstitutionKeys());
    }

    Vocabularies.addChildrenConcepts(searchRequest, conceptClient);

    CollectionListParams params =
        CollectionListParams.builder()
            .query(query)
            .code(searchRequest.getCode())
            .name(searchRequest.getName())
            .alternativeCode(searchRequest.getAlternativeCode())
            .machineTagNamespace(searchRequest.getMachineTagNamespace())
            .machineTagName(searchRequest.getMachineTagName())
            .machineTagValue(searchRequest.getMachineTagValue())
            .identifierType(searchRequest.getIdentifierType())
            .identifier(searchRequest.getIdentifier())
            .countries(searchRequest.getCountry())
            .regionCountries(parseGbifRegion(searchRequest))
            .city(searchRequest.getCity())
            .fuzzyName(searchRequest.getFuzzyName())
            .active(searchRequest.getActive())
            .contentTypes(searchRequest.getContentTypes())
            .preservationTypes(searchRequest.getPreservationTypes())
            .accessionStatus(searchRequest.getAccessionStatus())
            .personalCollection(searchRequest.getPersonalCollection())
            .masterSourceType(searchRequest.getMasterSourceType())
            .numberSpecimens(parseIntegerRangeParameter(searchRequest.getNumberSpecimens()))
            .displayOnNHCPortal(searchRequest.getDisplayOnNHCPortal())
            .replacedBy(searchRequest.getReplacedBy())
            .occurrenceCount(parseIntegerRangeParameter(searchRequest.getOccurrenceCount()))
            .typeSpecimenCount(parseIntegerRangeParameter(searchRequest.getTypeSpecimenCount()))
            .deleted(deleted)
            .sourceId(searchRequest.getSourceId())
            .source(searchRequest.getSource())
            .institutionKeys(new ArrayList<>(institutionKeys))
            .sortBy(searchRequest.getSortBy())
            .sortOrder(searchRequest.getSortOrder())
            .page(page)
            .build();

    long total = collectionMapper.count(params);
    List<CollectionDto> collectionDtos = collectionMapper.list(params);

    List<CollectionView> views =
        collectionDtos.stream().map(this::convertToCollectionView).collect(Collectors.toList());

    return new PagingResponse<>(page, total, views);
  }

  @Override
  public PagingResponse<CollectionView> listDeleted(CollectionSearchRequest searchRequest) {
    return listInternal(searchRequest, true);
  }

  @Override
  public ObjectGroup getAsLatimerCore(@NotNull UUID key) {
    return LatimerCoreConverter.toObjectGroup(getCollectionView(key), conceptClient);
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Override
  public UUID createFromLatimerCore(@NotNull @Valid ObjectGroup objectGroup) {
    Collection convertedCollection = LatimerCoreConverter.fromObjectGroup(objectGroup);
    UUID key = create(convertedCollection);

    // create contacts
    convertedCollection.getContactPersons().forEach(c -> addContactPerson(key, c));

    return key;
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Override
  public void updateFromLatimerCore(@NotNull @Valid ObjectGroup objectGroup) {
    UUID key =
        LatimerCoreConverter.getCollectionKey(objectGroup)
            .orElseThrow(() -> new IllegalArgumentException("GRSciColl key is required"));

    Collection collection = collectionMapper.get(key);

    Collection convertedCollection = LatimerCoreConverter.fromObjectGroup(objectGroup);
    if (collection.getAddress() != null && convertedCollection.getAddress() != null) {
      convertedCollection.getAddress().setKey(collection.getAddress().getKey());
    }

    if (collection.getMailingAddress() != null && convertedCollection.getMailingAddress() != null) {
      convertedCollection.getMailingAddress().setKey(collection.getMailingAddress().getKey());
    }

    update(convertedCollection);

    // update contacts
    List<Integer> oldContacts =
        collection.getContactPersons().stream().map(Contact::getKey).collect(Collectors.toList());
    convertedCollection
        .getContactPersons()
        .forEach(
            c -> {
              if (c.getKey() != null) {
                updateContactPerson(key, c);
                oldContacts.remove(c.getKey());
              } else {
                addContactPerson(key, c);
              }
            });

    // delete contacts
    oldContacts.forEach(c -> removeContactPerson(key, c));
  }

  @Override
  public List<KeyCodeNameResult> suggest(@RequestParam(value = "q", required = false) String q) {
    return collectionMapper.suggest(q);
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Validated({PrePersist.class, Default.class})
  @Override
  public UUID createFromDataset(UUID datasetKey, String collectionCode) {
    checkArgument(datasetKey != null, "Dataset key is required");
    checkArgument(!Strings.isNullOrEmpty(collectionCode), "Collection code is required");

    Dataset dataset = datasetMapper.get(datasetKey);
    checkArgument(dataset != null, "Dataset not found");

    Organization publishingOrganization =
        organizationMapper.get(dataset.getPublishingOrganizationKey());
    checkArgument(publishingOrganization != null, "Publishing organization not found");

    Collection collection =
        CollectionConverter.convertFromDataset(
            dataset, publishingOrganization, collectionCode, conceptClient);

    preCreate(collection);

    collection.setKey(UUID.randomUUID());
    baseMapper.create(collection);

    UUID collectionKey = collection.getKey();

    // create contacts
    collection
        .getContactPersons()
        .forEach(
            contact -> {
              // the validation is done manually because the automatic one is not triggered when the
              // calls are done within the same bean
              Set<ConstraintViolation<Contact>> violations = validator.validate(contact);
              if (!violations.isEmpty()) {
                throw new ConstraintViolationException("Invalid contact", violations);
              }

              addContactPerson(collectionKey, contact);
            });

    // create identifiers
    collection.getIdentifiers().forEach(identifier -> addIdentifier(collectionKey, identifier));

    // create master source sync metadata
    addMasterSourceMetadata(
        collectionKey, new MasterSourceMetadata(Source.DATASET, datasetKey.toString()));

    eventManager.post(CreateCollectionEntityEvent.newInstance(collection));

    return collectionKey;
  }

  private CollectionView convertToCollectionView(CollectionDto dto) {
    CollectionView collectionView = new CollectionView(dto.getCollection());
    collectionView.setInstitutionCode(dto.getInstitutionCode());
    collectionView.setInstitutionName(dto.getInstitutionName());
    return collectionView;
  }
}
