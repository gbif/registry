package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Class that acts both as the WS endpoint for {@link Institution} entities and also provides an *
 * implementation of {@link InstitutionService}.
 */
@Singleton
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("grscicoll/institution")
public class InstitutionResource extends BaseExtendableCollectionResource<Institution>
    implements InstitutionService {

  private final InstitutionMapper institutionMapper;
  private final EventBus eventBus;

  @Inject
  public InstitutionResource(InstitutionMapper institutionMapper, AddressMapper addressMapper, IdentifierMapper identifierMapper,
                             TagMapper tagMapper, EventBus eventBus) {
    super(institutionMapper, addressMapper, institutionMapper, tagMapper, institutionMapper, identifierMapper, institutionMapper,
          eventBus, Institution.class);
    this.institutionMapper = institutionMapper;
    this.eventBus = eventBus;
  }

  @GET
  public PagingResponse<Institution> list(@Nullable @QueryParam("q") String query,
                                          @Nullable @QueryParam("contact") UUID contactKey,
                                          @Context Pageable page) {
    page = page == null ? new PagingRequest() : page;
    query = Strings.emptyToNull(query);
    long total = institutionMapper.count(query, contactKey);
    return new PagingResponse<>(page, total, institutionMapper.list(query, contactKey, page));
  }

  @GET
  @Path("deleted")
  @Override
  public PagingResponse<Institution> listDeleted(@Context Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return new PagingResponse<>(page, institutionMapper.countDeleted(), institutionMapper.deleted(page));
  }

  @GET
  @Path("suggest")
  @Override
  public List<KeyCodeNameResult> suggest(@QueryParam("q") String q) {
    return institutionMapper.suggest(q);
  }
}
