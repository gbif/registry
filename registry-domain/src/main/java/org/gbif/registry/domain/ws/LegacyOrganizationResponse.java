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
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.Language;
import org.gbif.registry.domain.ws.util.LegacyResourceConstants;

import java.util.Optional;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

/**
 * Class used to generate response for legacy (GBRDS/IPT) API. Previously known as an Organisation
 * with an s in the GBRDS. </br> JAXB annotations allow the class to be converted into an XML
 * document or JSON response. @XmlElement is used to specify element names that consumers of legacy
 * services expect to find.
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
    mapOrganizationPart(organization);
    mapContactPart(contact);
    mapNodePart(node);
  }

  private void mapOrganizationPart(Organization organization) {
    key = organization.getKey() == null ? "" : organization.getKey().toString();
    name = Strings.nullToEmpty(organization.getTitle());
    nameLanguage =
        Optional.ofNullable(organization.getLanguage()).map(Language::getIso2LetterCode).orElse("");
    homepageURL = organization.getHomepage() == null ? "" : organization.getHomepage().toString();
    description = Strings.nullToEmpty(organization.getDescription());
    descriptionLanguage = nameLanguage;
  }

  private void mapContactPart(Contact contact) {
    primaryContactAddress =
        contact == null || contact.getAddress().isEmpty()
            ? ""
            : Strings.nullToEmpty(contact.getAddress().get(0));
    primaryContactDescription =
        contact == null ? "" : Strings.nullToEmpty(contact.getDescription());
    primaryContactEmail =
        contact == null || contact.getEmail() == null || contact.getEmail().isEmpty()
            ? ""
            : Strings.nullToEmpty(contact.getEmail().get(0));
    primaryContactPhone =
        contact == null || contact.getPhone() == null || contact.getPhone().isEmpty()
            ? ""
            : Strings.nullToEmpty(contact.getPhone().get(0));
    primaryContactName =
        contact == null
            ? ""
            : CONTACT_NAME.join(new String[] {contact.getFirstName(), contact.getLastName()});

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

  private void mapNodePart(Node node) {
    nodeKey = node.getKey() == null ? "" : node.getKey().toString();
    nodeName = node.getTitle() == null ? "" : node.getTitle();
    nodeContactEmail =
        node.getEmail() == null || node.getEmail().isEmpty() ? "" : node.getEmail().get(0);
  }

  /** No argument, default constructor needed by JAXB. */
  public LegacyOrganizationResponse() {}

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

  @Generated
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LegacyOrganizationResponse that = (LegacyOrganizationResponse) o;
    return Objects.equal(key, that.key)
        && Objects.equal(name, that.name)
        && Objects.equal(nameLanguage, that.nameLanguage)
        && Objects.equal(homepageURL, that.homepageURL)
        && Objects.equal(description, that.description)
        && Objects.equal(descriptionLanguage, that.descriptionLanguage)
        && Objects.equal(nodeKey, that.nodeKey)
        && Objects.equal(nodeName, that.nodeName)
        && Objects.equal(nodeContactEmail, that.nodeContactEmail)
        && Objects.equal(primaryContactType, that.primaryContactType)
        && Objects.equal(primaryContactName, that.primaryContactName)
        && Objects.equal(primaryContactEmail, that.primaryContactEmail)
        && Objects.equal(primaryContactAddress, that.primaryContactAddress)
        && Objects.equal(primaryContactPhone, that.primaryContactPhone)
        && Objects.equal(primaryContactDescription, that.primaryContactDescription);
  }

  @Generated
  @Override
  public int hashCode() {
    return Objects.hashCode(
        key,
        name,
        nameLanguage,
        homepageURL,
        description,
        descriptionLanguage,
        nodeKey,
        nodeName,
        nodeContactEmail,
        primaryContactType,
        primaryContactName,
        primaryContactEmail,
        primaryContactAddress,
        primaryContactPhone,
        primaryContactDescription);
  }

  @Generated
  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("key", key)
        .add("name", name)
        .add("nameLanguage", nameLanguage)
        .add("homepageURL", homepageURL)
        .add("description", description)
        .add("descriptionLanguage", descriptionLanguage)
        .add("nodeKey", nodeKey)
        .add("nodeName", nodeName)
        .add("nodeContactEmail", nodeContactEmail)
        .add("primaryContactType", primaryContactType)
        .add("primaryContactName", primaryContactName)
        .add("primaryContactEmail", primaryContactEmail)
        .add("primaryContactAddress", primaryContactAddress)
        .add("primaryContactPhone", primaryContactPhone)
        .add("primaryContactDescription", primaryContactDescription)
        .toString();
  }
}
