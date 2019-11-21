package org.gbif.registry.ws.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.registry.ws.annotation.ParamName;
import org.gbif.registry.ws.util.LegacyResourceConstants;
import org.gbif.registry.ws.util.LegacyResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

/**
 * Class used to create or update an Endpoint for legacy (GBRDS/IPT) API. Previously known as a Service in the GBRDS.
 * A set of HTTP Form parameters coming from a POST request are injected.
 * </br>
 * Its fields are injected using the @ParamName. It is assumed the following parameters exist in the HTTP request:
 * 'resourceKey', 'description', 'descriptionLanguage', 'type', 'accessPointURL'.
 * </br>
 * JAXB annotations allow the class to be converted into an XML document, that gets included in the Response following
 * a successful registration or update. @XmlElement is used to specify element names that consumers of legacy services
 * expect to find.
 */
@XmlRootElement(name = "service")
public class LegacyEndpoint extends Endpoint {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyEndpoint.class);

  // injected from HTTP form parameters
  private UUID datasetKey;

  /**
   * Set the endpoint's dataset key. Mandatory field, injected on both create and update requests.
   *
   * @param resourceKey dataset key as UUID
   */
  @ParamName(value = LegacyResourceConstants.RESOURCE_KEY_PARAM)
  public void setDatasetKey(String resourceKey) {
    try {
      datasetKey = UUID.fromString(Strings.nullToEmpty(resourceKey));
    } catch (IllegalArgumentException e) {
      LOG.error("Dataset key is not a valid UUID: {}", Strings.nullToEmpty(resourceKey));
    }
  }

  /**
   * Get the endpoint's dataset key.
   *
   * @return dataset key
   */
  @XmlElement(name = LegacyResourceConstants.RESOURCE_KEY_PARAM)
  @NotNull
  public UUID getDatasetKey() {
    return datasetKey;
  }

  /**
   * Set the endpoint description.
   *
   * @param description of the endpoint
   */
  @ParamName(value = LegacyResourceConstants.DESCRIPTION_PARAM)
  @Override
  public void setDescription(String description) {
    super.setDescription(LegacyResourceUtils.validateField(description, 10));
  }

  /**
   * Get the endpoint description. This method is not used but it is needed otherwise this Object
   * can't be converted into an XML document via JAXB.
   *
   * @return description of the endpoint
   */
  @XmlElement(name = LegacyResourceConstants.DESCRIPTION_PARAM)
  @Nullable
  public String getEndpointDescription() {
    return getDescription();
  }

  /**
   * Set endpoint type. First, check if it is not null or empty. The incoming type always comes from the GBRDS Service
   * Type vocabulary. Note: this field is required, and the old web services would throw 400 response if not found.
   *
   * @param type endpoint type
   */
  @ParamName(value = LegacyResourceConstants.TYPE_PARAM)
  public void setType(String type) {
    String injected = Strings.nullToEmpty(type);
    EndpointType lookup = EndpointType.fromString(injected);
    if (lookup == null) {
      // try to match some endpoints with their variant name
      if (injected.equalsIgnoreCase(LegacyResourceConstants.CHECKLIST_SERVICE_TYPE_1)
        || injected.equalsIgnoreCase(LegacyResourceConstants.CHECKLIST_SERVICE_TYPE_2)
        || injected.equalsIgnoreCase(LegacyResourceConstants.OCCURRENCE_SERVICE_TYPE_1)
        || injected.equalsIgnoreCase(LegacyResourceConstants.OCCURRENCE_SERVICE_TYPE_2)
        || injected.equalsIgnoreCase(LegacyResourceConstants.SAMPLING_EVENT_SERVICE_TYPE)) {
        setType(EndpointType.DWC_ARCHIVE);
      } else {
        LOG.error("Endpoint type could not be interpreted: {}", injected);
      }
    } else {
      setType(lookup);
    }
  }

  /**
   * Get endpoint type. This method is not used but it is needed otherwise this Object can't be converted into
   * an XML document via JAXB.
   *
   * @return primary contact type
   */
  @XmlElement(name = LegacyResourceConstants.TYPE_PARAM)
  @NotNull
  public EndpointType getEndpointType() {
    return getType();
  }

  /**
   * Set the endpoint URL.
   *
   * @param url of the endpoint
   */
  @ParamName(value = LegacyResourceConstants.ACCESS_POINT_URL_PARAM)
  public void setEndpointUrl(String url) {
    if (!Strings.isNullOrEmpty(url)) {
      try {
        URI uri = new URI(url);
        setUrl(uri);
      } catch (URISyntaxException e) {
        LOG.warn("Endpoint URL was invalid: {}", Strings.nullToEmpty(url));
      }
    }
  }

  /**
   * Get the endpoint URL. This method is not used but it is needed otherwise this Object
   * can't be converted into an XML document via JAXB.
   *
   * @return url of the endpoint
   */
  @XmlElement(name = LegacyResourceConstants.ACCESS_POINT_URL_PARAM)
  @NotNull
  public String getEndpointUrl() {
    if (getUrl() == null) {
      throw new IllegalStateException("Null is not acceptable");
    }
    return getUrl().toASCIIString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    LegacyEndpoint that = (LegacyEndpoint) o;
    return Objects.equal(datasetKey, that.datasetKey);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), datasetKey);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("datasetKey", datasetKey)
      .toString();
  }
}
