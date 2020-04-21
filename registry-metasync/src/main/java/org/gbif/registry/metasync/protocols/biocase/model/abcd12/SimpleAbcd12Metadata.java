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
package org.gbif.registry.metasync.protocols.biocase.model.abcd12;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.vocabulary.ContactType;

import java.net.URI;
import java.util.List;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.CallMethod;
import org.apache.commons.digester3.annotations.rules.CallParam;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;

import com.google.common.collect.Lists;

/** This object extracts the same information from ABCD 1.2 as the "old" registry did. */
@ObjectCreate(pattern = "response/content/DataSets/DataSet")
public class SimpleAbcd12Metadata {

  private static final String BASE_PATH = "response/content/DataSets/DataSet/";
  private final List<String> termsOfUses = Lists.newArrayList();
  private final List<String> iprDeclarations = Lists.newArrayList();
  private final List<String> rightsUrls = Lists.newArrayList();
  private final List<String> organisationNames = Lists.newArrayList();
  private final List<String> supplierUrls = Lists.newArrayList();
  private final List<Contact> contacts = Lists.newArrayList();

  @BeanPropertySetter(pattern = BASE_PATH + "OriginalSource/SourceName")
  private String code;

  @BeanPropertySetter(pattern = BASE_PATH + "DatasetDerivations/DatasetDerivation/Description")
  private String description;

  @BeanPropertySetter(pattern = BASE_PATH + "OriginalSource/SourceWebAddress")
  private URI homepage;

  @BeanPropertySetter(
      pattern = BASE_PATH + "DatasetDerivations/DatasetDerivation/Statements/LogoURL")
  private URI logoUrl;

  @BeanPropertySetter(
      pattern = BASE_PATH + "DatasetDerivations/DatasetDerivation/Supplier/Addresses/Address")
  private String address;

  @BeanPropertySetter(
      pattern =
          BASE_PATH + "DatasetDerivations/DatasetDerivation/Supplier/EmailAddresses/EmailAddress")
  private String email;

  @BeanPropertySetter(
      pattern =
          BASE_PATH
              + "DatasetDerivations/DatasetDerivation/Supplier/TelephoneNumbers/TelephoneNumber/Number")
  private String phone;

  @BeanPropertySetter(
      pattern = BASE_PATH + "DatasetDerivations/DatasetDerivation/Rights/TermsOfUse")
  private String rights;

  @BeanPropertySetter(
      pattern = BASE_PATH + "DatasetDerivations/DatasetDerivation/Statements/Acknowledgement")
  private String citationText;

  @BeanPropertySetter(pattern = BASE_PATH + "Units/Unit[0]/RecordBasis")
  private String basisOfRecord;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public URI getHomepage() {
    return homepage;
  }

  public void setHomepage(URI homepage) {
    this.homepage = homepage;
  }

  public URI getLogoUrl() {
    return logoUrl;
  }

