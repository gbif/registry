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

import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.MasterSourceMetadata;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.CreateCollectionEntityEvent;
import org.gbif.registry.events.collections.EventType;
import org.gbif.registry.events.collections.ReplaceEntityEvent;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionContactMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.MasterSourceSyncMetadataMapper;
import org.gbif.registry.persistence.mapper.collections.OccurrenceMappingMapper;
import org.gbif.registry.persistence.mapper.collections.params.InstitutionSearchParams;
import org.gbif.registry.service.WithMyBatis;
import org.gbif.registry.service.collections.converters.InstitutionConverter;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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
public class DefaultInstitutionService extends BaseCollectionEntityService<Institution>
    implements InstitutionService {

  private final InstitutionMapper institutionMapper;
  private final OrganizationMapper organizationMapper;
  private Validator validator;

  @Autowired
  protected DefaultInstitutionService(
      InstitutionMapper institutionMapper,
      AddressMapper addressMapper,
      MachineTagMapper machineTagMapper,
      TagMapper tagMapper,
      IdentifierMapper identifierMapper,
      CommentMapper commentMapper,
      OccurrenceMappingMapper occurrenceMappingMapper,
      MasterSourceSyncMetadataMapper metadataMapper,
      DatasetMapper datasetMapper,
      CollectionContactMapper contactMapper,
      OrganizationMapper organizationMapper,
      EventManager eventManager,
      WithMyBatis withMyBatis,
      Validator validator) {
    super(
        institutionMapper,
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
        Institution.class,
        eventManager,
        withMyBatis);
    this.institutionMapper = institutionMapper;
    this.organizationMapper = organizationMapper;
    this.validator = validator;
  }

  @Override
  public PagingResponse<Institution> list(InstitutionSearchRequest searchRequest) {
    Pageable page = searchRequest.getPage() == null ? new PagingRequest() : searchRequest.getPage();

    String query =
        searchRequest.getQ() != null
            ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(searchRequest.getQ()))
            : searchRequest.getQ();

    InstitutionSearchParams params =
        InstitutionSearchParams.builder()
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
            .type(searchRequest.getType())
            .institutionalGovernance(searchRequest.getInstitutionalGovernance())
            .disciplines(searchRequest.getDisciplines())
            .masterSourceType(searchRequest.getMasterSourceType())
            .numberSpecimens(parseNumberSpecimensParameter(searchRequest.getNumberSpecimens()))
            .displayOnNHCPortal(searchRequest.getDisplayOnNHCPortal())
            .build();

    long total = institutionMapper.count(params);
    return new PagingResponse<>(page, total, institutionMapper.list(params, page));
  }

  @Override
  public PagingResponse<Institution> listDeleted(@Nullable UUID replacedBy, Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return new PagingResponse<>(
        page,
        institutionMapper.countDeleted(replacedBy),
        institutionMapper.deleted(replacedBy, page));
  }

  @Override
  public List<KeyCodeNameResult> suggest(@RequestParam(value = "q", required = false) String q) {
    return institutionMapper.suggest(q);
  }

  @Override
  public void convertToCollection(UUID targetEntityKey, UUID collectionKey) {
    institutionMapper.convertToCollection(targetEntityKey, collectionKey);
    eventManager.post(
        ReplaceEntityEvent.newInstance(
            Institution.class, targetEntityKey, collectionKey, EventType.CONVERSION_TO_COLLECTION));
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Validated({PrePersist.class, Default.class})
  @Override
  public UUID createFromOrganization(UUID organizationKey, String institutionCode) {
    checkArgument(organizationKey != null, "Organization key is required");
    checkArgument(!Strings.isNullOrEmpty(institutionCode), "Institution code is required");

    Organization organization = organizationMapper.get(organizationKey);
    checkArgument(organization != null, "Organization not found");

    Institution institution =
        InstitutionConverter.convertFromOrganization(organization, institutionCode);

    preCreate(institution);

    institution.setKey(UUID.randomUUID());
    baseMapper.create(institution);

    UUID institutionKey = institution.getKey();

    // create contacts
    institution
        .getContactPersons()
        .forEach(
            contact -> {
              // the validation is done manually because the automatic one is not triggered when the
              // calls are done within the same bean
              Set<ConstraintViolation<Contact>> violations = validator.validate(contact);
              if (!violations.isEmpty()) {
                throw new ConstraintViolationException("Invalid contact", violations);
              }

              addContactPerson(institutionKey, contact);
            });

    // create machine tag for source
    addMasterSourceMetadata(
        institutionKey, new MasterSourceMetadata(Source.ORGANIZATION, organizationKey.toString()));

    eventManager.post(CreateCollectionEntityEvent.newInstance(institution));

    return institutionKey;
  }
}
