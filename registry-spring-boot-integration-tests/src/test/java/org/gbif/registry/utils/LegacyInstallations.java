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
  private static final String IPT_NAME = "Test IPT Registry2";
  private static final String IPT_DESCRIPTION = "Description of Test IPT";
  private static final String IPT_PRIMARY_CONTACT_TYPE = "technical";
  private static final String IPT_PRIMARY_CONTACT_NAME = "Kyle Braak";
  private static final List<String> IPT_PRIMARY_CONTACT_EMAIL = Lists.newArrayList("kbraak@gbif.org");
  private static final String IPT_SERVICE_TYPE = "RSS";
  private static final URI IPT_SERVICE_URL = URI.create("http://ipt.gbif.org/rss.do");
  private static final String IPT_WS_PASSWORD = "welcome";

  private static final String DATASET_SERVICE_TYPES = "EML|DWC-ARCHIVE-OCCURRENCE";
  private static final String DATASET_EVENT_SERVICE_TYPES = "EML|DWC-ARCHIVE-SAMPLING-EVENT";
  private static final String DATASET_SERVICE_URLS =
    "http://ipt.gbif.org/eml.do?r=ds123|http://ipt.gbif.org/archive.do?r=ds123";
  private static final URI DATASET_EML_SERVICE_URL = URI.create("http://ipt.gbif.org/eml.do?r=ds123");
  private static final URI DATASET_OCCURRENCE_SERVICE_URL = URI.create("http://ipt.gbif.org/archive.do?r=ds123");

  /**
   * Populate a list of name value pairs used in the common ws requests for IPT registrations and updates.
   * </br>
   * Basically a copy of the method in the IPT, to ensure the parameter names are identical.
   *
   * @param organizationKey organization key (UUID)
   * @return list of name value pairs, or an empty list if the IPT or organisation key were null
   */
  public static HttpHeaders buildParams(UUID organizationKey) {
    HttpHeaders requestParams = new HttpHeaders();
    // main
    requestParams.put(LegacyResourceConstants.ORGANIZATION_KEY_PARAM, Collections.singletonList(organizationKey.toString()));
    requestParams.put(LegacyResourceConstants.NAME_PARAM, Collections.singletonList(IPT_NAME));
    requestParams.put(LegacyResourceConstants.DESCRIPTION_PARAM, Collections.singletonList(IPT_DESCRIPTION));

    // primary contact
    requestParams.put(LegacyResourceConstants.PRIMARY_CONTACT_TYPE_PARAM, Collections.singletonList(IPT_PRIMARY_CONTACT_TYPE));
    requestParams.put(LegacyResourceConstants.PRIMARY_CONTACT_NAME_PARAM, Collections.singletonList(IPT_PRIMARY_CONTACT_NAME));
    requestParams.put(LegacyResourceConstants.PRIMARY_CONTACT_EMAIL_PARAM, IPT_PRIMARY_CONTACT_EMAIL);

    // service/endpoint
    requestParams.put(LegacyResourceConstants.SERVICE_TYPES_PARAM, Collections.singletonList(IPT_SERVICE_TYPE));
    requestParams.put(LegacyResourceConstants.SERVICE_URLS_PARAM, Collections.singletonList(IPT_SERVICE_URL.toASCIIString()));

    // add IPT password used for updating the IPT's own metadata & issuing atomic updateURL operations
    requestParams.put(LegacyResourceConstants.WS_PASSWORD_PARAM, Collections.singletonList(IPT_WS_PASSWORD));

    return requestParams;
  }

  /**
   * Populate a list of name value pairs used in the common ws requests for IPT dataset registrations and updates.
   * </br>
   * Basically a copy of the method in the IPT, to ensure the parameter names are identical.
   *
   * @param installationKey installation key
   * @return list of name value pairs, or an empty list if the dataset or organisation key were null
   */
  public static HttpHeaders buildDatasetParams(UUID organizationKey, UUID installationKey) {
    HttpHeaders requestParams = new HttpHeaders();
    // main
    requestParams.put(LegacyResourceConstants.ORGANIZATION_KEY_PARAM, Collections.singletonList(organizationKey.toString()));
    requestParams.put(LegacyResourceConstants.NAME_PARAM, Collections.singletonList(Requests.DATASET_NAME));
    requestParams.put(LegacyResourceConstants.DESCRIPTION_PARAM, Collections.singletonList(Requests.DATASET_DESCRIPTION));
    requestParams.put(LegacyResourceConstants.HOMEPAGE_URL_PARAM, Collections.singletonList(Requests.DATASET_HOMEPAGE_URL));
    requestParams.put(LegacyResourceConstants.LOGO_URL_PARAM, Collections.singletonList(Requests.DATASET_LOGO_URL));

    // primary contact
    requestParams.put(LegacyResourceConstants.PRIMARY_CONTACT_TYPE_PARAM,
      Collections.singletonList(Requests.DATASET_PRIMARY_CONTACT_TYPE));
    requestParams.put(LegacyResourceConstants.PRIMARY_CONTACT_EMAIL_PARAM,
      Requests.DATASET_PRIMARY_CONTACT_EMAIL);
    requestParams.put(LegacyResourceConstants.PRIMARY_CONTACT_NAME_PARAM,
      Collections.singletonList(Requests.DATASET_PRIMARY_CONTACT_NAME));
    requestParams.put(LegacyResourceConstants.PRIMARY_CONTACT_ADDRESS_PARAM,
      Requests.DATASET_PRIMARY_CONTACT_ADDRESS);
    requestParams.put(LegacyResourceConstants.PRIMARY_CONTACT_PHONE_PARAM,
      Requests.DATASET_PRIMARY_CONTACT_PHONE);

    // endpoint(s)
    requestParams.put(LegacyResourceConstants.SERVICE_TYPES_PARAM, Collections.singletonList(DATASET_SERVICE_TYPES));
    requestParams.put(LegacyResourceConstants.SERVICE_URLS_PARAM, Collections.singletonList(DATASET_SERVICE_URLS));

    // add additional ipt and organisation parameters
    requestParams.put(LegacyResourceConstants.IPT_KEY_PARAM, Collections.singletonList(installationKey.toString()));

    return requestParams;
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