  public void setLogoUrl(URI logoUrl) {
    this.logoUrl = logoUrl;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getRights() {
    return rights;
  }

  public void setRights(String rights) {
    this.rights = rights;
  }

  public String getCitationText() {
    return citationText;
  }

  public void setCitationText(String citationText) {
    this.citationText = citationText;
  }

  public String getBasisOfRecord() {
    return basisOfRecord;
  }

  public void setBasisOfRecord(String basisOfRecord) {
    this.basisOfRecord = basisOfRecord;
  }

  public List<String> getTermsOfUses() {
    return termsOfUses;
  }

  public List<String> getIprDeclarations() {
    return iprDeclarations;
  }

  public List<String> getRightsUrls() {
    return rightsUrls;
  }

  public List<String> getOrganisationNames() {
    return organisationNames;
  }

  public List<String> getSupplierUrls() {
    return supplierUrls;
  }

  public List<Contact> getContacts() {
    return contacts;
  }

  @CallMethod(pattern = BASE_PATH + "DatasetDerivations/DatasetDerivation/Rights/TermsOfUse")
  public void addTermsOfUse(
      @CallParam(pattern = BASE_PATH + "DatasetDerivations/DatasetDerivation/Rights/TermsOfUse")
          String termsOfUse) {
    termsOfUses.add(termsOfUse);
  }

  @CallMethod(pattern = BASE_PATH + "DatasetDerivations/DatasetDerivation/Rights/IPRDeclaration")
  public void addIprDeclaration(
      @CallParam(pattern = BASE_PATH + "DatasetDerivations/DatasetDerivation/Rights/IPRDeclaration")
          String iprDeclaration) {
    iprDeclarations.add(iprDeclaration);
  }

  @CallMethod(pattern = BASE_PATH + "DatasetDerivations/DatasetDerivation/Rights/RightsURL")
  public void addRightsUrl(
      @CallParam(pattern = BASE_PATH + "DatasetDerivations/DatasetDerivation/Rights/RightsURL")
          String iprDeclaration) {
    rightsUrls.add(iprDeclaration);
  }

  @CallMethod(pattern = BASE_PATH + "DatasetDerivation/Supplier/OrganisationName")
  public void addOrganisationName(
      @CallParam(pattern = BASE_PATH + "DatasetDerivation/Supplier/OrganisationName")
          String iprDeclaration) {
    organisationNames.add(iprDeclaration);
  }

  @CallMethod(pattern = BASE_PATH + "DatasetDerivation/Supplier/URLs/URL")
  public void addSupplierUrl(
      @CallParam(pattern = BASE_PATH + "DatasetDerivation/Supplier/URLs/URL")
          String iprDeclaration) {
    supplierUrls.add(iprDeclaration);
  }

  @CallMethod(pattern = BASE_PATH + "DatasetDerivations/DatasetDerivation/Supplier")
  public void addTechnicalContact(
      @CallParam(
              pattern =
                  BASE_PATH + "DatasetDerivations/DatasetDerivation/Supplier/Person/PersonName")
          String name,
      @CallParam(
              pattern =
                  BASE_PATH
                      + "DatasetDerivations/DatasetDerivation/Supplier/EmailAddresses/EmailAddress")
          String email,
      @CallParam(
              pattern =
                  BASE_PATH
                      + "DatasetDerivations/DatasetDerivation/Supplier/TelephoneNumbers/TelephoneNumber/Number")
          String phone,
      @CallParam(
              pattern =
                  BASE_PATH + "DatasetDerivations/DatasetDerivation/Supplier/Addresses/Address")
          String address) {
    Contact contact = new Contact();
    contact.setFirstName(name);
    contact.setEmail(Lists.newArrayList(email));
    contact.setPhone(Lists.newArrayList(phone));
    contact.setAddress(Lists.newArrayList(address));
    contact.setType(ContactType.TECHNICAL_POINT_OF_CONTACT);
    contacts.add(contact);
  }

  @CallMethod(pattern = BASE_PATH + "DatasetDerivations/DatasetDerivation/Rights/LegalOwner")
  public void addAdministrativeContact(
      @CallParam(
              pattern =
                  BASE_PATH
                      + "DatasetDerivations/DatasetDerivation/Rights/LegalOwner/Person/PersonName")
          String name,
      @CallParam(
              pattern =
                  BASE_PATH
                      + "DatasetDerivations/DatasetDerivation/Rights/LegalOwner/EmailAddresses/EmailAddress")
          String email,
      @CallParam(
              pattern =
                  BASE_PATH
                      + "DatasetDerivations/DatasetDerivation/Rights/LegalOwner/TelephoneNumbers/TelephoneNumber/Number")
          String phone,
      @CallParam(
              pattern =
                  BASE_PATH
                      + "DatasetDerivations/DatasetDerivation/Rights/LegalOwner/Addresses/Address")
          String address) {
    Contact contact = new Contact();
    contact.setFirstName(name);
    contact.setEmail(Lists.newArrayList(email));
    contact.setPhone(Lists.newArrayList(phone));
    contact.setAddress(Lists.newArrayList(address));
    contact.setType(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT);
    contacts.add(contact);
  }
}
