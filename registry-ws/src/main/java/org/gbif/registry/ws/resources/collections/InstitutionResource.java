/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.ws.resources.collections;

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.OccurrenceMappingMapper;
import org.gbif.registry.persistence.mapper.collections.params.InstitutionSearchParams;
import org.gbif.registry.security.EditorAuthorizationService;
import org.gbif.registry.service.collections.merge.InstitutionMergeService;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.IDIGBIO_GRSCICOLL_EDITOR_ROLE;

/**
 * Class that acts both as the WS endpoint for {@link Institution} entities and also provides an *
 * implementation of {@link InstitutionService}.
 */
@Validated
@RestController
@RequestMapping(value = "grscicoll/institution", produces = MediaType.APPLICATION_JSON_VALUE)
public class InstitutionResource extends ExtendedCollectionEntityResource<Institution>
    implements InstitutionService {

  private final InstitutionMapper institutionMapper;
  private final InstitutionMergeService institutionMergeService;

  public InstitutionResource(
      InstitutionMapper institutionMapper,
      AddressMapper addressMapper,
      IdentifierMapper identifierMapper,
      TagMapper tagMapper,
      MachineTagMapper machineTagMapper,
      CommentMapper commentMapper,
      OccurrenceMappingMapper occurrenceMappingMapper,
      EditorAuthorizationService userAuthService,
      InstitutionMergeService institutionMergeService,
      EventManager eventManager,
      WithMyBatis withMyBatis) {
    super(
        institutionMapper,
        addressMapper,
        tagMapper,
        identifierMapper,
        institutionMapper,
        machineTagMapper,
        commentMapper,
        occurrenceMappingMapper,
        institutionMapper,
        institutionMergeService,
        eventManager,
        Institution.class,
        userAuthService,
        withMyBatis);
    this.institutionMapper = institutionMapper;
    this.institutionMergeService = institutionMergeService;
  }

  @GetMapping("{key}")
  @NullToNotFound("/grscicoll/institution/{key}")
  @Override
  public Institution get(@PathVariable UUID key) {
    return super.get(key);
  }

  @GetMapping
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
            .contactKey(searchRequest.getContact())
            .code(searchRequest.getCode())
            .name(searchRequest.getName())
            .alternativeCode(searchRequest.getAlternativeCode())
            .machineTagNamespace(searchRequest.getMachineTagNamespace())
            .machineTagName(searchRequest.getMachineTagName())
            .machineTagValue(searchRequest.getMachineTagValue())
            .identifierType(searchRequest.getIdentifierType())
            .identifier(searchRequest.getIdentifier())
            .country(searchRequest.getCountry())
            .build();

    long total = institutionMapper.count(params);
    return new PagingResponse<>(page, total, institutionMapper.list(params, page));
  }

  @GetMapping("deleted")
  @Override
  public PagingResponse<Institution> listDeleted(Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return new PagingResponse<>(
        page, institutionMapper.countDeleted(), institutionMapper.deleted(page));
  }

  @GetMapping("suggest")
  @Override
  public List<KeyCodeNameResult> suggest(@RequestParam(value = "q", required = false) String q) {
    return institutionMapper.suggest(q);
  }

  @PostMapping("{key}/convertToCollection")
  @Secured({GRSCICOLL_ADMIN_ROLE, IDIGBIO_GRSCICOLL_EDITOR_ROLE})
  public UUID convertToCollection(
      @PathVariable("key") UUID entityKey, @RequestBody ConvertToCollectionParams params) {
    return institutionMergeService.convertToCollection(
        entityKey, params.institutionForNewCollectionKey, params.nameForNewInstitution);
  }

  private static final class ConvertToCollectionParams {
    UUID institutionForNewCollectionKey;
    String nameForNewInstitution;

    public UUID getInstitutionForNewCollectionKey() {
      return institutionForNewCollectionKey;
    }

    public void setInstitutionForNewCollectionKey(UUID institutionForNewCollectionKey) {
      this.institutionForNewCollectionKey = institutionForNewCollectionKey;
    }

    public String getNameForNewInstitution() {
      return nameForNewInstitution;
    }

    public void setNameForNewInstitution(String nameForNewInstitution) {
      this.nameForNewInstitution = nameForNewInstitution;
    }
  }
}
