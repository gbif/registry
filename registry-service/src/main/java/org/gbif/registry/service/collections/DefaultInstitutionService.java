package org.gbif.registry.service.collections;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.EventType;
import org.gbif.registry.events.collections.ReplaceEntityEvent;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.OccurrenceMappingMapper;
import org.gbif.registry.persistence.mapper.collections.params.InstitutionSearchParams;
import org.gbif.registry.service.WithMyBatis;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

@Validated
@Service
public class DefaultInstitutionService extends BasePrimaryCollectionEntityService<Institution>
    implements InstitutionService {

  private final InstitutionMapper institutionMapper;

  @Autowired
  protected DefaultInstitutionService(
      InstitutionMapper institutionMapper,
      AddressMapper addressMapper,
      MachineTagMapper machineTagMapper,
      TagMapper tagMapper,
      IdentifierMapper identifierMapper,
      CommentMapper commentMapper,
      OccurrenceMappingMapper occurrenceMappingMapper,
      EventManager eventManager,
      WithMyBatis withMyBatis) {
    super(
        Institution.class,
        institutionMapper,
        addressMapper,
        machineTagMapper,
        tagMapper,
        identifierMapper,
        commentMapper,
        occurrenceMappingMapper,
        eventManager,
        withMyBatis);
    this.institutionMapper = institutionMapper;
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
            .city(searchRequest.getCity())
            .fuzzyName(searchRequest.getFuzzyName())
            .active(searchRequest.getActive())
            .type(searchRequest.getType())
            .institutionalGovernance(searchRequest.getInstitutionalGovernance())
            .disciplines(searchRequest.getDisciplines())
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
}
