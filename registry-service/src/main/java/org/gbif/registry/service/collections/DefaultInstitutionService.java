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
import org.gbif.api.model.collections.latimercore.OrganisationalUnit;
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
import org.gbif.registry.persistence.mapper.*;
import org.gbif.registry.persistence.mapper.GrScicollVocabConceptMapper;
import org.gbif.registry.persistence.mapper.collections.*;
import org.gbif.registry.persistence.mapper.collections.dto.InstitutionGeoJsonDto;
import org.gbif.registry.persistence.mapper.collections.params.InstitutionListParams;
import org.gbif.registry.service.WithMyBatis;
import org.gbif.registry.service.collections.converters.InstitutionConverter;
import org.gbif.registry.service.collections.utils.LatimerCoreConverter;
import org.gbif.registry.service.collections.utils.Vocabularies;
import org.gbif.vocabulary.client.ConceptClient;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.security.UserRoles.*;
import static org.gbif.registry.service.collections.utils.ParamUtils.parseGbifRegion;
import static org.gbif.registry.service.collections.utils.ParamUtils.parseIntegerRangeParameters;

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
      Validator validator,
      ConceptClient conceptClient,
      GrScicollVocabConceptMapper grScicollVocabConceptMapper) {
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
        withMyBatis,
        conceptClient,
        grScicollVocabConceptMapper);
    this.institutionMapper = institutionMapper;
    this.organizationMapper = organizationMapper;
    this.validator = validator;
  }

  @Override
  public PagingResponse<Institution> list(InstitutionSearchRequest searchRequest) {
    return listInternal(searchRequest, false);
  }

  @Override
  public PagingResponse<OrganisationalUnit> listAsLatimerCore(
      InstitutionSearchRequest searchRequest) {

    PagingResponse<Institution> results = listInternal(searchRequest, false);

    List<OrganisationalUnit> organisationalUnits =
        results.getResults().stream()
            .map(LatimerCoreConverter::toOrganisationalUnit)
            .collect(Collectors.toList());

    return new PagingResponse<>(
        results.getOffset(), results.getLimit(), results.getCount(), organisationalUnits);
  }

  @Override
  public OrganisationalUnit getAsLatimerCore(@NotNull UUID key) {
    return LatimerCoreConverter.toOrganisationalUnit(get(key));
  }

  @Override
  public UUID createFromLatimerCore(@NotNull @Valid OrganisationalUnit organisationalUnit) {
    return create(LatimerCoreConverter.fromOrganisationalUnit(organisationalUnit));
  }

  @Override
  public void updateFromLatimerCore(@NotNull @Valid OrganisationalUnit organisationalUnit) {
    UUID key =
        LatimerCoreConverter.getInstitutionKey(organisationalUnit)
            .orElseThrow(() -> new IllegalArgumentException("GRSciColl key is required"));

    Institution institution = institutionMapper.get(key);

    Institution convertedInstitution =
        LatimerCoreConverter.fromOrganisationalUnit(organisationalUnit);
    if (institution.getAddress() != null && convertedInstitution.getAddress() != null) {
      convertedInstitution.getAddress().setKey(institution.getAddress().getKey());
    }

    if (institution.getMailingAddress() != null
        && convertedInstitution.getMailingAddress() != null) {
      convertedInstitution.getMailingAddress().setKey(institution.getMailingAddress().getKey());
    }

    update(convertedInstitution);
  }

  @NotNull
  private PagingResponse<Institution> listInternal(
      InstitutionSearchRequest searchRequest, boolean deleted) {
    if (searchRequest == null) {
      searchRequest = InstitutionSearchRequest.builder().build();
    }

    Pageable page = searchRequest.getPage() == null ? new PagingRequest() : searchRequest.getPage();

    Vocabularies.addChildrenConcepts(searchRequest, conceptClient);

    InstitutionListParams params = buildSearchParams(searchRequest, deleted, page);

    long total = institutionMapper.count(params);
    return new PagingResponse<>(page, total, institutionMapper.list(params));
  }

  private InstitutionListParams buildSearchParams(
      InstitutionSearchRequest searchRequest, boolean deleted, Pageable page) {
    String query =
        searchRequest.getQ() != null
            ? Strings.emptyToNull(CharMatcher.whitespace().trimFrom(searchRequest.getQ()))
            : searchRequest.getQ();

    return InstitutionListParams.builder()
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
        .types(searchRequest.getType())
        .institutionalGovernances(searchRequest.getInstitutionalGovernance())
        .disciplines(searchRequest.getDisciplines())
        .masterSourceType(searchRequest.getMasterSourceType())
        .numberSpecimens(parseIntegerRangeParameters(searchRequest.getNumberSpecimens()))
        .displayOnNHCPortal(searchRequest.getDisplayOnNHCPortal())
        .replacedBy(searchRequest.getReplacedBy())
        .occurrenceCount(parseIntegerRangeParameters(searchRequest.getOccurrenceCount()))
        .typeSpecimenCount(parseIntegerRangeParameters(searchRequest.getTypeSpecimenCount()))
        .institutionKeys(searchRequest.getInstitutionKeys())
        .sourceId(searchRequest.getSourceId())
        .source(searchRequest.getSource())
        .deleted(deleted)
        .sortBy(searchRequest.getSortBy())
        .sortOrder(searchRequest.getSortOrder())
        .contactUserId(searchRequest.getContactUserId())
        .contactEmail(searchRequest.getContactEmail())
        .page(page)
        .build();
  }

  @Override
  public PagingResponse<Institution> listDeleted(InstitutionSearchRequest searchRequest) {
    return listInternal(searchRequest, true);
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

  @Override
  public FeatureCollection listGeojson(InstitutionSearchRequest searchRequest) {
    List<InstitutionGeoJsonDto> dtos =
        institutionMapper.listGeoJson(buildSearchParams(searchRequest, false, null));

    FeatureCollection featureCollection = new FeatureCollection();
    dtos.forEach(
        dto -> {
          Feature feature = new Feature();
          feature.setProperty("key", dto.getKey());
          feature.setProperty("name", dto.getName());
          feature.setGeometry(
              new Point(dto.getLongitude().doubleValue(), dto.getLatitude().doubleValue()));
          featureCollection.add(feature);
        });

    return featureCollection;
  }
}
