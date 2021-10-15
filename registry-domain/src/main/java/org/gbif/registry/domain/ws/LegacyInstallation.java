/*
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
package org.gbif.registry.domain.ws;

import org.gbif.api.annotation.ParamName;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.registry.domain.ws.util.LegacyResourceConstants;

import java.net.URI;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Class used to create or update an Installation for legacy (GBRDS/IPT) API. A set of HTTP Form
 * parameters coming from a POST request are injected. </br> Its fields are injected using
 * the @ParamName. It is assumed the following parameters exist in the HTTP request:
 * 'organisationKey', 'name', 'description', 'primaryContactName', 'primaryContactEmail',
 * 'primaryContactType', 'serviceTypes', 'serviceURLs', and 'wsPassword'. </br> JAXB annotations
 * allow the class to be converted into an XML document, that gets included in the Response
 * following a successful registration or update. @XmlElement is used to specify element names that
 * consumers of legacy services expect to find.
 */
@XmlRootElement(name = "IptInstallation")
public class LegacyInstallation extends Installation implements LegacyEntity {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyInstallation.class);

  // injected from HTTP form parameters
  private ContactType primaryContactType;
  private String primaryContactEmail;
  private String primaryContactName;
  private EndpointType endpointType;
  private String endpointUrl;

  // created from combination of fields after injection
  private Contact primaryContact;
  private Endpoint feedEndpoint;

  // IPT constants
  private static final String RSS_ENDPOINT_TYPE = "RSS";

  /** Default constructor. */
  public LegacyInstallation() {
    setType(InstallationType.IPT_INSTALLATION);
  }

  /**
   * Set the hosting organization key. Mandatory field, injected on both register and update
   * requests.
   *
   * @param organizationKey organization key as UUID
   */
  @ParamName(LegacyResourceConstants.ORGANIZATION_KEY_PARAM)
  public void setHostingOrganizationKey(String organizationKey) {
    try {
      setOrganizationKey(UUID.fromString(Strings.nullToEmpty(organizationKey)));
    } catch (IllegalArgumentException e) {
      LOG.error(
          "Hosting organization key is not a valid UUID: {}", Strings.nullToEmpty(organizationKey));
    }
  }

  @XmlElement(name = LegacyResourceConstants.ORGANIZATION_KEY_PARAM)
  @NotNull
  public String getHostingOrganizationKey() {
    return getOrganizationKey() != null ? getOrganizationKey().toString() : null;
  }

  @XmlElement(name = LegacyResourceConstants.KEY_PARAM)
  @Nullable
  public String getIptInstallationKey() {
    return getKey() != null ? getKey().toString() : null;
  }

  /**
   * Set the title. </br> The title must be at least 2 characters long, a limit set in the database
   * schema. Since older versions of the IPT may not have imposed the same limit, the field is
   * padded if necessary so as to avoid problems during persistence.
   *
   * @param name title of the installation
   */
  @ParamName(LegacyResourceConstants.NAME_PARAM)
  public void setIptName(String name) {
    setTitle(validateField(name, 2));
  }

  /**
   * Get the title of the installation. This method is not used but it is needed otherwise this
   * Object can't be converted into an XML document via JAXB.
   *
   * @return title of the installation
   */
  @XmlTransient
  @Nullable
  public String getIptName() {
    return getTitle();
  }

  /**
   * Set the description. </br> The description must be at least 10 characters long, a limit set in
   * the database schema. Since older versions of the IPT may not have imposed the same limit, the
   * field is padded if necessary so as to avoid problems during persistence.
   *
   * @param description of the installation
   */
  @ParamName(LegacyResourceConstants.DESCRIPTION_PARAM)
  public void setIptDescription(String description) {
    setDescription(validateField(description, 10));
  }

  /**
   * Get the description of the installation. This method is not used but it is needed otherwise
   * this Object can't be converted into an XML document via JAXB.
   *
   * @return description of the installation
   */
  @XmlTransient
  @Nullable
  public String getIptDescription() {
    return getDescription();
  }

  /**
   * Get the endpoint type.
   *
   * @return the endpoint type
   */
  @XmlTransient
  @Nullable
  public EndpointType getEndpointType() {
    return endpointType;
  }

  /**
   * Set the endpoint type. IPT endpoint type RSS gets converted to type FEED.
   *
   * @param endpointType endpoint type
   */
  @ParamName(LegacyResourceConstants.SERVICE_TYPES_PARAM)
  public void setEndpointType(String endpointType) {
    this.endpointType =
        endpointType.equalsIgnoreCase(RSS_ENDPOINT_TYPE)
            ? EndpointType.FEED
            : EndpointType.fromString(endpointType);
  }

  /**
   * Get the endpoint URL.
   *
   * @return the endpoint URL
   */
  @XmlTransient
  @Nullable
  public String getEndpointUrl() {
    return endpointUrl;
  }

  /**
   * Set the endpoint URL.
   *
   * @param endpointUrl endpoint URL
   */
  @ParamName(LegacyResourceConstants.SERVICE_URLS_PARAM)
  public void setEndpointUrl(String endpointUrl) {
    this.endpointUrl = endpointUrl;
  }

  @XmlTransient
  @Nullable
  public String getWsPassword() {
    return getPassword();
  }

  @ParamName(LegacyResourceConstants.WS_PASSWORD_PARAM)
  public void setWsPassword(String wsPassword) {
    setPassword(Strings.nullToEmpty(wsPassword));
  }

  /**
   * Get primary contact name.
   *
   * @return primary contact name
   */
  @XmlTransient
  @Nullable
  public String getPrimaryContactName() {
    return primaryContactName;
  }

  /**
   * Set primary contact name. Note: this is not a required field.
   *
   * @param primaryContactName primary contact name
   */
  @ParamName(LegacyResourceConstants.PRIMARY_CONTACT_NAME_PARAM)
  public void setPrimaryContactName(String primaryContactName) {
    this.primaryContactName = primaryContactName;
  }

  /**
   * Get primary contact email.
   *
   * @return primary contact email
   */
  @XmlTransient
  @NotNull
  public String getPrimaryContactEmail() {
    return primaryContactEmail;
  }

  /**
   * Set primary contact email and check if it is a valid email address. Note: this field is
   * required, and the old web services would throw 400 response if not valid. TODO: once field
   * validation is working, the validation below can be removed
   *
   * @param primaryContactEmail primary contact email address
   */
  @ParamName(LegacyResourceConstants.PRIMARY_CONTACT_EMAIL_PARAM)
  public void setPrimaryContactEmail(String primaryContactEmail) {
    EmailValidator validator = EmailValidator.getInstance();
    if (validator.isValid(primaryContactEmail)) {
      this.primaryContactEmail = primaryContactEmail;
    } else {
      LOG.error("No valid primary contact email has been specified: {}", primaryContactEmail);
    }
  }

  /**
   * Get primary contact type.
   *
   * @return primary contact type
   */
  @XmlTransient
  @NotNull
  public ContactType getPrimaryContactType() {
    return primaryContactType;
  }

  /**
   * Set primary contact type. First, check if it is not null or empty. The incoming type is always
   * either administrative or technical, always defaulting to type technical. Note: this field is
   * required, and the old web services would throw 400 response if not found.
   *
   * @param primaryContactType primary contact type
   */
  @ParamName(LegacyResourceConstants.PRIMARY_CONTACT_TYPE_PARAM)
  public void setPrimaryContactType(String primaryContactType) {
    if (Strings.nullToEmpty(primaryContactType)
        .equalsIgnoreCase(LegacyResourceConstants.ADMINISTRATIVE_CONTACT_TYPE)) {
      this.primaryContactType = ContactType.ADMINISTRATIVE_POINT_OF_CONTACT;
    } else if (Strings.nullToEmpty(primaryContactType)
        .equalsIgnoreCase(LegacyResourceConstants.TECHNICAL_CONTACT_TYPE)) {
      this.primaryContactType = ContactType.TECHNICAL_POINT_OF_CONTACT;
    } else if (Strings.isNullOrEmpty(primaryContactType)) {
      LOG.error("No primary contact type has ben provided");
    }
  }

  /**
   * Get the endpoint of type FEED.
   *
   * @return the endpoint of type FEED
   */
  @XmlTransient
  public Endpoint getFeedEndpoint() {
    return feedEndpoint;
  }

  /**
   * Set the endpoint of type FEED. This endpoint will have been created via addEndpoint() that
   * creates the endpoint from the injected HTTP Form parameters.
   *
   * @param feedEndpoint endpoint of type FEED
   */
  public void setFeedEndpoint(Endpoint feedEndpoint) {
    this.feedEndpoint = feedEndpoint;
  }

  /**
   * Get the primary contact.
   *
   * @return the primary contact
   */
  @XmlTransient
  @Nullable
  public Contact getPrimaryContact() {
    return primaryContact != null
            && primaryContact.getEmail() != null
            && primaryContact.getType() != null
        ? primaryContact
        : null;
  }

  /**
   * Set the primary contact. This contact will have been created via addContact() that creates the
   * contact from the injected HTTP Form parameters.
   *
   * @param primaryContact primary contact
   */
  public void setPrimaryContact(Contact primaryContact) {
    this.primaryContact = primaryContact;
  }

  /**
   * Prepares the installation for being persisting, ensuring the primary contact and endpoint have
   * been constructed from the injected HTTP parameters.
   */
  public void prepare() {
    addPrimaryContact();
    addEndpoint();
  }

  /**
   * Generates the primary technical contact, and adds it to the installation. This method must be
   * called after all primary contact parameters have been set.
   */
  private void addPrimaryContact() {
    if (!Strings.isNullOrEmpty(primaryContactEmail) && primaryContactType != null) {

      // check if the primary contact with this type exists already
      Contact contact = null;
      for (Contact c : getContacts()) {
        if (c.isPrimary() && c.getType() == primaryContactType) {
          contact = c;
          break;
        }
      }
      // if it doesn't exist already, create it
      if (contact == null) {
        contact = new Contact();
        contact.setPrimary(true);
        contact.setType(primaryContactType);
      }
      // set/update other properties
      contact.setFirstName(primaryContactName);
      contact.setEmail(Lists.newArrayList(primaryContactEmail));

      primaryContact = contact;
    }
  }

  /**
   * Generates an Endpoint, and adds it to the installation. This method must be called after all
   * endpoint related parameters have been set.
   */
  private void addEndpoint() {
    if (!Strings.isNullOrEmpty(endpointUrl) && endpointType != null) {
      // check if the endpoint with type FEED exists already
      Endpoint endpoint = null;
      for (Endpoint e : getEndpoints()) {
        if (e.getType() == EndpointType.FEED) {
          endpoint = e;
          break;
        }
      }
      // if it doesn't exist already, create it
      if (endpoint == null) {
        endpoint = new Endpoint();
        endpoint.setType(endpointType);
      }
      endpoint.setUrl(URI.create(endpointUrl));
      feedEndpoint = endpoint;
    }
  }

  /**
   * Return a new GBIF API Installation instance, derived from the LegacyInstallation. Needed
   * because of: http://dev.gbif.org/issues/browse/POR-97
   *
   * @return Installation derived from LegacyInstallation
   */
  public Installation toApiInstallation() {
    Installation installation = new Installation();
    installation.setKey(getKey());
    installation.setCreatedBy(getCreatedBy());
    installation.setModifiedBy(getModifiedBy());
    installation.setTitle(getTitle());
    installation.setOrganizationKey(getOrganizationKey());
    installation.setPassword(getPassword());
    installation.setType(getType());
    installation.setDescription(getDescription());

    return installation;
  }
}
