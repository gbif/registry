package org.gbif.registry.ws.resources.legacy;

import org.gbif.registry.ws.util.LegacyResourceConstants;

import java.io.IOException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle all legacy web service Extension requests, previously handled by the GBRDS.
 */
@Singleton
@Path("registry/extensions")
public class ExtensionResource {

  private static final Logger LOG = LoggerFactory.getLogger(ExtensionResource.class);

  // Is the Registry configured to use Sandbox mode?
  private final boolean sandboxEnabled;
  private final String extensionsAsJson;

  @Inject
  public ExtensionResource(@Named("sandboxmode.enabled") boolean sandboxEnabled) {
    this.sandboxEnabled = sandboxEnabled;
    extensionsAsJson = setExtensionsAsJson();
  }

  /**
   * If the registry is configured in sandbox mode, sets the extensionsAsJson equal to the list of sandbox extensions.
   * Otherwise it sets the extensionsAsJson equal to the normal list of (production) extensions.
   * TODO: replace static list http://dev.gbif.org/issues/browse/REG-394
   *
   * @return list of all extensions as JSON
   */
  private String setExtensionsAsJson() {
    try {
      return (sandboxEnabled) ? Resources
        .toString(Resources.getResource("legacy/extensions_sandbox.json"), Charsets.UTF_8)
        : Resources.toString(Resources.getResource("legacy/extensions.json"), Charsets.UTF_8);
    } catch (IOException e) {
      LOG.error("An error occurred retrieving extensions: {}", e.getMessage());
    }
    return null;
  }

  /**
   * Get a list of all extensions, handling incoming request with path /extensions.json. If the registry is configured
   * in sandbox mode, the list of sandbox extensions must be returned. Otherwise the normal list of (production)
   * extensions is returned.
   *
   * @return list of all extensions
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getExtensions() {
    // if extensions weren't loaded, return 500 error
    if (extensionsAsJson == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
    }
    return Response.status(Response.Status.OK).entity(extensionsAsJson)
      .cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
  }
}
