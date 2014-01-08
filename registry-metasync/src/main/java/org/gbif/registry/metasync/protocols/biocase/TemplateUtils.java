package org.gbif.registry.metasync.protocols.biocase;

import org.gbif.registry.metasync.util.Constants;

import java.io.StringWriter;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

/**
 * Utility class used by the BioCASe synchroniser to load the various templates from the classpath and fill them with
 * the proper variables.
 */
final class TemplateUtils {

  private static final VelocityEngine VELOCITY_ENGINE = new VelocityEngine();

  static {
    Properties properties = new Properties();
    properties.setProperty("file.resource.loader.class",
                           "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    VELOCITY_ENGINE.init(properties);
  }

  /**
   * Returns a string that can be used to retrieve an "old style" inventory from a BioCASe Endpoint using a {@code scan}
   * request.
   */
  static String getBiocaseInventoryRequest(String contentNamespace) {
    Context context = new VelocityContext();
    context.put("contentNamespace", contentNamespace);
    context.put("concept", getTitlePath(contentNamespace));

    StringWriter writer = new StringWriter();
    VELOCITY_ENGINE.mergeTemplate("biocase/inventory.xml.vm", "UTF-8", context, writer);
    return writer.toString();
  }

  /**
   * Returns a string that can be used to retrieve metadata about a specific dataset identified by a title. You get that
   * title by first doing an inventory request. Under tho hood this does a {@code search} request.
   */
  public static String getBiocaseMetadataRequest(String contentNamespace, String datasetTitle) {
    Context context = new VelocityContext();
    context.put("contentNamespace", contentNamespace);
    context.put("datasetTitle", datasetTitle);
    context.put("titleConcept", getTitlePath(contentNamespace));

    if (contentNamespace.equals(Constants.ABCD_12_SCHEMA)) {
      context.put("nameConcept",
                  "/DataSets/DataSet/Units/Unit/Identifications/Identification/TaxonIdentified/NameAuthorYearString");
    } else if (contentNamespace.equals(Constants.ABCD_206_SCHEMA)) {
      context.put("nameConcept",
                  "/DataSets/DataSet/Units/Unit/Identifications/Identification/Result/TaxonIdentified/ScientificName/FullScientificNameString");
    }

    StringWriter writer = new StringWriter();
    VELOCITY_ENGINE.mergeTemplate("/biocase/metadata.xml.vm", "UTF-8", context, writer);
    return writer.toString();
  }

  private static String getTitlePath(String contentNamespace) {
    if (contentNamespace.equals(Constants.ABCD_12_SCHEMA)) {
      return "/DataSets/DataSet/OriginalSource/SourceName";
    }
    if (contentNamespace.equals(Constants.ABCD_206_SCHEMA)) {
      return "/DataSets/DataSet/Metadata/Description/Representation/Title";
    }
    return null;
  }

  private TemplateUtils() {
    throw new UnsupportedOperationException("Can't initialize class");
  }

}

