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
package org.gbif.registry.metasync.protocols.biocase;

import org.gbif.registry.metasync.util.Constants;

import java.io.StringWriter;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

/**
 * Utility class used by the BioCASe synchroniser to load the various templates from the classpath
 * and fill them with the proper variables.
 */
final class TemplateUtils {

  private static final String ABCD_12_NAME_CONCEPT =
      "/DataSets/DataSet/Units/Unit/Identifications/Identification/TaxonIdentified/NameAuthorYearString";
  private static final String ABCD_206_NAME_CONCEPT =
      "/DataSets/DataSet/Units/Unit/Identifications/Identification/Result/TaxonIdentified/ScientificName/FullScientificNameString";

  private static final VelocityEngine VELOCITY_ENGINE = new VelocityEngine();

  static {
    Properties properties = new Properties();
    properties.setProperty(
        "file.resource.loader.class",
        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    VELOCITY_ENGINE.init(properties);
  }

  /**
   * Returns a string that can be used to retrieve an "old style" inventory from a BioCASe Endpoint
   * using a {@code scan} request.
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
   * Returns a string that can be used to retrieve metadata about a specific dataset identified by a
   * title. You get that title by first doing an inventory request. Under the hood this does a
   * {@code search} request.
   */
  public static String getBiocaseMetadataRequest(String contentNamespace, String datasetTitle) {
    Context context = new VelocityContext();
    context.put("contentNamespace", contentNamespace);
    context.put("datasetTitle", datasetTitle);
    context.put("titleConcept", getTitlePath(contentNamespace));

    if (contentNamespace.equals(Constants.ABCD_12_SCHEMA)) {
      context.put("nameConcept", ABCD_12_NAME_CONCEPT);
    } else if (contentNamespace.equals(Constants.ABCD_206_SCHEMA)) {
      context.put("nameConcept", ABCD_206_NAME_CONCEPT);
    }

    StringWriter writer = new StringWriter();
    VELOCITY_ENGINE.mergeTemplate("/biocase/metadata.xml.vm", "UTF-8", context, writer);
    return writer.toString();
  }

  /**
   * Returns a string that can be used to retrieve metadata about a specific dataset identified by a
   * title. You get that title by first doing an inventory request. Under the hood this does a
   * {@code search} request.
   */
  public static String getBiocaseCountRequest(String contentNamespace, String datasetTitle) {
    Context context = new VelocityContext();
    context.put("contentNamespace", contentNamespace);
    context.put("datasetTitle", datasetTitle);
    context.put("titleConcept", getTitlePath(contentNamespace));

    if (contentNamespace.equals(Constants.ABCD_12_SCHEMA)) {
      context.put("nameConcept", ABCD_12_NAME_CONCEPT);
    } else if (contentNamespace.equals(Constants.ABCD_206_SCHEMA)) {
      context.put("nameConcept", ABCD_206_NAME_CONCEPT);
    }

    StringWriter writer = new StringWriter();
    VELOCITY_ENGINE.mergeTemplate("/biocase/count.xml.vm", "UTF-8", context, writer);
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
