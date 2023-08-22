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

import org.gbif.api.annotation.Generated;
import org.gbif.api.annotation.ParamName;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.DatasetSubtype;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.api.vocabulary.Language;
import org.gbif.registry.domain.ws.util.LegacyResourceConstants;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * Class used to create or update a Dataset for legacy (GBRDS/IPT) API. Previously known as a
 * Resource in the GBRDS. A set of HTTP Form parameters coming from a POST request are injected.
 * </br> Its fields are injected using the @ParamName. It is assumed the following parameters exist
 * in the HTTP request: 'organisationKey', 'name', 'description', 'primaryContactName',
 * 'primaryContactEmail', 'primaryContactType', 'serviceTypes', 'serviceURLs', 'homepageURL',
 * 'primaryContactPhone', 'logoURL', and 'primaryContactAddress'. </br> JAXB annotations allow the
 * class to be converted into an XML document, that gets included in the Response following a
 * successful registration or update. @XmlElement is used to specify element names that consumers of
 * legacy services expect to find.
 */
@XmlRootElement(name = "resource")
public class LegacyDataset extends Dataset implements LegacyEntity {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyDataset.class);

  // injected from HTTP form parameters
  private ContactType primaryContactType;
  private String primaryContactEmail;
  private String primaryContactName;
  private String primaryContactPhone;
  private String primaryContactAddress;
  private String primaryContactDescription;
  private String serviceTypes;
  private String serviceUrls;
  private DOI datasetDoi;
  private String rawSubtype;

  // created from combination of fields after injection
  private Contact primaryContact;
  private Endpoint emlEndpoint;
  private Endpoint archiveEndpoint;
  private Endpoint dataPackageEndpoint;

  // Dataset constants
  private static final String IPT_EML_SERVICE_TYPE = "EML";
  private static final Set<String> ARCHIVE_ENDPOINT_TYPE_ALTERNATIVES =
      ImmutableSet.of(
          LegacyResourceConstants.CHECKLIST_SERVICE_TYPE_1,
          LegacyResourceConstants.CHECKLIST_SERVICE_TYPE_2,
          LegacyResourceConstants.OCCURRENCE_SERVICE_TYPE_1,
          LegacyResourceConstants.OCCURRENCE_SERVICE_TYPE_2,
          LegacyResourceConstants.MATERIAL_ENTITY_SERVICE_TYPE_1,
          LegacyResourceConstants.MATERIAL_ENTITY_SERVICE_TYPE_2,
          LegacyResourceConstants.SAMPLING_EVENT_SERVICE_TYPE);
  private static final Set<String> DATA_PACKAGE_ENDPOINT_TYPE_ALTERNATIVES =
      ImmutableSet.of(
          EndpointType.CAMTRAP_DP_v_0_4.name(), LegacyResourceConstants.CAMTRAP_DP_SERVICE_TYPE);

  /** Default constructor. */
  public LegacyDataset() {
    // fill in mandatory field language with default value
    setLanguage(Language.ENGLISH);
  }

  /**
   * Get the dataset key. This method is not used but it is needed otherwise this Object can't be
   * converted into an XML document via JAXB.
   *
   * @return key of the dataset
   */
  @XmlElement(name = LegacyResourceConstants.KEY_PARAM)
  @Nullable
  public String getDatasetKey() {
    return getKey() == null ? null : getKey().toString();
  }

  /**
   * Get the dataset Doi. This method is not used but it is needed otherwise this Object can't be
   * converted into an XML document via JAXB.
   *
   * @return DOI of the dataset
   */
  @XmlElement(name = LegacyResourceConstants.DOI_PARAM)
  @Nullable
  public String getDatasetDoi() {
    return datasetDoi == null ? null : datasetDoi.toString();
  }

  /**
   * Set the publishing organization key, a mandatory field. Be aware the publishing organisation
   * key gets supplied in the parameters only on register requests. On update requests the key is
   * supplied in the credentials.
   *
   * @param organizationKey organization key as UUID
   */
  @ParamName(LegacyResourceConstants.ORGANIZATION_KEY_PARAM)
  public void setOrganizationKey(String organizationKey) {
    try {
      setPublishingOrganizationKey(UUID.fromString(Strings.nullToEmpty(organizationKey)));
    } catch (IllegalArgumentException e) {
      LOG.debug("Publishing organization key is not a valid UUID: {}", organizationKey);
    }
  }

  @XmlTransient
  @Nullable
  public String getRawSubtype() {
    return rawSubtype;
  }

  @ParamName(LegacyResourceConstants.SUBTYPE_PARAM)
  public void setRawSubtype(String rawSubtype) {
    this.rawSubtype = rawSubtype;
  }

  /**
   * Get the publishing organization key. This is a required field in Registry2, and is required by
   * the web services. This method is not used but it is needed otherwise this Object can't be
   * converted into an XML document via JAXB.
   *
   * @return publishing organization key of the dataset
   */
  @XmlElement(name = LegacyResourceConstants.ORGANIZATION_KEY_PARAM)
  @NotNull
  public String getOrganizationKey() {
    return getPublishingOrganizationKey().toString();
  }

  /**
   * Set the IPT key. Non-mandatory field because it has only been introduced as of IPT v2.0.5. It
   * is injected on both register and update requests.
   *
   * @param iptKey IPT key as UUID
   */
  @ParamName(LegacyResourceConstants.IPT_KEY_PARAM)
  public void setIptKey(String iptKey) {

    try {
      setInstallationKey(UUID.fromString(Strings.nullToEmpty(iptKey)));
    } catch (IllegalArgumentException e) {
      LOG.error("IPT key is not a valid UUID: {}", Strings.nullToEmpty(iptKey));
    }
  }

  /**
   * Set the Dataset DOI. Non-mandatory field because it has only been introduced as of IPT v2.2. It
   * is injected on both register and update requests.
   *
   * @param doi The DOI
   */
  @ParamName(LegacyResourceConstants.DOI_PARAM)
  public void setDatasetDoi(String doi) {
    try {
      if (doi != null) {
        this.datasetDoi = new DOI(doi);
      }
    } catch (IllegalArgumentException e) {
      LOG.error("DOI is not valid: {}", Strings.nullToEmpty(doi));
    }
  }

  /**
   * Get the key of the IPT installation. This method is not used but it is needed otherwise this
   * Object can't be converted into an XML document via JAXB.
   *
   * @return key of the dataset's IPT installation
   */
  @XmlTransient
  @Nullable
  public String getIptKey() {
    return getInstallationKey().toString();
  }

  /**
   * Set the title of the dataset.
   *
   * @param name title of the dataset
   */
  @ParamName(LegacyResourceConstants.NAME_PARAM)
  public void setDatasetName(String name) {
    setTitle(validateField(name, 2));
  }

  /**
   * Get the title of the dataset. This is a required field in Registry2, and is required by the web
   * services. This method is not used but it is needed otherwise this Object can't be converted
   * into an XML document via JAXB.
   *
   * @return title of the dataset
   */
  @XmlElement(name = LegacyResourceConstants.NAME_PARAM)
  @NotNull
  public String getDatasetName() {
    return getTitle();
  }

  /**
   * Set the language of the name of the dataset as the dataset langauge. 2 letter ISO 639-1
   * language code expected.
   *
   * @param nameLanguage language of the name of the dataset (ISO 639-1 2 letter language code)
   */
  @ParamName(LegacyResourceConstants.NAME_LANGUAGE_PARAM)
  public void setDatasetNameLanguage(String nameLanguage) {
    if (Strings.emptyToNull(nameLanguage) != null) {
      if (nameLanguage.length() == 2) {
        setLanguage(Language.fromIsoCode(nameLanguage));
      } else {
        LOG.error("Incoming parameter (nameLanguage) should be a 2-letter ISO 639-1 language code");
      }
    }
  }

  /**
   * Get the language of the dataset name. This is a required field in Registry2, and will be set
   * either in the default constructor, or via the form parameter "nameLanguage". This method is not
   * used but it is needed otherwise this Object can't be converted into an XML document via JAXB.
   *
   * @return language of the dataset name
   */
  @XmlElement(name = LegacyResourceConstants.NAME_LANGUAGE_PARAM)
  @NotNull
  public String getDatasetNameLanguage() {
    return getLanguage().getIso2LetterCode();
  }

  /**
   * Set the dataset description.
   *
   * @param description of the dataset
   */
  @ParamName(LegacyResourceConstants.DESCRIPTION_PARAM)
  public void setDatasetDescription(String description) {
    setDescription(validateField(description, 10));
  }

  /**
   * Get the dataset description. This method is not used but it is needed otherwise this Object
   * can't be converted into an XML document via JAXB.
   *
   * @return description of the dataset
   */
  @XmlElement(name = LegacyResourceConstants.DESCRIPTION_PARAM)
  @Nullable
  public String getDatasetDescription() {
    return getDescription();
  }

  /**
   * Get the language of the dataset description. There is no description language field in
   * Registry2, so this method defaults to returning the dataset language instead. This method is
   * not used but it is needed otherwise this Object can't be converted into an XML document via
   * JAXB.
   *
   * @return language of the dataset description
   */
  @XmlElement(name = LegacyResourceConstants.DESCRIPTION_LANGUAGE_PARAM)
  @NotNull
  public String getDatasetDescriptionLanguage() {
    return getLanguage().getIso2LetterCode();
  }

  /**
   * Set logo URL.
   *
   * @param logoUrl logo URL
   */
  @ParamName(LegacyResourceConstants.LOGO_URL_PARAM)
  public void setDatasetLogoUrl(String logoUrl) {
    if (!Strings.isNullOrEmpty(logoUrl)) {
      try {
        URI uri = new URI(logoUrl);
        setLogoUrl(uri);
      } catch (URISyntaxException e) {
        LOG.warn("Dataset logoURL was invalid: {}", Strings.nullToEmpty(logoUrl));
      }
    }
  }

  /**
   * Get logo URL. This method is not used but it is needed otherwise this Object can't be converted
   * into an XML document via JAXB.
   *
   * @return logo URL
   */
  @XmlElement(name = LegacyResourceConstants.LOGO_URL_PARAM)
  @Nullable
  public String getDatasetLogoUrl() {
    return getLogoUrl() == null ? null : getLogoUrl().toString();
  }

  /**
   * Set homepage URL.
   *
   * @param homepageUrl homepage URL
   */
  @ParamName(LegacyResourceConstants.HOMEPAGE_URL_PARAM)
  public void setDatasetHomepageUrl(String homepageUrl) {
    if (!Strings.isNullOrEmpty(homepageUrl)) {
      try {
        URI uri = new URI(homepageUrl);
        setHomepage(uri);
      } catch (URISyntaxException e) {
        LOG.warn("Dataset homepageURL was invalid: {}", Strings.nullToEmpty(homepageUrl));
      }
    }
  }

  /**
   * Get homepage URL. This method is not used but it is needed otherwise this Object can't be
   * converted into an XML document via JAXB.
   *
   * @return homepage URL
   */
  @XmlElement(name = LegacyResourceConstants.HOMEPAGE_URL_PARAM)
  @Nullable
  public String getDatasetHomepageUrl() {
    return getHomepage() == null ? null : getHomepage().toString();
  }

  /**
   * Set primary contact name. Note: this is not a required field.
   *
   * @param primaryContactName primary contact name
   */
  @ParamName(LegacyResourceConstants.PRIMARY_CONTACT_NAME_PARAM)
  public void setPrimaryContactName(String primaryContactName) {
    this.primaryContactName = Strings.emptyToNull(primaryContactName);
  }

  /**
   * Get primary contact name. This method is not used but it is needed otherwise this Object can't
   * be converted into an XML document via JAXB.
   *
   * @return primary contact name
   */
  @XmlElement(name = LegacyResourceConstants.PRIMARY_CONTACT_NAME_PARAM)
  @Nullable
  public String getPrimaryContactName() {
    return primaryContactName;
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
    if (validator.isValid(Strings.nullToEmpty(primaryContactEmail))) {
      this.primaryContactEmail = primaryContactEmail;
    } else {
      LOG.error("No valid primary contact email has been specified: {}", primaryContactEmail);
    }
  }

  /**
   * Get primary contact email. This method is not used but it is needed otherwise this Object can't
   * be converted into an XML document via JAXB.
   *
   * @return primary contact email
   */
  @XmlElement(name = LegacyResourceConstants.PRIMARY_CONTACT_EMAIL_PARAM)
  @NotNull
  public String getPrimaryContactEmail() {
    return primaryContactEmail;
  }

  /**
   * Set primary contact type. First, check if it is not null or empty. The incoming type is always
   * either administrative or technical. Note: this field is required, and the old web services
   * would throw 400 response if not found.
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
   * Get primary contact type. This method is not used but it is needed otherwise this Object can't
   * be converted into an XML document via JAXB.
   *
   * @return primary contact type
   */
  @XmlElement(name = LegacyResourceConstants.PRIMARY_CONTACT_TYPE_PARAM)
  @NotNull
  public ContactType getPrimaryContactType() {
    return primaryContactType;
  }

  /**
   * Set primary contact phone.
   *
   * @param primaryContactPhone primary contact type
   */
  @ParamName(LegacyResourceConstants.PRIMARY_CONTACT_PHONE_PARAM)
  public void setPrimaryContactPhone(String primaryContactPhone) {
    this.primaryContactPhone = Strings.emptyToNull(primaryContactPhone);
  }

  /**
   * Get primary contact phone. This method is not used but it is needed otherwise this Object can't
   * be converted into an XML document via JAXB.
   *
   * @return primary contact phone
   */
  @XmlElement(name = LegacyResourceConstants.PRIMARY_CONTACT_PHONE_PARAM)
  @Nullable
  public String getPrimaryContactPhone() {
    return primaryContactPhone;
  }

  /**
   * Set primary contact address.
   *
   * @param primaryContactAddress primary contact address
   */
  @ParamName(LegacyResourceConstants.PRIMARY_CONTACT_ADDRESS_PARAM)
  public void setPrimaryContactAddress(String primaryContactAddress) {
    this.primaryContactAddress = Strings.emptyToNull(primaryContactAddress);
  }

  /**
   * Get primary contact address. This method is not used but it is needed otherwise this Object
   * can't be converted into an XML document via JAXB.
   *
   * @return primary contact address
   */
  @XmlElement(name = LegacyResourceConstants.PRIMARY_CONTACT_ADDRESS_PARAM)
  @Nullable
  public String getPrimaryContactAddress() {
    return primaryContactAddress;
  }

  /**
   * Set primary contact description.
   *
   * @param primaryContactDescription primary contact type
   */
  @ParamName(LegacyResourceConstants.PRIMARY_CONTACT_DESCRIPTION_PARAM)
  public void setPrimaryContactDescription(String primaryContactDescription) {
    this.primaryContactDescription = Strings.emptyToNull(primaryContactDescription);
  }

  /**
   * Get primary contact description. This method is not used but it is needed otherwise this Object
   * can't be converted into an XML document via JAXB.
   *
   * @return primary contact phone
   */
  @XmlElement(name = LegacyResourceConstants.PRIMARY_CONTACT_DESCRIPTION_PARAM)
  @Nullable
  public String getPrimaryContactDescription() {
    return primaryContactDescription;
  }

  /**
   * Get the serviceTypes.
   *
   * @return the serviceTypes
   */
  @XmlTransient
  @Nullable
  public String getServiceTypes() {
    return serviceTypes;
  }

  /**
   * Set the serviceTypes, a concatenated list of service types.
   *
   * @param serviceTypes serviceTypes
   */
  @ParamName(value = LegacyResourceConstants.SERVICE_TYPES_PARAM)
  public void setServiceTypes(String serviceTypes) {
    this.serviceTypes = Strings.emptyToNull(serviceTypes);
  }

  /**
   * Get the serviceUrls.
   *
   * @return the serviceUrls
   */
  @XmlTransient
  @Nullable
  public String getServiceUrls() {
    return serviceUrls;
  }

  /**
   * Set the serviceUrls, a concatenated list of service URLs.
   *
   * @param serviceUrls serviceUrls
   */
  @ParamName(LegacyResourceConstants.SERVICE_URLS_PARAM)
  public void setServiceUrls(String serviceUrls) {
    this.serviceUrls = Strings.emptyToNull(serviceUrls);
  }

  /**
   * Get the endpoint of type EML.
   *
   * @return the endpoint of type EML
   */
  @XmlTransient
  public Endpoint getEmlEndpoint() {
    return emlEndpoint;
  }

  /**
   * Set the endpoint of type EML. This endpoint will have been created via addEndpoint() that
   * creates the endpoint from the injected HTTP Form parameters.
   *
   * @param emlEndpoint endpoint of type EML
   */
  public void setEmlEndpoint(Endpoint emlEndpoint) {
    this.emlEndpoint = emlEndpoint;
  }

  /**
   * Get the endpoint of type ARCHIVE.
   *
   * @return the endpoint of type ARCHIVE
   */
  @XmlTransient
  public Endpoint getArchiveEndpoint() {
    return archiveEndpoint;
  }

  /**
   * Get the endpoint of type DATA PACKAGE.
   *
   * @return the endpoint of type DATA PACKAGE
   */
  @XmlTransient
  public Endpoint getDataPackageEndpoint() {
    return dataPackageEndpoint;
  }

  /**
   * Set the endpoint of type ARCHIVE. This endpoint will have been created via addEndpoint() that
   * creates the endpoint from the injected HTTP Form parameters.
   *
   * @param archiveEndpoint endpoint of type ARCHIVE
   */
  public void setArchiveEndpoint(Endpoint archiveEndpoint) {
    this.archiveEndpoint = archiveEndpoint;
  }

  public void setDataPackageEndpoint(Endpoint dataPackageEndpoint) {
    this.dataPackageEndpoint = dataPackageEndpoint;
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

  /** Excludes element from XML document created via JAXB. */
  @XmlTransient
  @Nullable
  @Override
  public Citation getCitation() {
    return null;
  }

  /**
   * Prepares the dataset for being persisted, ensuring the primary contact, endpoints, and type
   * have been constructed from the injected HTTP parameters.
   */
  public void prepare() {
    addPrimaryContact();
    addEmlEndpoint();
    addArchiveEndpoint();
    addDataPackageEndpoint();
    setType(resolveType());
    setSubtype(resolveSubtype());
  }

  /**
   * Generates the primary contact, and adds it to the dataset. This method must be called after all
   * primary contact parameters have been set.
   *
   * @return new primary contact added
   */
  private Contact addPrimaryContact() {
    Contact contact = null;
    if (!Strings.isNullOrEmpty(primaryContactEmail) && primaryContactType != null) {

      // check if the primary contact with this type exists already
      for (Contact c : getContacts()) {
        if (c.isPrimary() && c.getType() == primaryContactType) {
          contact = c;
          break;
        }
      }
      // Only if it doesn't exist already, create it and populate it with the incoming contact
      // parameters.
      // If it does exist already, don't update it! This was the cause of
      // http://dev.gbif.org/issues/browse/POR-2733
      // Modifications to existing primary contacts should only happen via a) metadata sync or via
      // b) registry console
      if (contact == null) {
        contact = new Contact();
        contact.setPrimary(true);
        contact.setType(primaryContactType);
        contact.setFirstName(primaryContactName);
        contact.setEmail(Lists.newArrayList(primaryContactEmail));
        contact.setPhone(Lists.newArrayList(primaryContactPhone));
        contact.setAddress(Lists.newArrayList(primaryContactAddress));
        contact.setDescription(primaryContactDescription);
      }
      primaryContact = contact;
    }
    return contact;
  }

  /**
   * Generates an Endpoint of type EML, and adds it to the dataset. This method must be called after
   * all endpoint related parameters have been set.
   */
  private void addEmlEndpoint() {
    if (!Strings.isNullOrEmpty(serviceUrls) && !Strings.isNullOrEmpty(serviceTypes)) {
      // create tokenizers
      StringTokenizer serviceTypesTokenizer = new StringTokenizer(serviceTypes, "|");
      StringTokenizer serviceUrlsTokenizer = new StringTokenizer(serviceUrls, "|");
      // find EML service
      while (serviceTypesTokenizer.hasMoreTokens() && serviceUrlsTokenizer.hasMoreTokens()) {
        String type = serviceTypesTokenizer.nextToken();
        String url = serviceUrlsTokenizer.nextToken();
        if (type != null && url != null && type.equals(IPT_EML_SERVICE_TYPE)) {
          // create endpoint
          Endpoint endpoint = createEndpoint(url, EndpointType.EML);
          if (endpoint != null) {
            // set it
            emlEndpoint = endpoint;
          }
          // only 1 is expected
          break;
        }
      }
    }
  }

  /**
   * Generates an Endpoint of type ARCHIVE, and adds it to the dataset. This method must be called
   * after all endpoint related parameters have been set.
   */
  private void addArchiveEndpoint() {
    if (!Strings.isNullOrEmpty(serviceUrls) && !Strings.isNullOrEmpty(serviceTypes)) {
      // create tokenizers
      StringTokenizer serviceTypesTokenizer = new StringTokenizer(serviceTypes, "|");
      StringTokenizer serviceUrlsTokenizer = new StringTokenizer(serviceUrls, "|");
      // find EML service
      while (serviceTypesTokenizer.hasMoreTokens() && serviceUrlsTokenizer.hasMoreTokens()) {
        String type = serviceTypesTokenizer.nextToken();
        String url = serviceUrlsTokenizer.nextToken();
        if (type != null && url != null && ARCHIVE_ENDPOINT_TYPE_ALTERNATIVES.contains(type)) {
          // create endpoint
          Endpoint endpoint = createEndpoint(url, EndpointType.DWC_ARCHIVE);
          if (endpoint != null) {
            // set it
            archiveEndpoint = endpoint;
          }
          // only 1 is expected
          break;
        }
      }
    }
  }

  /**
   * Generates an Endpoint of data package type (CAMTRAP_DP), and adds it to the dataset. This method must be called
   * after all endpoint related parameters have been set.
   */
  private void addDataPackageEndpoint() {
    if (!Strings.isNullOrEmpty(serviceUrls) && !Strings.isNullOrEmpty(serviceTypes)) {
      // create tokenizers
      StringTokenizer serviceTypesTokenizer = new StringTokenizer(serviceTypes, "|");
      StringTokenizer serviceUrlsTokenizer = new StringTokenizer(serviceUrls, "|");

      while (serviceTypesTokenizer.hasMoreTokens() && serviceUrlsTokenizer.hasMoreTokens()) {
        String type = serviceTypesTokenizer.nextToken();
        String url = serviceUrlsTokenizer.nextToken();
        if (type != null && url != null && DATA_PACKAGE_ENDPOINT_TYPE_ALTERNATIVES.contains(type)) {
          // create endpoint
          Endpoint endpoint = createEndpoint(url, EndpointType.CAMTRAP_DP_v_0_4);
          if (endpoint != null) {
            // set it
            dataPackageEndpoint = endpoint;
          }
          // only 1 is expected
          break;
        }
      }
    }
  }

  /**
   * Creates a new Endpoint, or retrieves an existing Endpoint from the dataset. This method assumes
   * that the dataset only has 1 endpoint for each type: EML and DWC_ARCHIVE.
   *
   * @param url Endpoint URL
   * @param type Endpoint type
   * @return Endpoint created or updated
   */
  private Endpoint createEndpoint(String url, EndpointType type) {
    if (!Strings.isNullOrEmpty(url) && type != null) {
      // check if the endpoint with type exists already
      Endpoint endpoint = null;
      for (Endpoint e : getEndpoints()) {
        if (e.getType() == type) {
          endpoint = e;
          break;
        }
      }

      // if it doesn't exist already, create it
      if (endpoint == null) {
        endpoint = new Endpoint();
        endpoint.setType(type);
      }
      endpoint.setUrl(URI.create(url));
      return endpoint;
    }
    return null;
  }

  /**
   * Return the DatasetType from the Dataset's endpoints, defaulting to type METADATA if type
   * OCCURRENCE, MATERIAL_ENTITY, CHECKLIST or SAMPLING_EVENT could not be resolved.
   *
   * @return the DatasetType, defaulting to type METADATA if type OCCURRENCE, MATERIAL_ENTITY, CHECKLIST or
   *     SAMPLING_EVENT could not be resolved
   */
  public DatasetType resolveType() {
    if (serviceTypes != null) {
      if (serviceTypes.contains(LegacyResourceConstants.CHECKLIST_SERVICE_TYPE_1)
          || serviceTypes.contains(LegacyResourceConstants.CHECKLIST_SERVICE_TYPE_2)) {
        return DatasetType.CHECKLIST;
      } else if (serviceTypes.contains(LegacyResourceConstants.OCCURRENCE_SERVICE_TYPE_1)
          || serviceTypes.contains(LegacyResourceConstants.OCCURRENCE_SERVICE_TYPE_2)
          || serviceTypes.contains(LegacyResourceConstants.CAMTRAP_DP_SERVICE_TYPE)) {
        return DatasetType.OCCURRENCE;
      } else if (serviceTypes.contains(LegacyResourceConstants.MATERIAL_ENTITY_SERVICE_TYPE_1)
          || serviceTypes.contains(LegacyResourceConstants.MATERIAL_ENTITY_SERVICE_TYPE_2)) {
        return DatasetType.MATERIAL_ENTITY;
      } else if (serviceTypes.contains(LegacyResourceConstants.SAMPLING_EVENT_SERVICE_TYPE)) {
        return DatasetType.SAMPLING_EVENT;
      }
    }
    return DatasetType.METADATA;
  }

  /**
   * Return the DatasetSubtype from the string.
   */
  public DatasetSubtype resolveSubtype() {
    try {
      DatasetSubtype datasetSubtype = VocabularyUtils.lookupEnum(rawSubtype, DatasetSubtype.class);
      LOG.debug("Resolving subtype [{}] from source [{}]", datasetSubtype, rawSubtype);
      return datasetSubtype;
    } catch (Exception e) {
      LOG.error("Failed to resolve subtype", e);
      return null;
    }
  }

  /**
   * Return a new GBIF API Dataset instance, derived from the LegacyDataset. Needed because of:
   * http://dev.gbif.org/issues/browse/REG-459
   *
   * @return Dataset derived from LegacyDataset
   */
  public Dataset toApiDataset() {
    Dataset dataset = new Dataset();
    dataset.setKey(getKey());
    dataset.setCreatedBy(getCreatedBy());
    dataset.setModifiedBy(getModifiedBy());
    dataset.setTitle(getTitle());
    dataset.setPublishingOrganizationKey(getPublishingOrganizationKey());
    dataset.setInstallationKey(getInstallationKey());
    dataset.setLanguage(getLanguage());
    dataset.setDescription(getDescription());
    dataset.setLogoUrl(getLogoUrl());
    dataset.setHomepage(getHomepage());
    dataset.setType(getType());
    dataset.setDoi(datasetDoi);
    return dataset;
  }

  @Generated
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    LegacyDataset that = (LegacyDataset) o;
    return primaryContactType == that.primaryContactType
        && Objects.equal(primaryContactEmail, that.primaryContactEmail)
        && Objects.equal(primaryContactName, that.primaryContactName)
        && Objects.equal(primaryContactPhone, that.primaryContactPhone)
        && Objects.equal(primaryContactAddress, that.primaryContactAddress)
        && Objects.equal(primaryContactDescription, that.primaryContactDescription)
        && Objects.equal(serviceTypes, that.serviceTypes)
        && Objects.equal(serviceUrls, that.serviceUrls)
        && Objects.equal(datasetDoi, that.datasetDoi)
        && Objects.equal(primaryContact, that.primaryContact)
        && Objects.equal(emlEndpoint, that.emlEndpoint)
        && Objects.equal(archiveEndpoint, that.archiveEndpoint)
        && Objects.equal(dataPackageEndpoint, that.dataPackageEndpoint);
  }

  @Generated
  @Override
  public int hashCode() {
    return Objects.hashCode(
        super.hashCode(),
        primaryContactType,
        primaryContactEmail,
        primaryContactName,
        primaryContactPhone,
        primaryContactAddress,
        primaryContactDescription,
        serviceTypes,
        serviceUrls,
        datasetDoi,
        primaryContact,
        emlEndpoint,
        archiveEndpoint,
        dataPackageEndpoint);
  }

  @Generated
  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("primaryContactType", primaryContactType)
        .add("primaryContactEmail", primaryContactEmail)
        .add("primaryContactName", primaryContactName)
        .add("primaryContactPhone", primaryContactPhone)
        .add("primaryContactAddress", primaryContactAddress)
        .add("primaryContactDescription", primaryContactDescription)
        .add("serviceTypes", serviceTypes)
        .add("serviceUrls", serviceUrls)
        .add("datasetDoi", datasetDoi)
        .add("primaryContact", primaryContact)
        .add("emlEndpoint", emlEndpoint)
        .add("archiveEndpoint", archiveEndpoint)
        .add("dataPackageEndpoint", dataPackageEndpoint)
        .toString();
  }
}
