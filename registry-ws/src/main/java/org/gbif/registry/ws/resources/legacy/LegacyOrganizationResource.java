package org.gbif.registry.ws.resources.legacy;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.ws.model.ErrorResponse;
import org.gbif.registry.ws.model.LegacyOrganizationBriefResponse;
import org.gbif.registry.ws.model.LegacyOrganizationResponse;
import org.gbif.registry.ws.security.LegacyAuthorizationFilter;
import org.gbif.registry.ws.util.LegacyResourceConstants;
import org.gbif.registry.ws.util.LegacyResourceUtils;

import java.util.List;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.codehaus.jackson.map.util.JSONPObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle all legacy web service Organization requests, previously handled by the GBRDS.
 */
@Singleton
@Path("registry/organisation")
public class LegacyOrganizationResource {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyOrganizationResource.class);

  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final OrganizationMapper organizationMapper;
  // if true, send mails to disposable email address service
  private final boolean useDevEmail;
  private final String smtpHost;
  private final int smptPort;
  private final String devEmail;
  private final String ccEmail;
  private final String fromEmail;

  @Inject
  public LegacyOrganizationResource(OrganizationService organizationService, NodeService nodeService,
    OrganizationMapper organizationMapper, @Named("useDevEmail") boolean useDevEmail,
    @Named("smptHost") String smptHost, @Named("smptPort") int smptPort, @Named("devEmail") String devEmail,
    @Named("ccEmail") String ccEmail, @Named("fromEmail") String fromEmail) {
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.organizationMapper = organizationMapper;
    this.useDevEmail = useDevEmail;
    this.smtpHost = smptHost;
    this.smptPort = smptPort;
    this.devEmail = devEmail;
    this.ccEmail = ccEmail;
    this.fromEmail = fromEmail;
  }

  /**
   * This sub-resource can be called for various reasons:
   * </br>
   * 1. Get an Organization, handling incoming request with path /registry/organization/{key}.json?callback=?,
   * signifying that the response must be JSONP. This request is made in order to verify that an organization exists.
   * No authorization is required for this request.
   * </br>
   * 2. Validate the organization credentials sent with incoming GET request. Handling request with path
   * /registry/organization/{key}.json?op=login. Only after the credentials have been verified, is the
   * Response with Status.OK returned.
   * 3. Trigger an email reminder for the organization, sent to the primary contact email. Handling request with path
   * /registry/organization/{key}.json?op=password. An HTML response indicating successful delivery is included in 200
   * response.
   *
   * @param organisationKey organization key (UUID) coming in as path param
   * @param callback        parameter
   *
   * @return 1. Organization, wrapped with callback parameter in JSONP, or null if organization with key does not
   *         exist.
   *         2. (case: op=login) Response with Status.OK if credentials were verified, or Response with
   *         Status.UNAUTHORIZED if they weren't
   *         3. (case: op=password) Response with Status.OK if email reminder was delivered successfully
   */
  @GET
  @Path("{key}")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON,
    "application/x-javascript", "application/javascriptx-javascript"})
  @Consumes(
    {MediaType.TEXT_PLAIN, MediaType.APPLICATION_FORM_URLENCODED, "application/x-javascript", "application/javascript"})
  public Object getOrganization(@PathParam("key") UUID organisationKey, @QueryParam("callback") String callback,
    @QueryParam("op") String op, @Context SecurityContext security) {

    // incoming path parameter for organization key required
    if (organisationKey == null) {
      return Response.status(Response.Status.BAD_REQUEST).cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED)
        .build();
    }
    LOG.info("Get Organization with key={}", organisationKey.toString());

    Organization organization = organizationService.get(organisationKey);
    if (organization == null) {
      // the organization didn't exist, and expected response is "{Error: "No organisation matches the key provided}"
      return Response.status(Response.Status.OK).entity(new ErrorResponse("No organisation matches the key provided"))
        .cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
    }

    if (op != null) {
      // ?op=login
      if (op.equalsIgnoreCase("login")) {
        // are the organization credentials matching with the path?
        UUID authKey = LegacyAuthorizationFilter.extractOrgKeyFromSecurity(security);

        if (!organisationKey.equals(authKey)) {
          LOG.error("Authorization failed for organization with key={}", organisationKey.toString());
          return Response.status(Response.Status.UNAUTHORIZED)
            .cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
        }
        return Response.status(Response.Status.OK).cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
      }
      // ?op=password
      // Email a password reminder to organization's primary contact, and notify requester of success in response
      else if (op.equalsIgnoreCase("password")) {
        // contact email address is nullable, but mandatory for sending a mail
        Contact contact = LegacyResourceUtils.getPrimaryContact(organization);
        String emailAddress = (contact == null) ? null: contact.getEmail();
        if (emailAddress == null) {
          LOG.error("Password reminder failed: organization primary contact has no email address");
          return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(new ErrorResponse("Password reminder failed: organization primary contact has no email address"))
            .cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
        } else {
          try {
            Email email = new SimpleEmail();
            email.setHostName(smtpHost);
            email.setSmtpPort(smptPort);
            email.setFrom(fromEmail);
            email.setSubject("GBIF: Password reminder for: " + organization.getTitle());
            email.setMsg(getEmailBody(contact, organization));

            // add recipients, depending on whether development mode is on for sending email?
            if (useDevEmail) {
              email.addTo(devEmail);
            } else {
              email.addTo(emailAddress);
              email.addCc(ccEmail);
            }

            email.send();
          } catch (EmailException e) {
            LOG.error("Password reminder failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity(new ErrorResponse("Password reminder failed: " + e.getMessage()))
              .type(MediaType.APPLICATION_JSON).cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
          }
          LOG.debug("Password reminder sent to: {}", emailAddress);
          return Response.status(Response.Status.OK).entity(
            "<html><body><b>The password reminder was sent successfully to the email: </b>" + emailAddress
            + "</body></html>").cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
        }
      }
    }

    // retrieve primary contact for organization
    Contact contact = LegacyResourceUtils.getPrimaryContact(organization);
    // construct organization response object
    Node node = nodeService.get(organization.getEndorsingNodeKey());
    LegacyOrganizationResponse o = new LegacyOrganizationResponse(organization, contact, node);

    // callback?
    if (callback != null) {
      return new JSONPObject(callback, o);
    }
    // simple read?
    else {
      return Response.status(Response.Status.OK).entity(o).cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED)
        .build();
    }
  }

  /**
   * Get a list of all Organizations, handling incoming request with path /registry/organisation.json. For each
   * Organization, only the key and title(name) fields are required. No authorization is required for this request.
   *
   * @return list of all Organizations
   */
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public Response getOrganizations() {
    LOG.debug("List all Organizations for IPT");
    List<LegacyOrganizationBriefResponse> organizations = organizationMapper.listLegacyOrganizationsBrief();

    // return array, required for serialization otherwise get com.sun.jersey.api.MessageException: A message body
    // writer for Java class java.util.ArrayList
    LegacyOrganizationBriefResponse[] array =
      organizations.toArray(new LegacyOrganizationBriefResponse[organizations.size()]);
    return Response.status(Response.Status.OK).entity(array)
      .cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
  }

  /**
   * Build the email body, sent to the primary contact of the organization reminding them of the password.
   *
   * @param contact primary contact of the organization
   * @param organization organization
   * @return email body
   */
  private String getEmailBody(Contact contact, Organization organization) {
    StringBuilder body = new StringBuilder();
    body.append("Dear ");
    body.append(contact.getFirstName());
    body.append(": \n\n");

    body.append("You, or someone else, has requested the password for the organisation '");
    body.append(organization.getTitle());
    body.append("' to be sent to your e-mail address (");
    body.append(contact.getEmail());
    body.append(")\n\n");

    body.append("The information requested is: \n\n");
    body.append("Username: ");
    body.append(organization.getKey());
    body.append("\n");
    body.append("Password: ");
    body.append(organization.getPassword());
    body.append("\n\n");

    body.append("If you did not request this information, please disregard this message\n\n");

    body.append("GBIF (Global Biodiversity Information Facility)\n");
    body.append("http://www.gbif.org\n");
    body.append(ccEmail);

    return body.toString();
  }

}
