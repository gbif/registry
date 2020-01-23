package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.ws.security.EditorAuthorizationService;

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
 * Class that acts both as the WS endpoint for {@link Institution} entities and also provides an *
 * implementation of {@link InstitutionService}.
 */
@Singleton
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(GRSCICOLL_PATH + "/institution")
public class InstitutionResource extends ExtendedCollectionEntityResource<Institution>
    implements InstitutionService {

  private final InstitutionMapper institutionMapper;

  @Inject
  public InstitutionResource(
      InstitutionMapper institutionMapper,
      AddressMapper addressMapper,
      IdentifierMapper identifierMapper,
      TagMapper tagMapper,
      MachineTagMapper machineTagMapper,
      EditorAuthorizationService userAuthService,
      EventBus eventBus) {
    super(
        institutionMapper,
        addressMapper,
        tagMapper,
        identifierMapper,
        institutionMapper,
        machineTagMapper,
        eventBus,
        Institution.class,
        userAuthService);
    this.institutionMapper = institutionMapper;
  }

  @Override
  void checkUniqueness(Institution entity) {
    Preconditions.checkArgument(
        !institutionMapper.codeExists(entity.getCode()), "The institution code already exists");
  }

  @Override
  void checkUniquenessInUpdate(Institution oldEntity, Institution newEntity) {
    if (!oldEntity.getCode().equals(newEntity.getCode())) {
      checkUniqueness(newEntity);
    }
  }

  @GET
  public PagingResponse<Institution> list(@Nullable @QueryParam("q") String query,
                                          @Nullable @QueryParam("contact") UUID contactKey,
                                          @Nullable @QueryParam("code") String code,
                                          @Nullable @QueryParam("name") String name,
                                          @Context Pageable page) {
    page = page == null ? new PagingRequest() : page;
    query = query != null ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(query)) : query;
    long total = institutionMapper.count(query, contactKey, code, name);
    return new PagingResponse<>(page, total, institutionMapper.list(query, contactKey, code, name, page));
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
