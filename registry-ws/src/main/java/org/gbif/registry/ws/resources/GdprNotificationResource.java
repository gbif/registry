package org.gbif.registry.ws.resources;

import org.gbif.registry.gdpr.GdprService;
import org.gbif.registry.ws.model.GdprNotification;
import org.gbif.registry.ws.util.LegacyResourceConstants;

import java.util.Objects;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;

/**
 * Server resource for {@link GdprNotification}.
 */
@Singleton
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("gdprNotification")
public class GdprNotificationResource {

  private static final Logger LOG = LoggerFactory.getLogger(GdprNotificationResource.class);

  private final GdprService gdprService;

  @Inject
  public GdprNotificationResource(GdprService gdprService) {
    this.gdprService = gdprService;
  }

  @GET
  @Path("{email}/notified")
  @RolesAllowed(ADMIN_ROLE)
  public Response hasNotification(@PathParam("email") String email, @QueryParam("version") String version) {
    if (Objects.isNull(email)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Email is required").build();
    }

    boolean exists = gdprService.existsNotification(email, version);
    return Response.ok(exists ? "true" : "false").cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
  }

  @POST
  @RolesAllowed(ADMIN_ROLE)
  public Response create(GdprNotification notification) {
    if (Objects.isNull(notification) || Objects.isNull(notification.getEmail())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Email is required").build();
    }

    try {
      gdprService.createNotification(notification.getEmail(), notification.getVersion(), notification.getContext());
    } catch (Exception exc) {
      LOG.error("Could not create GDPR notification for {}", notification.getEmail(), exc);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(exc.getMessage()).
        cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
    }

    return Response.status(Response.Status.CREATED).build();
  }

}
