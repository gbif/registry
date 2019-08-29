package org.gbif.registry.ws.resources.collections;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

import static org.gbif.registry.ws.util.GrscicollUtils.GRSCICOLL_PATH;

/**
 * Class that acts both as the WS endpoint for {@link Collection} entities and also provides an
 * implementation of {@link CollectionService}.
 */
@RestController
@RequestMapping(value = GRSCICOLL_PATH + "/collection",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
public class CollectionResource extends BaseExtendableCollectionResource<Collection> implements CollectionService {

  private final CollectionMapper collectionMapper;

  public CollectionResource(CollectionMapper collectionMapper,
                            AddressMapper addressMapper,
                            IdentifierMapper identifierMapper,
                            TagMapper tagMapper,
                            EventManager eventManager,
                            WithMyBatis withMyBatis) {
    super(collectionMapper, addressMapper, collectionMapper, tagMapper, collectionMapper, identifierMapper,
        collectionMapper, eventManager, Collection.class, withMyBatis);
    this.collectionMapper = collectionMapper;
  }

  @RequestMapping(method = RequestMethod.GET)
  public PagingResponse<Collection> list(@Nullable @RequestParam(value = "q", required = false) String query,
                                         @Nullable @RequestParam("institution") UUID institutionKey,
                                         @Nullable @RequestParam("contact") UUID contactKey,
                                         @Nullable Pageable page) {
    page = page == null ? new PagingRequest() : page;
    query = query != null ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(query)) : query;
    long total = collectionMapper.count(institutionKey, contactKey, query);
    return new PagingResponse<>(page, total, collectionMapper.list(institutionKey, contactKey, query, page));
  }

  @GetMapping("deleted")
  @Override
  public PagingResponse<Collection> listDeleted(@Nullable Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return new PagingResponse<>(page, collectionMapper.countDeleted(), collectionMapper.deleted(page));
  }

  @GetMapping("suggest")
  @Override
  public List<KeyCodeNameResult> suggest(@RequestParam("q") String q) {
    return collectionMapper.suggest(q);
  }
}