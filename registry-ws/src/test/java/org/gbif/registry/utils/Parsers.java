package org.gbif.registry.utils;

import org.gbif.registry.ws.util.LegacyResourceConstants;

import java.io.InputStream;
import java.nio.charset.Charset;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Parsers {

  private static Logger LOG = LoggerFactory.getLogger(Parsers.class);
  private static final Charset UTF8 = Charset.forName("UTF8");
  public static LegacyDatasetResponseHandler legacyDatasetResponseHandler;
  public static LegacyOrganizationResponseHandler legacyOrganizationResponseHandler;
  public static LegacyIptEntityHandler legacyIptEntityHandler;
  public static LegacyEndpointResponseHandler legacyEndpointResponseHandler;

  public static SAXParser saxParser;

  static {
    try {
      saxParser = getNsAwareSaxParserFactory().newSAXParser();
    } catch (Exception e) {
      LOG.error("Problem occurred creating new SAXParser: {}", e.getMessage(), e);
    }
    legacyDatasetResponseHandler = new LegacyDatasetResponseHandler();
    legacyOrganizationResponseHandler = new LegacyOrganizationResponseHandler();
    legacyIptEntityHandler = new LegacyIptEntityHandler();
    legacyEndpointResponseHandler = new LegacyEndpointResponseHandler();
  }

  /**
   * Configure a non validating, namespace aware, SAXParserFactory and return it.
   * </br>
   * Note: this method is copied directly out of the IPT.
   *
   * @return configured SAXParserFactory
   */
  private static SAXParserFactory getNsAwareSaxParserFactory() {
    SAXParserFactory saxf = null;
    try {
      saxf = SAXParserFactory.newInstance();
      saxf.setValidating(false);
      saxf.setNamespaceAware(true);
    } catch (Exception e) {
      LOG.error("Cant create namespace aware SAX Parser Factory: " + e.getMessage(), e);
    }
    return saxf;
  }

  /**
   * Convert a String into a ByteArrayInputStream.
   *
   * @param source String to convert
   *
   * @return ByteArrayInputStream
   */
  public static InputStream getUtf8Stream(String source) {
    return IOUtils.toInputStream(source, UTF8);
  }

  /**
   * Super simple SAX handler that extracts all element and attribute content from any XML document. The resulting
   * string is concatenating all content and inserts a space at every element or attribute start.
   * </br>
   * Note: this class is copied directly out of the IPT. It also uses a commons-lang3 dependency.
   */
  public static class LegacyIptEntityHandler extends DefaultHandler {

    private String content;
    public String organisationKey;
    public String resourceKey;
    public String serviceKey;
    public String password;
    public String key;

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      content += String.valueOf(ArrayUtils.subarray(ch, start, start + length));
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
      if (name.equalsIgnoreCase("key")) {
        key = content.replaceAll("\\s", "");
      } else if (name.equalsIgnoreCase("organisationKey") || name.equalsIgnoreCase("organizationKey")) {
        organisationKey = content.replaceAll("\\s", "");
      } else if (name.equalsIgnoreCase("resourceKey")) {
        resourceKey = content.replaceAll("\\s", "");
      } else if (name.equalsIgnoreCase("serviceKey")) {
        serviceKey = content.replaceAll("\\s", "");
      }
      content = "";
    }

    @Override
    public void startDocument() throws SAXException {
      content = "";
      key = "";
      organisationKey = "";
      resourceKey = "";
      serviceKey = "";
      password = "";
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
      content = "";
    }
  }

  /**
   * Super simple SAX handler that parses all LegacyDatasetResponse elements expected in an XML Response. The resulting
   * string is concatenating all content and inserts a space at every element or attribute start.
   *
   * @see org.gbif.registry.ws.model.LegacyDatasetResponse
   */
  public static class LegacyDatasetResponseHandler extends DefaultHandler {

    private String content;
    public String key;
    public String organisationKey;
    public String name;
    public String nameLanguage;
    public String description;
    public String descriptionLanguage;
    public String logoURL;
    public String homepageURL;
    public String primaryContactName;
    public String primaryContactEmail;
    public String primaryContactType;
    public String primaryContactAddress;
    public String primaryContactPhone;
    public String primaryContactDescription;

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      content += String.valueOf(ArrayUtils.subarray(ch, start, start + length));
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
      if (name.equalsIgnoreCase(LegacyResourceConstants.KEY_PARAM)) {
        key = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.ORGANIZATION_KEY_PARAM) || name
        .equalsIgnoreCase("organizationKey")) {
        organisationKey = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.NAME_PARAM)) {
        this.name = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.NAME_LANGUAGE_PARAM)) {
        nameLanguage = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.DESCRIPTION_PARAM)) {
        description = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.DESCRIPTION_LANGUAGE_PARAM)) {
        descriptionLanguage = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.LOGO_URL_PARAM)) {
        logoURL = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.HOMEPAGE_URL_PARAM)) {
        homepageURL = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.PRIMARY_CONTACT_NAME_PARAM)) {
        primaryContactName = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.PRIMARY_CONTACT_EMAIL_PARAM)) {
        primaryContactEmail = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.PRIMARY_CONTACT_TYPE_PARAM)) {
        primaryContactType = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.PRIMARY_CONTACT_ADDRESS_PARAM)) {
        primaryContactAddress = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.PRIMARY_CONTACT_PHONE_PARAM)) {
        primaryContactPhone = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.PRIMARY_CONTACT_DESCRIPTION_PARAM)) {
        primaryContactDescription = content;
      }
      content = "";
    }

    @Override
    public void startDocument() throws SAXException {
      content = "";
      key = "";
      organisationKey = "";
      name = "";
      nameLanguage = "";
      description = "";
      descriptionLanguage = "";
      logoURL = "";
      homepageURL = "";
      primaryContactName = "";
      primaryContactEmail = "";
      primaryContactType = "";
      primaryContactAddress = "";
      primaryContactPhone = "";
      primaryContactDescription = "";
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
      content = "";
    }
  }

  /**
   * Super simple SAX handler that parses all LegacyOrganizationResponse elements expected in an XML Response. The
   * resulting
   * string is concatenating all content and inserts a space at every element or attribute start.
   *
   * @see org.gbif.registry.ws.model.LegacyOrganizationResponse
   */
  public static class LegacyOrganizationResponseHandler extends DefaultHandler {

    private String content;
    public String key;
    public String organisationKey;
    public String name;
    public String nameLanguage;
    public String description;
    public String descriptionLanguage;
    public String nodeName;
    public String nodeKey;
    public String nodeContactEmail;
    public String homepageURL;
    public String primaryContactName;
    public String primaryContactEmail;
    public String primaryContactType;
    public String primaryContactAddress;
    public String primaryContactPhone;
    public String primaryContactDescription;

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      content += String.valueOf(ArrayUtils.subarray(ch, start, start + length));
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
      if (name.equalsIgnoreCase(LegacyResourceConstants.KEY_PARAM)) {
        key = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.ORGANIZATION_KEY_PARAM) || name
        .equalsIgnoreCase("organizationKey")) {
        organisationKey = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.NAME_PARAM)) {
        this.name = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.NAME_LANGUAGE_PARAM)) {
        nameLanguage = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.DESCRIPTION_PARAM)) {
        description = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.DESCRIPTION_LANGUAGE_PARAM)) {
        descriptionLanguage = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.NODE_CONTACT_EMAIL)) {
        nodeContactEmail = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.NODE_KEY_PARAM)) {
        nodeKey = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.NODE_NAME_PARAM)) {
        nodeName = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.HOMEPAGE_URL_PARAM)) {
        homepageURL = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.PRIMARY_CONTACT_NAME_PARAM)) {
        primaryContactName = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.PRIMARY_CONTACT_EMAIL_PARAM)) {
        primaryContactEmail = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.PRIMARY_CONTACT_TYPE_PARAM)) {
        primaryContactType = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.PRIMARY_CONTACT_ADDRESS_PARAM)) {
        primaryContactAddress = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.PRIMARY_CONTACT_PHONE_PARAM)) {
        primaryContactPhone = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.PRIMARY_CONTACT_DESCRIPTION_PARAM)) {
        primaryContactDescription = content;
      }
      content = "";
    }

    @Override
    public void startDocument() throws SAXException {
      content = "";
      key = "";
      organisationKey = "";
      name = "";
      nameLanguage = "";
      description = "";
      descriptionLanguage = "";
      nodeKey = "";
      nodeName = "";
      nodeContactEmail = "";
      homepageURL = "";
      primaryContactName = "";
      primaryContactEmail = "";
      primaryContactType = "";
      primaryContactAddress = "";
      primaryContactPhone = "";
      primaryContactDescription = "";
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
      content = "";
    }
  }

  /**
   * Super simple SAX handler parses all LegacyEndpointResponse elements expected in an XML Response. The resulting
   * string is concatenating all content and inserts a space at every element or attribute start.
   *
   * @see org.gbif.registry.ws.model.LegacyEndpointResponse
   */
  public static class LegacyEndpointResponseHandler extends DefaultHandler {

    private String content;
    public String key;
    public String resourceKey;
    public String organisationKey;
    public String type;
    public String accessPointURL;
    public String description;
    public String descriptionLanguage;
    public String typeDescription;

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      content += String.valueOf(ArrayUtils.subarray(ch, start, start + length));
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
      if (name.equalsIgnoreCase(LegacyResourceConstants.KEY_PARAM)) {
        key = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.ORGANIZATION_KEY_PARAM) || name
        .equalsIgnoreCase("organizationKey")) {
        organisationKey = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.RESOURCE_KEY_PARAM)) {
        resourceKey = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.TYPE_PARAM)) {
        type = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.DESCRIPTION_PARAM)) {
        description = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.ACCESS_POINT_URL_PARAM)) {
        accessPointURL = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.DESCRIPTION_LANGUAGE_PARAM)) {
        descriptionLanguage = content;
      } else if (name.equalsIgnoreCase(LegacyResourceConstants.TYPE_DESCRIPTION_PARAM)) {
        typeDescription = content;
      }
      content = "";
    }

    @Override
    public void startDocument() throws SAXException {
      content = "";
      key = "";
      organisationKey = "";
      description = "";
      resourceKey = "";
      accessPointURL = "";
      type = "";
      descriptionLanguage = "";
      typeDescription = "";
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
      content = "";
    }
  }
}
