package org.gbif.registry.metadata;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.eml.temporal.TemporalCoverage;
import org.gbif.api.model.registry.eml.temporal.VerbatimTimePeriod;
import org.gbif.api.model.registry.eml.temporal.VerbatimTimePeriodType;
import org.gbif.api.vocabulary.ContactType;

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
  private static final Configuration FTL = DatasetXMLWriterConfigurationProvider.provideFreemarker(TEMPLATE_PATH);

  private EMLWriter() {
    // static utils class
  }

  /**
   * Wrapper for a dataset instance that exposes some very EML specific methods.
   * Mostly used for generating EML, see EMLWriter.
   */
  public static class EmlDatasetWrapper {

    private final Dataset dataset;

    public EmlDatasetWrapper(Dataset dataset) {
      this.dataset = dataset;
    }

    public List<Contact> getAssociatedParties() {
      List<Contact> contacts = Lists.newArrayList();
      for (Contact c : dataset.getContacts()) {
        if (!c.isPrimary()) {
          contacts.add(c);
        }
      }
      return contacts;
    }

    public Contact getResourceCreator() {
      return getFirstPreferredType(ContactType.ORIGINATOR);
    }

    public Contact getAdministrativeContact() {
      return getFirstPreferredType(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT);
    }

    public Contact getMetadataProvider() {
      return getFirstPreferredType(ContactType.METADATA_AUTHOR);
    }

    private Contact getFirstPreferredType(ContactType type) {
      Contact pref = null;
      for (Contact c : dataset.getContacts()) {
        if (type == c.getType()) {
          if (pref == null || c.isPrimary()) {
            pref = c;
          }
        }
      }
      return pref;
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
   * Write an EML document from a Dataset object.
   *
   * @param dataset non null dataset object
   * @param writer where the output document will go. The writer is not closed by this method.
   * @param useDoiAsIdentifier should the packageId be the dataset.doi? If true, the dataset.doi won't be included in the list of alternate identifiers.
   * @throws IOException if an error occurs while processing the template
   */
  public static void write(Dataset dataset, Writer writer, boolean useDoiAsIdentifier) throws IOException {
    Preconditions.checkNotNull(dataset, "Dataset can't be null");

    Map<String, Object> map = ImmutableMap.of
            ("dataset", dataset, "eml", new EmlDatasetWrapper(dataset), "useDoiAsIdentifier", useDoiAsIdentifier);

    try {
      FTL.getTemplate(EML_TEMPLATE).process(map, writer);
    } catch (TemplateException e) {
      throw new IOException("Error while processing the EML Freemarker template for dataset " + dataset.getKey(), e);
    }
  }

}
