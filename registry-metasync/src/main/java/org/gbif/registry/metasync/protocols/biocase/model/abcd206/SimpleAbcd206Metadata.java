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
package org.gbif.registry.metasync.protocols.biocase.model.abcd206;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.vocabulary.ContactType;

import java.net.URI;
import java.util.List;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.CallMethod;
import org.apache.commons.digester3.annotations.rules.CallParam;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;

import com.google.common.collect.Lists;

/** This object extracts the same information from ABCD 2.06 as the "old" registry did. */
@ObjectCreate(pattern = "response/content/DataSets/DataSet")
public class SimpleAbcd206Metadata {

  private static final String BASE_PATH = "response/content/DataSets/DataSet/";
  private final List<String> termsOfUses = Lists.newArrayList();
  private final List<String> iprDeclarations = Lists.newArrayList();
  private final List<String> disclaimers = Lists.newArrayList();
  private final List<String> organisations = Lists.newArrayList();
  private final List<String> ownerUrls = Lists.newArrayList();
  private final List<Contact> contacts = Lists.newArrayList();

  @BeanPropertySetter(pattern = BASE_PATH + "Metadata/Description/Representation/Title")
  private String name;
  // description
  @BeanPropertySetter(pattern = BASE_PATH + "Metadata/Description/Representation/Details")
  private String details;

  @BeanPropertySetter(pattern = BASE_PATH + "Metadata/Description/Representation/URI")
  private URI homepage;

  @BeanPropertySetter(pattern = BASE_PATH + "Metadata/Owners/Owner/LogoURI")
  private URI logoUrl;

  @BeanPropertySetter(pattern = BASE_PATH + "Metadata/Owners/Owner/Addresses/Address")
  private String address;

  @BeanPropertySetter(pattern = BASE_PATH + "Metadata/Owners/Owner/EmailAddresses/EmailAddress")
  private String email;

  @BeanPropertySetter(
      pattern = BASE_PATH + "Metadata/Owners/Owner/TelephoneNumbers/TelephoneNumber/Number")
  private String phone;

  @BeanPropertySetter(pattern = BASE_PATH + "Metadata/IPRStatements/Copyrights/Copyright/Text")
  private String rights;

  @BeanPropertySetter(pattern = BASE_PATH + "Metadata/IPRStatements/Licenses/License/Text")
  private String licenseText;

  @BeanPropertySetter(pattern = BASE_PATH + "Metadata/IPRStatements/Licenses/License/URI")
  private URI licenseUri;

  @BeanPropertySetter(pattern = BASE_PATH + "Metadata/IPRStatements/Citations/Citation/Text")
  private String citationText;

  @BeanPropertySetter(pattern = BASE_PATH + "Units/Unit[0]/RecordBasis")
  private String basisOfRecord;

  @BeanPropertySetter(pattern = BASE_PATH + "DatasetGUID")
  private String datasetGUID;

  public String getBasisOfRecord() {
    return basisOfRecord;
  }

  public void setBasisOfRecord(String basisOfRecord) {
    this.basisOfRecord = basisOfRecord;
  }

  public String getLicenseText() {
    return licenseText;
  }

  public void setLicenseText(String licenseText) {
    this.licenseText = licenseText;
  }

  public URI getLicenseUri() {
    return licenseUri;
  }

  public void setLicenseUri(URI licenseUri) {
    this.licenseUri = licenseUri;
  }

  public String getCitationText() {
    return citationText;
  }

  public void setCitationText(String citationText) {
    this.citationText = citationText;
  }

  public String getRights() {
    return rights;
  }

