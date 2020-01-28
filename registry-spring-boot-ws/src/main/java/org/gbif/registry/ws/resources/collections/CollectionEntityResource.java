package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.ws.security.EditorAuthorizationService;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.gbif.registry.ws.util.GrscicollUtils.GRSCICOLL_PATH;

/**
 * Class that acts both as the WS endpoint for {@link Collection} entities and also provides an
 * implementation of {@link CollectionService}.
 */
@RestController
@RequestMapping(
    value = GRSCICOLL_PATH + "/collection",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
public class CollectionEntityResource extends ExtendedCollectionEntityResource<Collection>
    implements CollectionService {

  private final CollectionMapper collectionMapper;

  public CollectionEntityResource(
      CollectionMapper collectionMapper,
      AddressMapper addressMapper,
      IdentifierMapper identifierMapper,
      TagMapper tagMapper,
      MachineTagMapper machineTagMapper,
      EventManager eventManager,
      EditorAuthorizationService userAuthService,
      WithMyBatis withMyBatis) {
    super(
        collectionMapper,
        addressMapper,
        tagMapper,
        identifierMapper,
        collectionMapper,
        machineTagMapper,
        eventManager,
        Collection.class,
        userAuthService,
        withMyBatis);
    this.collectionMapper = collectionMapper;
  }

  @GetMapping
  public PagingResponse<Collection> list(@Nullable @RequestParam(value = "q", required = false) String query,
                                         @Nullable @RequestParam(value = "institution", required = false) UUID institutionKey,
                                         @Nullable @RequestParam(value = "contact", required = false) UUID contactKey,
                                         @Nullable @RequestParam(value = "code", required = false) String code,
                                         @Nullable @RequestParam(value = "name", required = false) String name,
                                         Pageable page) {
    page = page == null ? new PagingRequest() : page;
    query = query != null ? Strings.emptyToNull(CharMatcher.whitespace().trimFrom(query)) : query;
    long total = collectionMapper.count(institutionKey, contactKey, query, code, name);
    return new PagingResponse<>(page, total, collectionMapper.list(institutionKey, contactKey, query, code, name, page));
  }

  @GetMapping("deleted")
  @Override
  public PagingResponse<Collection> listDeleted(Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return new PagingResponse<>(page, collectionMapper.countDeleted(), collectionMapper.deleted(page));
  }

  @GetMapping("suggest")
  @Override
  public List<KeyCodeNameResult> suggest(@RequestParam(value = "q", required = false) String q) {
    return collectionMapper.suggest(q);
  }
}
