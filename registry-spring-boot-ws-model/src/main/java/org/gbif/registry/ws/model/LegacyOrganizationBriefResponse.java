package org.gbif.registry.ws.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.ws.util.LegacyResourceConstants;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class used to generate response for legacy (GBRDS/IPT) API.
 * </br>
 * JAXB annotations allow the class to be converted into an XML document or JSON response. @XmlElement is used to
 * specify element names that consumers of legacy services expect to find.
 */
@XmlRootElement(name = "organisation")
public class LegacyOrganizationBriefResponse {

  private String key;
  private String name;

  public LegacyOrganizationBriefResponse(Organization organization) {
    key = organization.getKey() == null ? "" : organization.getKey().toString();
    name = Strings.nullToEmpty(organization.getTitle());
  }

  /**
   * No argument, default constructor needed by JAXB.
   */
  public LegacyOrganizationBriefResponse() {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LegacyOrganizationBriefResponse that = (LegacyOrganizationBriefResponse) o;
    return Objects.equal(key, that.key) &&
      Objects.equal(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(key, name);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("key", key)
      .add("name", name)
      .toString();
  }
}
