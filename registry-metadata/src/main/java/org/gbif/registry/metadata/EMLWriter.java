package org.gbif.registry.metadata;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.eml.temporal.TemporalCoverage;
import org.gbif.api.model.registry.eml.temporal.VerbatimTimePeriod;
import org.gbif.api.model.registry.eml.temporal.VerbatimTimePeriodType;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.metadata.contact.ContactAdapter;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

/**
 * A simple tool to serialize a dataset object into an EML GBIF profile compliant xml document.
 */
public class EMLWriter {

  private static final String TEMPLATE_PATH = "/gbif-eml-profile-template";
  private static final String EML_TEMPLATE = String.format("eml-dataset-%s.ftl", EMLProfileVersion.GBIF_1_0_2.getVersion());
  private final Configuration freemarkerConfig;
  private final boolean useDoiAsIdentifier;

  private static final Configuration FTL = DatasetXMLWriterConfigurationProvider.provideFreemarker(TEMPLATE_PATH);

  /**
   * Private constructor, use {@link #newInstance()}
   * @param cfg
   */
  private EMLWriter(Configuration cfg, boolean useDoiAsIdentifier) {
    this.freemarkerConfig = cfg;
    this.useDoiAsIdentifier = useDoiAsIdentifier;
  }

  /**
   * Get a new instance of EMLWriter with default Freemarker configuration.
   * Same as calling {@link #newInstance(boolean)} method with useDoiAsIdentifier = false
   *
   * @return new instance
   */
  public static EMLWriter newInstance(){
    return new EMLWriter(DatasetXMLWriterConfigurationProvider.provideFreemarker(), false);
  }

  /**
   * Get a new instance of EMLWriter with default Freemarker configuration.
   *
   * @param useDoiAsIdentifier should the packageId be the dataset.doi? If true, the dataset.doi won't be included in the list of alternate identifiers
   * @return
   */
  public static EMLWriter newInstance(boolean useDoiAsIdentifier){
    return new EMLWriter(DatasetXMLWriterConfigurationProvider.provideFreemarker(), useDoiAsIdentifier);
  }

  /**
   * Write a document from a Dataset object.
   *
   * @param dataset non null dataset object
   * @param writer where the output document will go. The writer is not closed by this method.
   * @throws IOException if an error occurs while processing the template
   */
  public void writeTo(Dataset dataset, Writer writer) throws IOException {
    write(dataset, writer, useDoiAsIdentifier);
  }


  /**
   * Wrapper for a dataset instance that exposes some very EML specific methods.
   * Mostly used for generating EML, see EMLWriter.
   * This class requires to be public to be used in the Freemarker template.
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

    public Contact getAdministrativeContact() {
      return contactAdapter.getAdministrativeContact();
    }

    public Contact getMetadataProvider() {
      return contactAdapter.getFirstPreferredType(ContactType.METADATA_AUTHOR);
    }

    public List getFormationPeriods() {
      return getTimePeriods(VerbatimTimePeriodType.FORMATION_PERIOD);
    }

    public List getLivingTimePeriods() {
      return getTimePeriods(VerbatimTimePeriodType.LIVING_TIME_PERIOD);
    }

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

  }

  /**
   * This method will be deprecated soon, please use an instance {@link #newInstance()}
   *
   * Same as calling {@link #write(Dataset, Writer, boolean) write} method with useDoiAsIdentifier = false.
   *
   * @param dataset
   * @param writer
   * @throws IOException
   */
  public static void write(Dataset dataset, Writer writer) throws IOException {
    write(dataset,writer,false);
  }

  /**
   * This method will be deprecated soon, please use an instance {@link #newInstance()}
   *
   * Write an EML document from a Dataset object.
   *
   * @param dataset non null dataset object
   * @param writer where the output document will go. The writer is not closed by this method.
   * @param _useDoiAsIdentifier should the packageId be the dataset.doi? If true, the dataset.doi won't be included in the list of alternate identifiers.
   * @throws IOException if an error occurs while processing the template
   */
  public static void write(Dataset dataset, Writer writer, boolean _useDoiAsIdentifier) throws IOException {
    Preconditions.checkNotNull(dataset, "Dataset can't be null");

    Map<String, Object> map = ImmutableMap.of
            ("dataset", dataset, "eml", new EmlDatasetWrapper(dataset), "useDoiAsIdentifier", _useDoiAsIdentifier);

    try {
      FTL.getTemplate(EML_TEMPLATE).process(map, writer);
    } catch (TemplateException e) {
      throw new IOException("Error while processing the EML Freemarker template for dataset " + dataset.getKey(), e);
    }
  }

}
