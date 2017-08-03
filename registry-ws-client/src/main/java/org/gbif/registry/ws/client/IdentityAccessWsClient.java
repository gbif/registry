package org.gbif.registry.ws.client;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.registry.ws.client.guice.RegistryWs;
import org.gbif.ws.client.BaseWsGetClient;
import org.gbif.ws.client.guice.GbifApplicationAuthModule;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.google.inject.Inject;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

/**
 * WebService client implementation of {@link IdentityAccessService}.
 * Typically, another app authenticated by appkeys uses this client to get a {@link GbifUser}.
 * This service only makes sense if used with the {@link GbifApplicationAuthModule}.
 *
 * {@link #authenticate(String, String)} method is only there to comply with {@link IdentityAccessService}.
 */
public class IdentityAccessWsClient extends BaseWsGetClient<GbifUser, String> implements IdentityAccessService {

  @Inject
  public IdentityAccessWsClient(@RegistryWs WebResource resource, @NotNull ClientFilter authFilter) {
    super(GbifUser.class, resource.path("admin/user"), authFilter);
  }

  @Nullable
  @Override
  public GbifUser get(String userName) {
    return super.get(userName);
  }

  /**
   * Implemented only to comply with the interface.
   * Authentication is checked on each call so it shall be handled by the {@link ClientFilter}.
   * @param username
   * @param password
   * @return
   */
  @Nullable
  @Override
  public GbifUser authenticate(String username, String password) {
    throw new UnsupportedOperationException("Not implemented in Ws Client.");
  }
}
