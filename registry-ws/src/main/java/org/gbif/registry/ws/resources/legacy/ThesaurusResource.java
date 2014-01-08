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
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle all legacy web service Thesauri requests, previously handled by the GBRDS.
 */
@Singleton
@Path("registry/thesauri")
public class ThesaurusResource {

  private static final Logger LOG = LoggerFactory.getLogger(ThesaurusResource.class);
  private final String thesauriAsJson;

  {
     thesauriAsJson = setThesauriAsJson();
  }

  /**
   * Sets the list of all vocabularies from read file.
   * TODO: replace static list http://dev.gbif.org/issues/browse/REG-394
   *
   * @return list of all vocabularies as JSON
   */
  private String setThesauriAsJson() {
    try {
      return Resources.toString(Resources.getResource("legacy/thesauri.json"), Charsets.UTF_8);
    } catch (IOException e) {
      LOG.error("An error occurred retrieving thesauri: {}", e.getMessage());
    }
    return null;
  }

  /**
   * Get a list of all vocabularies, handling incoming request with path /thesauri.json.
   *
   * @return list of all vocabularies
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getThesauri() {
    // if thesauri weren't loaded, return 500 error
    if (thesauriAsJson == null) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
    }
    return Response.status(Response.Status.OK).entity(thesauriAsJson)
      .cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
  }
}
