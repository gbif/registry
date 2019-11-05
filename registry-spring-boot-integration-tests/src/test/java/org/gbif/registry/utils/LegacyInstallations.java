package org.gbif.registry.utils;

import com.google.common.collect.Lists;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.registry.ws.model.LegacyInstallation;
import org.gbif.registry.ws.util.LegacyResourceConstants;
import org.springframework.http.HttpHeaders;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class LegacyInstallations {

  // set of HTTP form parameters sent in POST request
  public static final String IPT_NAME = "Test IPT Registry2";
  public static final String IPT_DESCRIPTION = "Description of Test IPT";
  public static final String IPT_PRIMARY_CONTACT_TYPE = "technical";
  public static final String IPT_PRIMARY_CONTACT_NAME = "Kyle Braak";
  public static final List<String> IPT_PRIMARY_CONTACT_EMAIL = Lists.newArrayList("kbraak@gbif.org");
  public static final String IPT_SERVICE_TYPE = "RSS";
  public static final URI IPT_SERVICE_URL = URI.create("http://ipt.gbif.org/rss.do");
  public static final String IPT_WS_PASSWORD = "welcome";

  /**
   * Populate a list of name value pairs used in the common ws requests for IPT registrations and updates.
   * </br>
   * Basically a copy of the method in the IPT, to ensure the parameter names are identical.
   *
   * @param organizationKey organization key (UUID)
   * @return list of name value pairs, or an empty list if the IPT or organisation key were null
   */
  public static HttpHeaders buildParams(UUID organizationKey) {
    HttpHeaders httpHeaders = new HttpHeaders();
    // main
    httpHeaders.put(LegacyResourceConstants.ORGANIZATION_KEY_PARAM, Collections.singletonList(organizationKey.toString()));
    httpHeaders.put(LegacyResourceConstants.NAME_PARAM, Collections.singletonList(IPT_NAME));
    httpHeaders.put(LegacyResourceConstants.DESCRIPTION_PARAM, Collections.singletonList(IPT_DESCRIPTION));

    // primary contact
    httpHeaders.put(LegacyResourceConstants.PRIMARY_CONTACT_TYPE_PARAM, Collections.singletonList(IPT_PRIMARY_CONTACT_TYPE));
    httpHeaders.put(LegacyResourceConstants.PRIMARY_CONTACT_NAME_PARAM, Collections.singletonList(IPT_PRIMARY_CONTACT_NAME));
    httpHeaders.put(LegacyResourceConstants.PRIMARY_CONTACT_EMAIL_PARAM, IPT_PRIMARY_CONTACT_EMAIL);

    // service/endpoint
    httpHeaders.put(LegacyResourceConstants.SERVICE_TYPES_PARAM, Collections.singletonList(IPT_SERVICE_TYPE));
    httpHeaders.put(LegacyResourceConstants.SERVICE_URLS_PARAM, Collections.singletonList(IPT_SERVICE_URL.toASCIIString()));

    // add IPT password used for updating the IPT's own metadata & issuing atomic updateURL operations
    httpHeaders.put(LegacyResourceConstants.WS_PASSWORD_PARAM, Collections.singletonList(IPT_WS_PASSWORD));

    return httpHeaders;
  }

  public static LegacyInstallation newInstance(UUID organizationKey) {
    LegacyInstallation installation = new LegacyInstallation();
    installation.setOrganizationKey(organizationKey);
    // main
    installation.setIptName(IPT_NAME);
    installation.setIptDescription(IPT_DESCRIPTION);
    // primary contact
    installation.setPrimaryContactType(IPT_PRIMARY_CONTACT_TYPE);
    installation.setPrimaryContactName(IPT_PRIMARY_CONTACT_NAME);
    installation.setPrimaryContactEmail(IPT_PRIMARY_CONTACT_EMAIL.get(0));
    // service/endpoint
    installation.setEndpointType(IPT_SERVICE_TYPE);
    installation.setEndpointUrl(IPT_SERVICE_URL.toASCIIString());
    // add IPT password used for updating the IPT's own metadata & issuing atomic updateURL operations
    installation.setWsPassword(IPT_WS_PASSWORD);

    Contact contact = new Contact();
    contact.setFirstName(IPT_PRIMARY_CONTACT_NAME);
    contact.setEmail(IPT_PRIMARY_CONTACT_EMAIL);
    contact.setType(ContactType.TECHNICAL_POINT_OF_CONTACT);
    installation.setContacts(Collections.singletonList(contact));

    Endpoint endpoint = new Endpoint();
    endpoint.setUrl(IPT_SERVICE_URL);
    endpoint.setType(EndpointType.FEED);
    installation.setEndpoints(Collections.singletonList(endpoint));

    return installation;
  }
}
