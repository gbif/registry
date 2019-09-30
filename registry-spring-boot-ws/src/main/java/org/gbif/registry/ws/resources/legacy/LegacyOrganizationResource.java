package org.gbif.registry.ws.resources.legacy;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.codehaus.jackson.map.util.JSONPObject;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.ws.model.ErrorResponse;
import org.gbif.registry.ws.model.LegacyOrganizationBriefResponse;
import org.gbif.registry.ws.model.LegacyOrganizationResponse;
import org.gbif.registry.ws.util.LegacyResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Handle all legacy web service Organization requests, previously handled by the GBRDS.
 */
@RestController
@RequestMapping("registry/organisation")
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

  // TODO: 13/09/2019 use configuration instead of Value
  public LegacyOrganizationResource(@Qualifier("organizationServiceStub") OrganizationService organizationService,
                                    NodeService nodeService,
                                    OrganizationMapper organizationMapper,
                                    @Value("${mail.devemail.enabled}") boolean useDevEmail,
                                    @Value("${spring.mail.host}") String smptHost,
                                    @Value("${spring.mail.port}") int smptPort,
                                    @Value("${spring.mail.username}") String devEmail,
                                    @Value("${mail.cc}") String ccEmail,
                                    @Value("${mail.from}") String fromEmail) {
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
   * @return 1. Organization, wrapped with callback parameter in JSONP, or null if organization with key does not
   * exist.
   * 2. (case: op=login) Response with Status.OK if credentials were verified, or Response with
   * Status.UNAUTHORIZED if they weren't
   * 3. (case: op=password) Response with Status.OK if email reminder was delivered successfully
   */
  @GetMapping(value = "{key}",
      consumes = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE,
          "application/x-javascript", "application/javascript"},
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE,
          "application/x-javascript", "application/javascriptx-javascript"})
  public Object getOrganization(@PathVariable("key") UUID organisationKey,
                                @RequestParam("callback") String callback,
                                @RequestParam("op") String op) {

    // incoming path parameter for organization key required
    if (organisationKey == null) {
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .cacheControl(CacheControl.noCache())
          .build();
    }
    LOG.info("Get Organization with key={}", organisationKey.toString());

    Organization organization = organizationService.get(organisationKey);
    if (organization == null) {
      // the organization didn't exist, and expected response is "{Error: "No organisation matches the key provided}"
      return ResponseEntity
          .status(HttpStatus.OK)
          .cacheControl(CacheControl.noCache())
          .body(new ErrorResponse("No organisation matches the key provided"));
    }

    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (op != null) {
      // ?op=login
      if (op.equalsIgnoreCase("login")) {
        // are the organization credentials matching with the path?
        UUID authKey = LegacyResourceUtils.extractOrgKeyFromSecurity(authentication);

        if (!organisationKey.equals(authKey)) {
          LOG.error("Authorization failed for organization with key={}", organisationKey.toString());
          return ResponseEntity
              .status(HttpStatus.UNAUTHORIZED)
              .cacheControl(CacheControl.noCache())
              .build();
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .cacheControl(CacheControl.noCache())
            .build();
      }
      // ?op=password
      // Email a password reminder to organization's primary contact, and notify requester of success in response
      else if (op.equalsIgnoreCase("password")) {
        // contact email address is nullable, but mandatory for sending a mail
        Contact contact = LegacyResourceUtils.getPrimaryContact(organization);
        String emailAddress = (contact == null || contact.getEmail().isEmpty()) ? null : contact.getEmail().get(0);
        if (emailAddress == null) {
          LOG.error("Password reminder failed: organization primary contact has no email address");
          return ResponseEntity
              .status(HttpStatus.INTERNAL_SERVER_ERROR)
              .cacheControl(CacheControl.noCache())
              .body(new ErrorResponse("Password reminder failed: organization primary contact has no email address"));
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
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.noCache())
                .body(new ErrorResponse("Password reminder failed: " + e.getMessage()));
          }
          LOG.debug("Password reminder sent to: {}", emailAddress);
          return ResponseEntity
              .status(HttpStatus.OK)
              .cacheControl(CacheControl.noCache())
              .body("<html><body><b>The password reminder was sent successfully to the email: </b>" + emailAddress + "</body></html>");
        }
      }
    }

    // retrieve primary contact for organization
    Contact contact = LegacyResourceUtils.getPrimaryContact(organization);
    // construct organization response object
    Node node = nodeService.get(organization.getEndorsingNodeKey());
    LegacyOrganizationResponse org = new LegacyOrganizationResponse(organization, contact, node);

    // callback?
    if (callback != null) {
      return new JSONPObject(callback, org);
    }
    // simple read?
    else {
      return ResponseEntity
          .status(HttpStatus.OK)
          .cacheControl(CacheControl.noCache())
          .body(org);
    }
  }

  /**
   * Get a list of all Organizations, handling incoming request with path /registry/organisation.json. For each
   * Organization, only the key and title(name) fields are required. No authorization is required for this request.
   *
   * @return list of all Organizations
   */
  @RequestMapping(method = RequestMethod.GET, produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity getOrganizations() {
    LOG.debug("List all Organizations for IPT");
    List<LegacyOrganizationBriefResponse> organizations = organizationMapper.listLegacyOrganizationsBrief();

    // return array, required for serialization otherwise get com.sun.jersey.api.MessageException: A message body
    // writer for Java class java.util.ArrayList
    LegacyOrganizationBriefResponse[] array =
        organizations.toArray(new LegacyOrganizationBriefResponse[organizations.size()]);
    return ResponseEntity
        .status(HttpStatus.OK)
        .cacheControl(CacheControl.noCache())
        .body(array);
  }

  /**
   * Build the email body, sent to the primary contact of the organization reminding them of the password.
   *
   * @param contact      primary contact of the organization
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
    body.append("https://www.gbif.org\n");
    body.append(ccEmail);

    return body.toString();
  }
}
