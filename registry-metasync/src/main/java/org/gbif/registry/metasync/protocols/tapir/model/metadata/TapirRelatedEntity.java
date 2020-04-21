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
package org.gbif.registry.metasync.protocols.tapir.model.metadata;

import org.gbif.api.vocabulary.Language;

import java.util.List;
import java.util.Set;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.CallMethod;
import org.apache.commons.digester3.annotations.rules.CallParam;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetNext;
import org.apache.commons.digester3.annotations.rules.SetProperty;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ObjectCreate(pattern = "response/metadata/relatedEntity")
public class TapirRelatedEntity {

  @SetProperty(pattern = "response/metadata", attributeName = "lang")
  private Language defaultLanguage;

  private Set<String> roles = Sets.newHashSet();

  @SetProperty(pattern = "response/metadata/relatedEntity/entity", attributeName = "type")
  private String type;

  @BeanPropertySetter(pattern = "response/metadata/relatedEntity/entity/identifier")
  private String identifier;

  private LocalizedString names = new LocalizedString();

  @BeanPropertySetter(pattern = "response/metadata/relatedEntity/entity/acronym")
  private String acronym;

  @BeanPropertySetter(pattern = "response/metadata/relatedEntity/entity/logoURL")
  private String logoUrl;

  private LocalizedString descriptions = new LocalizedString();

  @BeanPropertySetter(pattern = "response/metadata/relatedEntity/entity/address")
  private String address;

  @BeanPropertySetter(pattern = "response/metadata/relatedEntity/entity/regionCode")
  private String regionCode;

  @BeanPropertySetter(pattern = "response/metadata/relatedEntity/entity/countryCode")
  private String countryCode;

  @BeanPropertySetter(pattern = "response/metadata/relatedEntity/entity/zipCode")
  private String zipCode;

  private Set<String> relatedInformation = Sets.newHashSet();
  private List<TapirContact> contacts = Lists.newArrayList();

  @CallMethod(pattern = "response/metadata/relatedEntity/role")
  public void addRole(@CallParam(pattern = "response/metadata/relatedEntity/role") String role) {
    roles.add(role);
  }

  @CallMethod(pattern = "response/metadata/relatedEntity/entity/name")
  public void addName(
      @CallParam(
              pattern = "response/metadata/relatedEntity/entity/name",
              attributeName = "xml:lang")
          Language language,
      @CallParam(pattern = "response/metadata/relatedEntity/entity/name") String name) {
    names.addValue(language, name);
  }

  @CallMethod(pattern = "response/metadata/relatedEntity/entity/description")
  public void addDescription(
      @CallParam(
              pattern = "response/metadata/relatedEntity/entity/description",
              attributeName = "xml:lang")
          Language language,
      @CallParam(pattern = "response/metadata/relatedEntity/entity/description") String name) {
    descriptions.addValue(language, name);
  }

  @CallMethod(pattern = "response/metadata/relatedEntity/entity/relatedInformation")
  public void addRelatedInformation(
      @CallParam(pattern = "response/metadata/relatedEntity/entity/relatedInformation")
          String relatedInformation) {
    this.relatedInformation.add(relatedInformation);
  }

  public Language getDefaultLanguage() {
    return defaultLanguage;
  }

  public void setDefaultLanguage(Language defaultLanguage) {
    this.defaultLanguage = defaultLanguage;
  }

  public Set<String> getRoles() {
    return roles;
  }

  public void setRoles(Set<String> roles) {
    this.roles = roles;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public LocalizedString getNames() {
    return names;
  }

  public void setNames(LocalizedString names) {
    this.names = names;
  }

  public String getAcronym() {
    return acronym;
  }

  public void setAcronym(String acronym) {
    this.acronym = acronym;
  }

  public String getLogoUrl() {
    return logoUrl;
  }

  public void setLogoUrl(String logoUrl) {
    this.logoUrl = logoUrl;
  }

  public LocalizedString getDescriptions() {
    return descriptions;
  }

  public void setDescriptions(LocalizedString descriptions) {
    this.descriptions = descriptions;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getRegionCode() {
    return regionCode;
  }

  public void setRegionCode(String regionCode) {
    this.regionCode = regionCode;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

  public String getZipCode() {
    return zipCode;
  }

  public void setZipCode(String zipCode) {
    this.zipCode = zipCode;
  }

  public Set<String> getRelatedInformation() {
    return relatedInformation;
  }

  public void setRelatedInformation(Set<String> relatedInformation) {
    this.relatedInformation = relatedInformation;
  }

  public List<TapirContact> getContacts() {
    return contacts;
  }

  public void setContacts(List<TapirContact> contacts) {
    this.contacts = contacts;
  }

  @SetNext
  public void addContact(TapirContact contact) {
    this.contacts.add(contact);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("defaultLanguage", defaultLanguage)
        .add("roles", roles)
        .add("type", type)
        .add("identifier", identifier)
        .add("names", names)
        .add("acronym", acronym)
        .add("logoUrl", logoUrl)
        .add("descriptions", descriptions)
        .add("address", address)
        .add("regionCode", regionCode)
        .add("countryCode", countryCode)
        .add("zipCode", zipCode)
        .add("relatedInformation", relatedInformation)
        .add("contacts", contacts)
        .toString();
  }
}
