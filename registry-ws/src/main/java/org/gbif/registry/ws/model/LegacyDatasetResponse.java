package org.gbif.registry.ws.model;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.ws.util.LegacyResourceConstants;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Strings;

/**
 * Class used to generate responses for legacy (GBRDS/IPT) API.
 * </br>
 * JAXB annotations allow the class to be converted into an XML document or JSON response. @XmlElement is used to
 * specify element names that consumers of legacy services expect to find.
 */
@XmlRootElement(name = "resource")
public class LegacyDatasetResponse {

  private String key;
  private String organisationKey;
  private String name;
  private String nameLanguage;
  private String description;
  private String descriptionLanguage;
  private String homepageURL;
  private String primaryContactName;
  private String primaryContactAddress;
  private String primaryContactEmail;
  private String primaryContactPhone;
  private String primaryContactDescription;
  private String primaryContactType;

  public LegacyDatasetResponse(Dataset dataset, Contact contact) {
    key = dataset.getKey() == null ? "" : dataset.getKey().toString();
    organisationKey = dataset.getOwningOrganizationKey() == null ? "" : dataset.getOwningOrganizationKey().toString();
    name = dataset.getTitle() == null ? "" : dataset.getTitle();
    description = dataset.getDescription() == null ? "" : dataset.getDescription();
    descriptionLanguage = dataset.getLanguage() == null ? "" : dataset.getLanguage().getIso2LetterCode();
    nameLanguage = dataset.getLanguage() == null ? "" : dataset.getLanguage().getIso2LetterCode();
    homepageURL = dataset.getHomepage() == null ? "" : dataset.getHomepage().toString();
    primaryContactAddress = contact == null ? "" : Strings.nullToEmpty(contact.getAddress());
    primaryContactDescription = contact == null ? "" : Strings.nullToEmpty(contact.getDescription());
    primaryContactEmail = contact == null ? "" : Strings.nullToEmpty(contact.getEmail());
    primaryContactPhone = contact == null ? "" : Strings.nullToEmpty(contact.getPhone());
    primaryContactName = contact == null
      ? ""
      : Strings.nullToEmpty(contact.getFirstName()) + " " + Strings.nullToEmpty(contact.getLastName());
    // conversion of contact type, defaulting to ""
    primaryContactType = "";
    if (contact != null) {
      ContactType type = contact.getType();
      if (type != null) {
        if (type.compareTo(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT) == 0) {
          primaryContactType = LegacyResourceConstants.ADMINISTRATIVE_CONTACT_TYPE;
        } else if (type.compareTo(ContactType.TECHNICAL_POINT_OF_CONTACT) == 0) {
          primaryContactType = LegacyResourceConstants.TECHNICAL_CONTACT_TYPE;
        }
      }
    }
  }

  /**
   * No argument, default constructor needed by JAXB.
   */
  public LegacyDatasetResponse() {
  }

  @XmlElement(name = LegacyResourceConstants.KEY_PARAM)
  @NotNull
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  @XmlElement(name = LegacyResourceConstants.NAME_PARAM)
  @NotNull
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @XmlElement(name = LegacyResourceConstants.NAME_LANGUAGE_PARAM)
  @NotNull
  public String getNameLanguage() {
    return nameLanguage;
  }

  public void setNameLanguage(String nameLanguage) {
    this.nameLanguage = nameLanguage;
  }

  @XmlElement(name = LegacyResourceConstants.ORGANIZATION_KEY_PARAM)
  @NotNull
  public String getOrganisationKey() {
    return organisationKey;
  }

  public void setOrganisationKey(String organisationKey) {
    this.organisationKey = organisationKey;
  }

  @XmlElement(name = LegacyResourceConstants.DESCRIPTION_PARAM)
  @NotNull
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @XmlElement(name = LegacyResourceConstants.DESCRIPTION_LANGUAGE_PARAM)
  @NotNull
  public String getDescriptionLanguage() {
    return descriptionLanguage;
  }

  public void setDescriptionLanguage(String descriptionLanguage) {
    this.descriptionLanguage = descriptionLanguage;
  }

  @XmlElement(name = LegacyResourceConstants.HOMEPAGE_URL_PARAM)
  @NotNull
  public String getHomepageURL() {
    return homepageURL;
  }

  public void setHomepageURL(String homepageURL) {
    this.homepageURL = homepageURL;
  }

  @XmlElement(name = LegacyResourceConstants.PRIMARY_CONTACT_NAME_PARAM)
  @NotNull
  public String getPrimaryContactName() {
    return primaryContactName;
  }

  public void setPrimaryContactName(String primaryContactName) {
    this.primaryContactName = primaryContactName;
  }

  @XmlElement(name = LegacyResourceConstants.PRIMARY_CONTACT_ADDRESS_PARAM)
  @NotNull
  public String getPrimaryContactAddress() {
    return primaryContactAddress;
  }

  public void setPrimaryContactAddress(String primaryContactAddress) {
    this.primaryContactAddress = primaryContactAddress;
  }

  @XmlElement(name = LegacyResourceConstants.PRIMARY_CONTACT_EMAIL_PARAM)
  @NotNull
  public String getPrimaryContactEmail() {
    return primaryContactEmail;
  }

  public void setPrimaryContactEmail(String primaryContactEmail) {
    this.primaryContactEmail = primaryContactEmail;
  }

  @XmlElement(name = LegacyResourceConstants.PRIMARY_CONTACT_PHONE_PARAM)
  @NotNull
  public String getPrimaryContactPhone() {
    return primaryContactPhone;
  }

  public void setPrimaryContactPhone(String primaryContactPhone) {
    this.primaryContactPhone = primaryContactPhone;
  }

  @XmlElement(name = LegacyResourceConstants.PRIMARY_CONTACT_DESCRIPTION_PARAM)
  @NotNull
  public String getPrimaryContactDescription() {
    return primaryContactDescription;
  }

  public void setPrimaryContactDescription(String primaryContactDescription) {
    this.primaryContactDescription = primaryContactDescription;
  }

  @XmlElement(name = LegacyResourceConstants.PRIMARY_CONTACT_TYPE_PARAM)
  @NotNull
  public String getPrimaryContactType() {
    return primaryContactType;
  }

  public void setPrimaryContactType(String primaryContactType) {
    this.primaryContactType = primaryContactType;
  }
}
