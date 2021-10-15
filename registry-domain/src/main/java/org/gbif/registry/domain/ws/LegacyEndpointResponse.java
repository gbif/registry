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

import org.gbif.api.model.registry.Endpoint;
import org.gbif.registry.domain.ws.util.LegacyResourceConstants;

import java.util.Optional;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class used to generate responses for legacy (GBRDS/IPT) API. </br> JAXB annotations allow the
 * class to be converted into an XML document or JSON response. @XmlElement is used to specify
 * element names that consumers of legacy services expect to find.
 */
@XmlRootElement(name = "service")
public class LegacyEndpointResponse {

  private String key;
  private String organisationKey;
  private String resourceKey;
  private String description;
  private String descriptionLanguage;
  private String type;
  private String typeDescription;
  private String accessPointURL;

  public LegacyEndpointResponse(Endpoint endpoint, UUID datasetKey) {
    key = Optional.ofNullable(endpoint.getKey()).map(Object::toString).orElse("");
    type = Optional.ofNullable(endpoint.getType()).map(Enum::name).orElse("");
    resourceKey = datasetKey == null ? "" : datasetKey.toString();
    description = endpoint.getDescription() == null ? "" : endpoint.getDescription();
    accessPointURL = endpoint.getUrl() == null ? "" : endpoint.getUrl().toASCIIString();
    // always empty - not null because they have to be included in response
    organisationKey = "";
    descriptionLanguage = "";
    typeDescription = "";
  }

  /** No argument, default constructor needed by JAXB. */
  public LegacyEndpointResponse() {}

  @XmlElement(name = LegacyResourceConstants.KEY_PARAM)
  @NotNull
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  @XmlElement(name = LegacyResourceConstants.RESOURCE_KEY_PARAM)
  @NotNull
  public String getResourceKey() {
    return resourceKey;
  }

  public void setResourceKey(String resourceKey) {
    this.resourceKey = resourceKey;
  }

  @XmlElement(name = LegacyResourceConstants.ORGANIZATION_KEY_PARAM)
  @NotNull
  public String getOrganisationKey() {
    return organisationKey;
  }

  public void setOrganisationKey(String organisationKey) {
    this.organisationKey = organisationKey;
  }

  @XmlElement(name = LegacyResourceConstants.TYPE_PARAM)
  @NotNull
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @XmlElement(name = LegacyResourceConstants.TYPE_DESCRIPTION_PARAM)
  @NotNull
  public String getTypeDescription() {
    return typeDescription;
  }

  public void setTypeDescription(String typeDescription) {
    this.typeDescription = typeDescription;
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

  @XmlElement(name = LegacyResourceConstants.ACCESS_POINT_URL_PARAM)
  @NotNull
  public String getAccessPointURL() {
    return accessPointURL;
  }

  public void setAccessPointURL(String accessPointURL) {
    this.accessPointURL = accessPointURL;
  }
}
