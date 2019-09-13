package org.gbif.registry.ws.model;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.ws.util.LegacyResourceConstants;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class used to generate response for legacy (GBRDS/IPT) API. Previously known as an Organisation with an s in the
 * GBRDS.
 * </br>
 * JAXB annotations allow the class to be converted into an XML document or JSON response. @XmlElement is used to
 * specify element names that consumers of legacy services expect to find.
 */
@XmlRootElement(name = "organisation")
public class LegacyOrganizationResponse {

  private String key;
  private String name;
  private String nameLanguage;
  private String homepageURL;
  private String description;
  private String descriptionLanguage;
  private String nodeKey;
  private String nodeName;
  private String nodeContactEmail;
  private String primaryContactType;
  private String primaryContactName;
  private String primaryContactEmail;
  private String primaryContactAddress;
  private String primaryContactPhone;
  private String primaryContactDescription;

  private static final Joiner CONTACT_NAME = Joiner.on(" ").skipNulls();

  public LegacyOrganizationResponse(Organization organization, Contact contact, Node node) {
    key = organization.getKey() == null ? "" : organization.getKey().toString();
    name = Strings.nullToEmpty(organization.getTitle());
    nameLanguage = organization.getLanguage() == null ? "" : organization.getLanguage().getIso2LetterCode();
    homepageURL = organization.getHomepage() == null ? "" : organization.getHomepage().toString();
    description = Strings.nullToEmpty(organization.getDescription());
    descriptionLanguage = organization.getLanguage() == null ? "" : organization.getLanguage().getIso2LetterCode();
    primaryContactAddress = contact == null || contact.getAddress().isEmpty() ? "" :
        Strings.nullToEmpty(contact.getAddress().get(0));
    primaryContactDescription = contact == null ? "" : Strings.nullToEmpty(contact.getDescription());
    primaryContactEmail = contact == null || contact.getEmail().isEmpty() ? "" :
        Strings.nullToEmpty(contact.getEmail().get(0));
    primaryContactPhone = contact == null || contact.getPhone().isEmpty() ? "" :
        Strings.nullToEmpty(contact.getPhone().get(0));
    primaryContactName =
        contact == null ? "" : CONTACT_NAME.join(new String[] {contact.getFirstName(), contact.getLastName()});
    nodeKey = node.getKey() == null ? "" : node.getKey().toString();
    nodeName = node.getTitle() == null ? "" : node.getTitle();
    nodeContactEmail = node.getEmail().isEmpty() ? "" : node.getEmail().get(0);
    // conversion of contact type, defaulting to empty
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
  public LegacyOrganizationResponse() {
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

  @XmlElement(name = "nodeKey")
  @NotNull
  public String getNodeKey() {
    return nodeKey;
  }

  public void setNodeKey(String nodeKey) {
    this.nodeKey = nodeKey;
  }

  @XmlElement(name = "nodeName")
  @NotNull
  public String getNodeName() {
    return nodeName;
  }

  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

  @XmlElement(name = "nodeContactEmail")
  @NotNull
  public String getNodeContactEmail() {
    return nodeContactEmail;
  }

  public void setNodeContactEmail(String nodeContactEmail) {
    this.nodeContactEmail = nodeContactEmail;
  }

  @XmlElement(name = LegacyResourceConstants.NAME_LANGUAGE_PARAM)
  @NotNull
  public String getNameLanguage() {
    return nameLanguage;
  }

  public void setNameLanguage(String nameLanguage) {
    this.nameLanguage = nameLanguage;
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

  @XmlElement(name = LegacyResourceConstants.PRIMARY_CONTACT_TYPE_PARAM)
  @NotNull
  public String getPrimaryContactType() {
    return primaryContactType;
  }

  public void setPrimaryContactType(String primaryContactType) {
    this.primaryContactType = primaryContactType;
  }

  @XmlElement(name = LegacyResourceConstants.PRIMARY_CONTACT_NAME_PARAM)
  @NotNull
  public String getPrimaryContactName() {
    return primaryContactName;
  }

  public void setPrimaryContactName(String primaryContactName) {
    this.primaryContactName = primaryContactName;
  }

  @XmlElement(name = LegacyResourceConstants.PRIMARY_CONTACT_EMAIL_PARAM)
  @NotNull
  public String getPrimaryContactEmail() {
    return primaryContactEmail;
  }

  public void setPrimaryContactEmail(String primaryContactEmail) {
    this.primaryContactEmail = primaryContactEmail;
  }

  @XmlElement(name = LegacyResourceConstants.PRIMARY_CONTACT_ADDRESS_PARAM)
  @NotNull
  public String getPrimaryContactAddress() {
    return primaryContactAddress;
  }

  public void setPrimaryContactAddress(String primaryContactAddress) {
    this.primaryContactAddress = primaryContactAddress;
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
}
