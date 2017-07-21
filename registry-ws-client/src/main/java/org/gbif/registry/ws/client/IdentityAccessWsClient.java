package org.gbif.registry.ws.client;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.registry.ws.client.guice.RegistryWs;
import org.gbif.ws.client.BaseWsGetClient;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

/**
 * WebService client implementation of {@link IdentityAccessService}.
 */
public class IdentityAccessWsClient extends BaseWsGetClient<GbifUser, String> implements IdentityAccessService {

  private final WebResource USER_LOGIN_RESOURCE;

  @Inject
  public IdentityAccessWsClient(@RegistryWs WebResource resource, @NotNull ClientFilter authFilter) {
    super(GbifUser.class, resource.path("admin/user"), authFilter);
    USER_LOGIN_RESOURCE = resource.path("user/login");
  }

  @Nullable
  @Override
  public GbifUser get(String userName) {
    return super.get(userName);
  }

  @Nullable
  @Override
  public GbifUser authenticate(String username, String password) {
    return USER_LOGIN_RESOURCE
            .type(MediaType.APPLICATION_JSON)
            .get(this.resourceClass);
  }
}
