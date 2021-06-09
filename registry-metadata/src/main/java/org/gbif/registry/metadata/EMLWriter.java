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
package org.gbif.registry.metadata;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.eml.temporal.DateRange;
import org.gbif.api.model.registry.eml.temporal.SingleDate;
import org.gbif.api.model.registry.eml.temporal.TemporalCoverage;
import org.gbif.api.model.registry.eml.temporal.VerbatimTimePeriod;
import org.gbif.api.model.registry.eml.temporal.VerbatimTimePeriodType;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.metadata.contact.ContactAdapter;
import org.gbif.registry.metadata.parse.ParagraphContainer;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

/**
 * A simple tool to serialize a dataset object into an XML document compliant with the latest
 * version of the GBIF Metadata Profile, currently version 1.1.
 */
@ThreadSafe
public class EMLWriter {

  private static final String TEMPLATE_PATH = "/gbif-eml-profile-template";
  private static final String EML_TEMPLATE =
      String.format("eml-dataset-%s.ftl", EMLProfileVersion.GBIF_1_1.getVersion());
  private final Configuration freemarkerConfig;
  private final boolean useDoiAsIdentifier;
  private final boolean omitXmlDeclaration;

  // we keep to instances until we remove the 2 deprecated static methods
  private static final EMLWriter LEGACY_METHOD_INSTANCE_NO_DOI = newInstance();
  private static final EMLWriter LEGACY_METHOD_INSTANCE_WITH_DOI = newInstance(true);

  /**
   * Private constructor, use {@link #newInstance()}
   *
   * @param cfg
   */
  private EMLWriter(Configuration cfg, boolean useDoiAsIdentifier, boolean omitXmlDeclaration) {
    this.freemarkerConfig = cfg;
    this.useDoiAsIdentifier = useDoiAsIdentifier;
    this.omitXmlDeclaration = omitXmlDeclaration;
  }

  /**
   * Get a new instance of EMLWriter with default Freemarker configuration. Same as calling {@link
   * #newInstance(boolean)} method with useDoiAsIdentifier = false
   *
   * @return new instance
   */
  public static EMLWriter newInstance() {
    return new EMLWriter(
        DatasetXMLWriterConfigurationProvider.provideFreemarker(TEMPLATE_PATH), false, false);
  }

  /**
   * Get a new instance of EMLWriter with default Freemarker configuration.
   *
   * @param useDoiAsIdentifier should the packageId be the dataset.doi? If true, the dataset.doi
   *     won't be included in the list of alternate identifiers
   * @return
   */
  public static EMLWriter newInstance(boolean useDoiAsIdentifier) {
    return new EMLWriter(
        DatasetXMLWriterConfigurationProvider.provideFreemarker(TEMPLATE_PATH),
        useDoiAsIdentifier,
        false);
  }

  /**
   * Get a new instance of EMLWriter with default Freemarker configuration.
   *
   * @param useDoiAsIdentifier should the packageId be the dataset.doi? If true, the dataset.doi
   *     won't be included in the list of alternate identifiers
   * @param omitXmlDeclaration should the XML declaration be omitted in the generated document
   * @return
   */
  public static EMLWriter newInstance(boolean useDoiAsIdentifier, boolean omitXmlDeclaration) {
    return new EMLWriter(
        DatasetXMLWriterConfigurationProvider.provideFreemarker(TEMPLATE_PATH),
        useDoiAsIdentifier,
        omitXmlDeclaration);
  }

  /**
   * Write a document from a Dataset object.
   *
   * @param dataset non null dataset object
   * @param writer where the output document will go. The writer is not closed by this method.
   * @throws IOException if an error occurs while processing the template
   */
  public void writeTo(Dataset dataset, Writer writer) throws IOException {
    innerWrite(dataset, writer);
  }

  private void innerWrite(Dataset dataset, Writer writer) throws IOException {
    Preconditions.checkNotNull(dataset, "Dataset can't be null");

    Map<String, Object> map =
        ImmutableMap.of(
            "dataset",
            dataset,
            "eml",
            new EmlDatasetWrapper(dataset),
            "useDoiAsIdentifier",
            useDoiAsIdentifier,
            "omitXmlDeclaration",
            omitXmlDeclaration);

    try {
      freemarkerConfig.getTemplate(EML_TEMPLATE).process(map, writer);
    } catch (TemplateException e) {
      throw new IOException(
          "Error while processing the EML Freemarker template for dataset " + dataset.getKey(), e);
    }
  }

