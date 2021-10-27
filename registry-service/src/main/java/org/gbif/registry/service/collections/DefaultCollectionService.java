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
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionContactMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.OccurrenceMappingMapper;
import org.gbif.registry.persistence.mapper.collections.dto.CollectionDto;
import org.gbif.registry.persistence.mapper.collections.params.CollectionSearchParams;
import org.gbif.registry.service.WithMyBatis;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

@Validated
@Service
public class DefaultCollectionService extends BasePrimaryCollectionEntityService<Collection>
    implements CollectionService {

  private final CollectionMapper collectionMapper;

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
      EventManager eventManager,
      WithMyBatis withMyBatis) {
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
        eventManager,
        withMyBatis);
    this.collectionMapper = collectionMapper;
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

  private CollectionView convertToCollectionView(CollectionDto dto) {
    CollectionView collectionView = new CollectionView(dto.getCollection());
    collectionView.setInstitutionCode(dto.getInstitutionCode());
    collectionView.setInstitutionName(dto.getInstitutionName());
    return collectionView;
  }
}
