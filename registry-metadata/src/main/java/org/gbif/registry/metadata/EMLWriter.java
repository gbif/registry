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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;

/**
 * A simple tool to serialize a dataset object into an EML GBIF profile compliant xml document.
 */
public class EMLWriter {

  private static final String TEMPLATE_PATH = "/gbif-eml-profile-template";
  private static final String EML_TEMPLATE = "eml-dataset.ftl";
  private static final Configuration FTL = provideFreemarker();

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
   * Provides a freemarker template loader. It is configured to access the utf8 templates folder on the classpath, i.e.
   * /src/resources/templates
   */
  private static Configuration provideFreemarker() {
    // load templates from classpath by prefixing /templates
    TemplateLoader tl = new ClassTemplateLoader(EMLWriter.class, TEMPLATE_PATH);

    Configuration fm = new Configuration();
    fm.setDefaultEncoding("utf8");
    fm.setTemplateLoader(tl);

    return fm;
  }

  public static void write(Dataset dataset, Writer writer) throws IOException {
    write(dataset,writer,false);
  }

  /**
   * Creates an EML which packageId is the dataset.doi.
   * The dataset.doi won't be included in the list of alternate identifiers.
   */
  public static void write(Dataset dataset, Writer writer, boolean useDoiAsIdentifier) throws IOException {
    if (dataset == null) {
      throw new IllegalArgumentException("Dataset can't be null");
    }

    Map<String, Object> map = Maps.newHashMap();
    map.put("dataset", dataset);
    map.put("eml", new EmlDatasetWrapper(dataset));
    map.put("useDoiAsIdentifier", useDoiAsIdentifier);


    try {
      FTL.getTemplate(EML_TEMPLATE).process(map, writer);
    } catch (TemplateException e) {
      throw new IOException("Error while processing the EML freemarker template for dataset " + dataset.getKey(), e);
    }
  }

}