  public void setRights(String rights) {
    this.rights = rights;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public URI getLogoUrl() {
    return logoUrl;
  }

  public void setLogoUrl(URI logoUrl) {
    this.logoUrl = logoUrl;
  }

  public URI getHomepage() {
    return homepage;
  }

  public void setHomepage(URI homepage) {
    this.homepage = homepage;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public String getName() {
    return name;
  }

  public String getDatasetGUID() {
    return datasetGUID;
  }

  public void setDatasetGUID(String datasetGUID) {
    this.datasetGUID = datasetGUID;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getTermsOfUses() {
    return termsOfUses;
  }

  public List<String> getIprDeclarations() {
    return iprDeclarations;
  }

  public List<String> getDisclaimers() {
    return disclaimers;
  }

  public List<String> getOrganisations() {
    return organisations;
  }

  public List<String> getOwnerUrls() {
    return ownerUrls;
  }

  public List<Contact> getContacts() {
    return contacts;
  }

  @CallMethod(pattern = BASE_PATH + "Metadata/IPRStatements/TermsOfUseStatements/TermsOfUse")
  public void addTermsOfUse(
      @CallParam(pattern = BASE_PATH + "Metadata/IPRStatements/TermsOfUseStatements/TermsOfUse")
          String termsOfUse) {
    termsOfUses.add(termsOfUse);
  }

  @CallMethod(pattern = BASE_PATH + "Metadata/IPRDeclarations/IPRDeclaration")
  public void addIprDeclaration(
      @CallParam(pattern = BASE_PATH + "Metadata/IPRDeclarations/IPRDeclaration")
          String iprDeclaration) {
    iprDeclarations.add(iprDeclaration);
  }

  @CallMethod(pattern = BASE_PATH + "Metadata/Disclaimers/Disclaimer")
  public void addDisclaimer(
      @CallParam(pattern = BASE_PATH + "Metadata/Disclaimers/Disclaimer") String disclaimer) {
    disclaimers.add(disclaimer);
  }

  @CallMethod(pattern = BASE_PATH + "Metadata/Owners/Owner/Organisation/Name/Representation/Text")
  public void addOrganisation(
      @CallParam(
              pattern = BASE_PATH + "Metadata/Owners/Owner/Organisation/Name/Representation/Text")
          String organisation) {
    organisations.add(organisation);
  }

  @CallMethod(pattern = BASE_PATH + "Metadata/Owners/Owner/URIs/URL")
  public void addOwnerUrl(
      @CallParam(pattern = BASE_PATH + "Metadata/Owners/Owner/URIs/URL") String ownerUrl) {
    ownerUrls.add(ownerUrl);
  }

  @CallMethod(pattern = BASE_PATH + "TechnicalContacts/TechnicalContact")
  public void addTechnicalContact(
      @CallParam(pattern = BASE_PATH + "TechnicalContacts/TechnicalContact/Name") String name,
      @CallParam(pattern = BASE_PATH + "TechnicalContacts/TechnicalContact/Email") String email,
      @CallParam(pattern = BASE_PATH + "TechnicalContacts/TechnicalContact/Phone") String phone,
      @CallParam(pattern = BASE_PATH + "TechnicalContacts/TechnicalContact/Address")
          String address) {
    Contact contact = new Contact();
    contact.setFirstName(name);
    contact.setEmail(Lists.newArrayList(email));
    contact.setPhone(Lists.newArrayList(phone));
    contact.setAddress(Lists.newArrayList(address));
    contact.setType(ContactType.TECHNICAL_POINT_OF_CONTACT);
    contacts.add(contact);
  }

  @CallMethod(pattern = BASE_PATH + "ContentContacts/ContentContact")
  public void addAdministrativeContact(
      @CallParam(pattern = BASE_PATH + "ContentContacts/ContentContact/Name") String name,
      @CallParam(pattern = BASE_PATH + "ContentContacts/ContentContact/Email") String email,
      @CallParam(pattern = BASE_PATH + "ContentContacts/ContentContact/Phone") String phone,
      @CallParam(pattern = BASE_PATH + "ContentContacts/ContentContact/Address") String address) {
    Contact contact = new Contact();
    contact.setFirstName(name);
    contact.setEmail(Lists.newArrayList(email));
    contact.setPhone(Lists.newArrayList(phone));
    contact.setAddress(Lists.newArrayList(address));
    contact.setType(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT);
    contacts.add(contact);
  }
}
