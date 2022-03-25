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

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.MasterSourceMetadata;
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
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionContactMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.MasterSourceSyncMetadataMapper;
import org.gbif.registry.persistence.mapper.collections.OccurrenceMappingMapper;
import org.gbif.registry.persistence.mapper.collections.dto.CollectionDto;
import org.gbif.registry.persistence.mapper.collections.params.CollectionSearchParams;
import org.gbif.registry.service.WithMyBatis;
import org.gbif.registry.service.collections.converters.CollectionConverter;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import javax.validation.groups.Default;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_EDITOR_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_MEDIATOR_ROLE;

@Validated
@Service
public class DefaultCollectionService extends BasePrimaryCollectionEntityService<Collection>
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
      Validator validator) {
    super(
        Collection.class,
        collectionMapper,
        addressMapper,
        machineTagMapper,
        tagMapper,
        identifierMapper,
        commentMapper,
        occurrenceMappingMapper,
        contactMapper,
        metadataMapper,
        datasetMapper,
        organizationMapper,
        eventManager,
        withMyBatis);
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
    Pageable page = searchRequest.getPage() == null ? new PagingRequest() : searchRequest.getPage();

    String query =
        searchRequest.getQ() != null
            ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(searchRequest.getQ()))
            : searchRequest.getQ();

    CollectionSearchParams params =
        CollectionSearchParams.builder()
            .institutionKey(searchRequest.getInstitution())
            .contactKey(searchRequest.getContact())
            .query(query)
            .code(searchRequest.getCode())
            .name(searchRequest.getName())
            .alternativeCode(searchRequest.getAlternativeCode())
            .machineTagNamespace(searchRequest.getMachineTagNamespace())
            .machineTagName(searchRequest.getMachineTagName())
            .machineTagValue(searchRequest.getMachineTagValue())
            .identifierType(searchRequest.getIdentifierType())
            .identifier(searchRequest.getIdentifier())
            .country(searchRequest.getCountry())
            .city(searchRequest.getCity())
            .fuzzyName(searchRequest.getFuzzyName())
            .active(searchRequest.getActive())
            .contentTypes(searchRequest.getContentTypes())
            .preservationTypes(searchRequest.getPreservationTypes())
            .accessionStatus(searchRequest.getAccessionStatus())
            .personalCollection(searchRequest.getPersonalCollection())
            .masterSourceType(searchRequest.getMasterSourceType())
            .build();

    long total = collectionMapper.count(params);
    List<CollectionDto> collectionDtos = collectionMapper.list(params, page);

    List<CollectionView> views =
        collectionDtos.stream().map(this::convertToCollectionView).collect(Collectors.toList());

    return new PagingResponse<>(page, total, views);
  }

  @Override
  public PagingResponse<CollectionView> listDeleted(@Nullable UUID replacedBy, Pageable page) {
    page = page == null ? new PagingRequest() : page;

    long total = collectionMapper.countDeleted(replacedBy);
    List<CollectionDto> dtos = collectionMapper.deleted(replacedBy, page);
    List<CollectionView> views =
        dtos.stream().map(this::convertToCollectionView).collect(Collectors.toList());

    return new PagingResponse<>(page, total, views);
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
        CollectionConverter.convertFromDataset(dataset, publishingOrganization, collectionCode);

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