  /**
   * Wrapper for a dataset instance that exposes some EML specific methods. Mostly used for
   * generating EML, see EMLWriter. This class requires to be public to be used in the Freemarker
   * template.
   */
  public static class EmlDatasetWrapper {

    private final Dataset dataset;
    private final ContactAdapter contactAdapter;

    public EmlDatasetWrapper(Dataset dataset) {
      this.dataset = dataset;
      this.contactAdapter = new ContactAdapter(dataset.getContacts());
    }

    public List<Contact> getAssociatedParties() {
      return contactAdapter.getAssociatedParties();
    }

    public Contact getResourceCreator() {
      return contactAdapter.getResourceCreator();
    }

    /** @return list of {@link Contact} of type ContactType.ORIGINATOR */
    public List<Contact> getCreators() {
      return contactAdapter.getCreators();
    }

    public Contact getAdministrativeContact() {
      return contactAdapter.getAdministrativeContact();
    }

    /** @return list of {@link Contact} of type ContactType.ADMINISTRATIVE_POINT_OF_CONTACT */
    public List<Contact> getContacts() {
      return contactAdapter.getContacts();
    }

    public List<String> getDescription() {
      return new ParagraphContainer(dataset.getDescription()).getParagraphs();
    }

    public Contact getMetadataProvider() {
      return contactAdapter.getFirstPreferredType(ContactType.METADATA_AUTHOR);
    }

    /** @return list of {@link Contact} of type ContactType.METADATA_AUTHOR */
    public List<Contact> getMetadataProviders() {
      return contactAdapter.getMetadataProviders();
    }

    /**
     * @return list of all formation periods {@link VerbatimTimePeriodType} of type
     *     VerbatimTimePeriodType.FORMATION_PERIOD
     */
    public List<VerbatimTimePeriod> getFormationPeriods() {
      return getTimePeriods(VerbatimTimePeriodType.FORMATION_PERIOD);
    }

    /**
     * @return list of all formation periods {@link VerbatimTimePeriodType} of type
     *     VerbatimTimePeriodType.LIVING_TIME_PERIOD
     */
    public List<VerbatimTimePeriod> getLivingTimePeriods() {
      return getTimePeriods(VerbatimTimePeriodType.LIVING_TIME_PERIOD);
    }

    /** @return list of all {@link VerbatimTimePeriodType} of specified type */
    private List<VerbatimTimePeriod> getTimePeriods(VerbatimTimePeriodType type) {
      List<VerbatimTimePeriod> periods = Lists.newArrayList();
      for (TemporalCoverage tc : dataset.getTemporalCoverages()) {
        if (tc instanceof VerbatimTimePeriod) {
          VerbatimTimePeriod tp = (VerbatimTimePeriod) tc;
          if (type.equals(tp.getType())) {
            periods.add(tp);
          }
        }
      }
      return periods;
    }

    /**
     * @return list of all {@link SingleDate} and {@link DateRange} {@link TemporalCoverage} or an
     *     empty list if none found
     */
    public List<TemporalCoverage> getSingleDateAndDateRangeCoverages() {
      List<TemporalCoverage> periods = Lists.newArrayList();
      for (TemporalCoverage tc : dataset.getTemporalCoverages()) {
        if (tc instanceof DateRange || tc instanceof SingleDate) {
          periods.add(tc);
        }
      }
      return periods;
    }
  }

  /**
   * @param dataset
   * @param writer
   * @throws IOException
   * @deprecated, please use an instance {@link #newInstance()}
   *     <p>Same as calling {@link #write(Dataset, Writer, boolean) write} method with
   *     useDoiAsIdentifier = false.
   */
  @Deprecated
  public static void write(Dataset dataset, Writer writer) throws IOException {
    write(dataset, writer, false);
  }

  /**
   * @param dataset non null dataset object
   * @param writer where the output document will go. The writer is not closed by this method.
   * @param useDoiAsIdentifier should the packageId be the dataset.doi? If true, the dataset.doi
   *     won't be included in the list of alternate identifiers.
   * @throws IOException if an error occurs while processing the template
   * @deprecated, please use an instance {@link #newInstance()}
   *     <p>Write an EML document from a Dataset object.
   */
  @Deprecated
  public static void write(Dataset dataset, Writer writer, boolean useDoiAsIdentifier)
      throws IOException {
    Preconditions.checkNotNull(dataset, "Dataset can't be null");

    if (useDoiAsIdentifier) {
      LEGACY_METHOD_INSTANCE_WITH_DOI.writeTo(dataset, writer);
    } else {
      LEGACY_METHOD_INSTANCE_NO_DOI.writeTo(dataset, writer);
    }
  }
}
