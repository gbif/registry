/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.resources.legacy;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.domain.ws.ErrorResponse;
import org.gbif.registry.domain.ws.LegacyOrganizationBriefResponse;
import org.gbif.registry.domain.ws.LegacyOrganizationBriefResponseListWrapper;
import org.gbif.registry.domain.ws.LegacyOrganizationResponse;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.ws.surety.OrganizationEmailEndorsementService;
import org.gbif.registry.ws.util.LegacyResourceUtils;
import org.gbif.ws.NotFoundException;
import org.gbif.ws.util.CommonWsUtils;

import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.util.JSONPObject;

/** Handle all legacy web service Organization requests, previously handled by the GBRDS. */
@RestController
@RequestMapping("registry")
public class LegacyOrganizationResource {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyOrganizationResource.class);

  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final OrganizationMapper organizationMapper;
  private final OrganizationEmailEndorsementService emailManager;

  public LegacyOrganizationResource(
      OrganizationService organizationService,
      NodeService nodeService,
      OrganizationMapper organizationMapper,
      OrganizationEmailEndorsementService emailManager) {
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.organizationMapper = organizationMapper;
    this.emailManager = emailManager;
  }

  /**
   * This sub-resource can be called for various reasons: </br> 1. Get an Organization, handling
   * incoming request with path /registry/organization/{key}.json?callback=?, signifying that the
   * response must be JSONP. This request is made in order to verify that an organization exists. No
   * authorization is required for this request. </br> 2. Validate the organization credentials sent
   * with incoming GET request. Handling request with path
   * /registry/organization/{key}.json?op=login. Only after the credentials have been verified, is
   * the Response with Status.OK returned. 3. Trigger an email reminder for the organization, sent
   * to the primary contact email. Handling request with path
   * /registry/organization/{key}.json?op=password. An HTML response indicating successful delivery
   * is included in 200 response.
   *
   * <p>
   *
   * @param organisationKey organization key (UUID) coming in as path param
   * @param callback parameter
   * @return 1. Organization, wrapped with callback parameter in JSONP, or null if organization with
   *     key does not exist. 2. (case: op=login) Response with Status.OK if credentials were
   *     verified, or Response with Status.UNAUTHORIZED if they weren't 3. (case: op=password)
   *     Response with Status.OK if email reminder was delivered successfully
   */
  @GetMapping(
      value = {
        "organisation/{key:[a-zA-Z0-9-]+}",
        "organisation/{key:[a-zA-Z0-9-]+}{extension:\\.[a-z]+}"
      },
      consumes = {MediaType.ALL_VALUE},
      produces = {
        MediaType.APPLICATION_XML_VALUE,
        MediaType.APPLICATION_JSON_VALUE,
        "application/x-javascript",
        "application/javascriptx-javascript"
      })
  public Object getOrganization(
      @PathVariable("key") UUID organisationKey,
      @PathVariable(value = "extension", required = false) String extension,
      @RequestParam(value = "callback", required = false) String callback,
      @RequestParam(value = "op", required = false) String op,
      HttpServletResponse response) {
    String responseType =
        CommonWsUtils.getResponseTypeByExtension(extension, MediaType.APPLICATION_XML_VALUE);
    if (responseType != null) {
      response.setContentType(responseType);
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .cacheControl(CacheControl.noCache())
          .build();
    }
    LOG.debug("Get Organization with key={}", organisationKey);

    Organization organization;
    try {
      organization = organizationService.get(organisationKey);
    } catch (NotFoundException e) {
      // the organization didn't exist
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .cacheControl(CacheControl.noCache())
          .build();
    }

    if (op != null) {
      // ?op=login
      if (op.equalsIgnoreCase("login")) {
        // LegacyAuthorizationFilter will cause 401 if wrong organisationKey was used as a login
        return ResponseEntity.status(HttpStatus.OK).cacheControl(CacheControl.noCache()).build();
      }
      // ?op=password
      // Email a password reminder to organization's primary contact, and notify requester of
      // success in response
      else if (op.equalsIgnoreCase("password")) {
        // contact email address is nullable, but mandatory for sending a mail
        Contact contact = LegacyResourceUtils.getPrimaryContact(organization);
        String emailAddress =
            (contact == null || contact.getEmail() == null || contact.getEmail().isEmpty())
                ? null
                : contact.getEmail().get(0);
        if (emailAddress == null) {
          LOG.error("Password reminder failed: organization primary contact has no email address");
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .cacheControl(CacheControl.noCache())
              .body(
                  new ErrorResponse(
                      "Password reminder failed: organization primary contact has no email address"));
        } else {
          boolean reminderResultSuccess =
              emailManager.passwordReminder(organization, contact, emailAddress);
          if (!reminderResultSuccess) {
            LOG.error("Password reminder failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.noCache())
                .body(new ErrorResponse("Password reminder failed"));
          }
          LOG.debug("Password reminder sent to: {}", emailAddress);
          return ResponseEntity.status(HttpStatus.OK)
              .cacheControl(CacheControl.noCache())
              .body(
                  "<html><body><b>The password reminder was sent successfully to the email: </b>"
                      + emailAddress
                      + "</body></html>");
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
      return ResponseEntity.status(HttpStatus.OK)
          .contentType(MediaType.parseMediaType(responseType))
          .cacheControl(CacheControl.noCache())
          .body(org);
    }
  }

  /**
   * Get a list of all Organizations, handling incoming request with path
   * /registry/organisation.json. For each Organization, only the key and title(name) fields are
   * required. No authorization is required for this request. When no extension provided then xml is
   * default
   *
   * @return list of all Organizations
   */
  @GetMapping(
      value = {"organisation", "organisation{extension:\\.[a-z]+}"},
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<LegacyOrganizationBriefResponseListWrapper> getOrganizations(
      @PathVariable(required = false, value = "extension") String extension,
      HttpServletResponse response) {
    LOG.debug("List all Organizations for IPT");

    String responseType =
        CommonWsUtils.getResponseTypeByExtension(extension, MediaType.APPLICATION_XML_VALUE);
    if (responseType != null) {
      response.setContentType(responseType);
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .cacheControl(CacheControl.noCache())
          .build();
    }

    List<LegacyOrganizationBriefResponse> organizations =
        organizationMapper.listLegacyOrganizationsBrief();

    return ResponseEntity.status(HttpStatus.OK)
        .cacheControl(CacheControl.noCache())
        .contentType(MediaType.parseMediaType(responseType))
        .body(new LegacyOrganizationBriefResponseListWrapper(organizations));
  }
}
