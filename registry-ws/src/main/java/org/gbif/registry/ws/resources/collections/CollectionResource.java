package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import static org.gbif.registry.ws.util.GrscicollUtils.GRSCICOLL_PATH;

/**
 * Class that acts both as the WS endpoint for {@link Collection} entities and also provides an
 * implementation of {@link CollectionService}.
 */
@Singleton
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(GRSCICOLL_PATH + "/collection")
public class CollectionResource extends BaseExtendableCollectionResource<Collection>
    implements CollectionService {

  private final CollectionMapper collectionMapper;

  @Inject
  public CollectionResource(CollectionMapper collectionMapper, AddressMapper addressMapper,
                            IdentifierMapper identifierMapper,TagMapper tagMapper, EventBus eventBus) {
    super(collectionMapper, addressMapper, collectionMapper, tagMapper, collectionMapper, identifierMapper, collectionMapper,
          eventBus, Collection.class);
    this.collectionMapper = collectionMapper;
  }

  @Override
  void checkUniqueness(Collection entity) {
    // check if the code is unique
    Preconditions.checkArgument(
        !collectionMapper.codeExists(entity.getCode()), "The collection code already exists");
  }

  @Override
  void checkUniquenessInUpdate(Collection oldEntity, Collection newEntity) {
    if (!oldEntity.getCode().equals(newEntity.getCode())) {
      checkUniqueness(newEntity);
    }
  }

  @GET
  public PagingResponse<Collection> list(@Nullable @QueryParam("q") String query,
                                         @Nullable @QueryParam("institution") UUID institutionKey,
                                         @Nullable @QueryParam("contact") UUID contactKey,
                                         @Nullable @QueryParam("code") String code,
                                         @Nullable @QueryParam("name") String name,
                                         @Nullable @Context Pageable page) {
    page = page == null ? new PagingRequest() : page;
    query = query != null ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(query)) : query;
    long total = collectionMapper.count(institutionKey, contactKey, code, name, query);
    return new PagingResponse<>(page, total, collectionMapper.list(institutionKey, contactKey, query, code, name, page));
  }

  @GET
  @Path("deleted")
  @Override
  public PagingResponse<Collection> listDeleted(@Context Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return new PagingResponse<>(page, collectionMapper.countDeleted(), collectionMapper.deleted(page));
  }

  @GET
  @Path("suggest")
  @Override
  public List<KeyCodeNameResult> suggest(@QueryParam("q") String q) {
    return collectionMapper.suggest(q);
  }
}
