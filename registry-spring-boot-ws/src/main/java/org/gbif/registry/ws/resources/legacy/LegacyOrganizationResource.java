package org.gbif.registry.ws.resources.legacy;

import com.fasterxml.jackson.databind.util.JSONPObject;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.domain.ws.ErrorResponse;
import org.gbif.registry.domain.ws.LegacyOrganizationBriefResponse;
import org.gbif.registry.domain.ws.LegacyOrganizationBriefResponseListWrapper;
import org.gbif.registry.domain.ws.LegacyOrganizationResponse;
import org.gbif.registry.domain.ws.util.LegacyResourceUtils;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.ws.NotFoundException;
import org.gbif.ws.util.CommonWsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;

/**
 * Handle all legacy web service Organization requests, previously handled by the GBRDS.
 */
@RestController
@RequestMapping("registry")
public class LegacyOrganizationResource {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyOrganizationResource.class);

  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final OrganizationMapper organizationMapper;
  // if true, send mails to disposable email address service
  private final boolean useDevEmail;
  private final String smtpHost;
  private final Integer smptPort;
  private final String devEmail;
  private final String ccEmail;
  private final String fromEmail;
  private final String password;

  public LegacyOrganizationResource(OrganizationService organizationService,
                                    NodeService nodeService,
                                    OrganizationMapper organizationMapper,
                                    @Value("${mail.devemail.enabled}") boolean useDevEmail,
                                    @Value("${spring.mail.host}") String smtpHost,
                                    @Value("${spring.mail.port:#{NULL}}") Integer smtpPort,
                                    @Value("${mail.devemail.address}") String devEmail,
                                    @Value("${mail.cc}") String ccEmail,
                                    @Value("${mail.from}") String fromEmail,
                                    @Value("${spring.mail.password}") String password) {
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.organizationMapper = organizationMapper;
    this.useDevEmail = useDevEmail;
    this.smtpHost = smtpHost;
    this.smptPort = smtpPort;
    this.devEmail = devEmail;
    this.ccEmail = ccEmail;
    this.fromEmail = fromEmail;
    this.password = password;
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
   * <p>
   *
   * @param organisationKey organization key (UUID) coming in as path param
   * @param callback        parameter
   * @return 1. Organization, wrapped with callback parameter in JSONP, or null if organization with key does not
   * exist.
   * 2. (case: op=login) Response with Status.OK if credentials were verified, or Response with
   * Status.UNAUTHORIZED if they weren't
   * 3. (case: op=password) Response with Status.OK if email reminder was delivered successfully
   */
  @GetMapping(value = {"organisation/{key:[a-zA-Z0-9-]+}", "organisation/{key:[a-zA-Z0-9-]+}{extension:\\.[a-z]+}"},
    consumes = {MediaType.ALL_VALUE},
    produces = {MediaType.APPLICATION_XML_VALUE,
      MediaType.APPLICATION_JSON_VALUE,
      "application/x-javascript",
      "application/javascriptx-javascript"})
  public Object getOrganization(@PathVariable("key") UUID organisationKey,
                                @PathVariable(value = "extension", required = false) String extension,
                                @RequestParam(value = "callback", required = false) String callback,
                                @RequestParam(value = "op", required = false) String op,
                                HttpServletResponse response) {
    String responseType = CommonWsUtils.getResponseTypeByExtension(extension, MediaType.APPLICATION_XML_VALUE);
    if (responseType != null) {
      response.setContentType(responseType);
    } else {
      return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .cacheControl(CacheControl.noCache())
        .build();
    }
    LOG.info("Get Organization with key={}", organisationKey);

    Organization organization;
    try {
      organization = organizationService.get(organisationKey);
    } catch (NotFoundException e) {
      // the organization didn't exist, and expected response is "{Error: "No organisation matches the key provided}"
      return ResponseEntity
        .status(HttpStatus.OK)
        .cacheControl(CacheControl.noCache())
        .body(new ErrorResponse("No organisation matches the key provided"));
    }

    if (op != null) {
      // ?op=login
      if (op.equalsIgnoreCase("login")) {
        // LegacyAuthorizationFilter will cause 401 if wrong organisationKey was used as a login
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
        String emailAddress = (contact == null || contact.getEmail() == null || contact.getEmail().isEmpty())
          ? null
          : contact.getEmail().get(0);
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
              email.setStartTLSEnabled(true);
              email.setSSLCheckServerIdentity(true);
              email.setAuthentication(devEmail, password);
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
   * When no extension provided then xml is default
   *
   * @return list of all Organizations
   */
  @GetMapping(value = {"organisation", "organisation{extension:\\.[a-z]+}"},
    produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity getOrganizations(@PathVariable(required = false, value = "extension") String extension,
                                         HttpServletResponse response) {
    LOG.debug("List all Organizations for IPT");

    String responseType = CommonWsUtils.getResponseTypeByExtension(extension, MediaType.APPLICATION_XML_VALUE);
    if (responseType != null) {
      response.setContentType(responseType);
    } else {
      return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .cacheControl(CacheControl.noCache())
        .build();
    }

    List<LegacyOrganizationBriefResponse> organizations = organizationMapper.listLegacyOrganizationsBrief();

    return ResponseEntity
      .status(HttpStatus.OK)
      .cacheControl(CacheControl.noCache())
      .body(new LegacyOrganizationBriefResponseListWrapper(organizations));
  }

  /**
   * Build the email body, sent to the primary contact of the organization reminding them of the password.
   *
   * @param contact      primary contact of the organization
   * @param organization organization
   * @return email body
   */
  private String getEmailBody(Contact contact, Organization organization) {
    return "Dear " +
      contact.getFirstName() +
      ": \n\n" +
      "You, or someone else, has requested the password for the organisation '" +
      organization.getTitle() +
      "' to be sent to your e-mail address (" +
      contact.getEmail() +
      ")\n\n" +
      "The information requested is: \n\n" +
      "Username: " +
      organization.getKey() +
      "\n" +
      "Password: " +
      organization.getPassword() +
      "\n\n" +
      "If you did not request this information, please disregard this message\n\n" +
      "GBIF (Global Biodiversity Information Facility)\n" +
      "https://www.gbif.org\n" +
      ccEmail;
  }
}
