package org.gbif.registry.ws.resources.collections;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

import static org.gbif.registry.ws.util.GrscicollUtils.GRSCICOLL_PATH;

/**
 * Class that acts both as the WS endpoint for {@link Institution} entities and also provides an *
 * implementation of {@link InstitutionService}.
 */
@RestController
@RequestMapping(value = GRSCICOLL_PATH + "/institution",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class InstitutionResource extends BaseExtendableCollectionResource<Institution>
    implements InstitutionService {

  private final InstitutionMapper institutionMapper;

  public InstitutionResource(InstitutionMapper institutionMapper,
                             AddressMapper addressMapper,
                             IdentifierMapper identifierMapper,
                             TagMapper tagMapper,
                             EventManager eventManager,
                             WithMyBatis withMyBatis) {
    super(institutionMapper, addressMapper, institutionMapper, tagMapper, institutionMapper, identifierMapper, institutionMapper,
        eventManager, Institution.class, withMyBatis);
    this.institutionMapper = institutionMapper;
  }

  @GetMapping
  public PagingResponse<Institution> list(@Nullable @RequestParam(value = "q", required = false) String query,
                                          @Nullable @RequestParam(value = "contact", required = false) UUID contactKey,
                                          Pageable page) {
    page = page == null ? new PagingRequest() : page;
    query = query != null ? Strings.emptyToNull(CharMatcher.whitespace().trimFrom(query)) : query;
    long total = institutionMapper.count(query, contactKey);
    return new PagingResponse<>(page, total, institutionMapper.list(query, contactKey, page));
  }

  @GetMapping("deleted")
  @Override
  public PagingResponse<Institution> listDeleted(Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return new PagingResponse<>(page, institutionMapper.countDeleted(), institutionMapper.deleted(page));
  }

  @GetMapping("suggest")
  @Override
  public List<KeyCodeNameResult> suggest(@RequestParam(value = "q", required = false) String q) {
    return institutionMapper.suggest(q);
  }
}
