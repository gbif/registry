package org.gbif.registry.metadata;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.eml.temporal.DateRange;
import org.gbif.api.model.registry.eml.temporal.SingleDate;
import org.gbif.api.model.registry.eml.temporal.TemporalCoverage;
import org.gbif.api.model.registry.eml.temporal.VerbatimTimePeriod;
import org.gbif.api.util.formatter.TemporalCoverageFormatterVisitor;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.metadata.contact.ContactAdapter;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;

/**
 * Writer to serialize a Dataset as DublinCore XML document.
 * Currently using a OAI DC profile.
 *
 *
 */
public class DublinCoreWriter {

  public static final String ADDITIONAL_PROPERTY_OCC_COUNT = "occurrence_count";
  public static final String ADDITIONAL_PROPERTY_DC_FORMAT = "dublincore_format";

  private static final String DC_TEMPLATE = "oai-dc-profile-template/dc-dataset.ftl";
  private final Configuration freemarkerConfig;

  //We should probably use @Named("portal.url") but it would be more appropriate to wait
  //until we turn this class in a non static one.
  private static final String IDENTIFIER_PREFIX = "http://www.gbif.org/dataset/";
  private static final FastDateFormat FDF = DateFormatUtils.ISO_DATE_FORMAT;
  private static final TemporalCoverageFormatterVisitor TC_FORMATTER = new DcTemporalTemporalCoverageFormatter();

  /**
   * Private constructor, use {@link #newInstance()}
   * @param cfg
   */
  private DublinCoreWriter(Configuration cfg) {
    freemarkerConfig = cfg;
  }

  /**
   * Get a new instance of DublinCoreWriter with default Freemarker configuration.
   *
   * @return new instance
   */
  public static DublinCoreWriter newInstance(){
    return new DublinCoreWriter(DatasetXMLWriterConfigurationProvider.provideFreemarker());
  }

  /**
   * Write a DublinCore document from a Dataset object.
   *
   * @param organization organization who published this dataset, should not be null but nulls are handled.
   * @param dataset      non null dataset object
   * @param additionalProperties
   * @param writer       where the output document will go. The writer is not closed by this method.
   *
   * @throws IOException if an error occurs while processing the template
   */
  public void writeTo(@Nullable Organization organization, @NotNull Dataset dataset, Map<String,Object> additionalProperties, Writer writer) throws IOException {
    Preconditions.checkNotNull(dataset, "Dataset can't be null");
    Map<String, Object> map = Maps.newHashMap();
    map.put("dataset", dataset);
    map.put("dc", new DcDatasetWrapper(dataset, additionalProperties));

    if (organization != null) {
      map.put("organization", organization);
    }
    map = ImmutableMap.copyOf(map);
    try {
      freemarkerConfig.getTemplate(DC_TEMPLATE).process(map, writer);
    } catch (TemplateException e) {
      throw new IOException("Error while processing the DublinCore Freemarker template for dataset " + dataset.getKey(), e);
    }
  }

  /**
   * This class requires to be public to be used in the Freemarker template.
   */
  public static class DcDatasetWrapper {
    private final Dataset dataset;
    private final ContactAdapter contactAdapter;
    private final Map<String,Object> additionalProperties;

    public DcDatasetWrapper(Dataset dataset){
      this(dataset, null);
    }

    public DcDatasetWrapper(Dataset dataset, Map<String,Object> additionalProperties) {
      this.dataset = dataset;
      this.contactAdapter = new ContactAdapter(dataset.getContacts());
      this.additionalProperties = additionalProperties;
    }

    public String getFormat(){
      if(additionalProperties !=null && additionalProperties.containsKey(ADDITIONAL_PROPERTY_DC_FORMAT)){
        return (String)additionalProperties.get(ADDITIONAL_PROPERTY_DC_FORMAT);
      }
      return "";
    }

    /**
     * The following ordering will be respected: ADMINISTRATIVE_POINT_OF_CONTACT, METADATA_AUTHOR and ORIGINATOR
     * @return set of formatted contact name ordered by ContactType
     */
    public Set<String> getCreators() {
      Set<String> creators = Sets.newLinkedHashSet();
      List<Contact> filteredContacts = contactAdapter.getFilteredContacts(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT,
              ContactType.METADATA_AUTHOR, ContactType.ORIGINATOR);
      for(Contact contact : filteredContacts){
        creators.add(ContactAdapter.formatContactName(contact));
      }
      return creators;
    }

    /**
     * Get a list of "description" for DublinCore usage.
     * @return never null
     */
    public List<String> getDescription(){
      List<String> descriptions = Lists.newArrayList();
      if(StringUtils.isNotBlank(dataset.getDescription())){
        descriptions.add(dataset.getDescription());
      }
      return descriptions;
    }

    public Long getOccurrenceCount(){
      if(additionalProperties !=null && additionalProperties.containsKey(ADDITIONAL_PROPERTY_OCC_COUNT)){
        return (Long)additionalProperties.get(ADDITIONAL_PROPERTY_OCC_COUNT);
      }
      return null;
    }

    /**
     * Get a list of "coverage" formatted for DublinCore usage.
     *
     * @return never null
     */
    public List<String> getFormattedTemporalCoverage(){
      List<String> coverage = Lists.newArrayList();
      if(dataset.getTemporalCoverages() != null){
        for(TemporalCoverage tc : dataset.getTemporalCoverages()){
          coverage.add(tc.acceptFormatter(TC_FORMATTER));
        }
      }
      return coverage;
    }

    public String getIdentifier() {
      return IDENTIFIER_PREFIX + dataset.getKey().toString();
    }

    public Contact getResourceCreator() {
      return contactAdapter.getResourceCreator();
    }
  }

  /**
   * Implementation of TemporalCoverageFormatterVisitor for DublinCore
   */
  private static class DcTemporalTemporalCoverageFormatter implements TemporalCoverageFormatterVisitor {

    @Override
    public String format(DateRange dateRange) {
      String startDate = dateRange.getStart() != null ? FDF.format(dateRange.getStart()) : "";
      String endDate = dateRange.getEnd() != null ? FDF.format(dateRange.getEnd()) : "";
      return startDate + "/" + endDate;
    }

    @Override
    public String format(SingleDate singleDate) {
      if(singleDate.getDate() == null){
        return "";
      }
      return FDF.format(singleDate.getDate());
    }

    // unsupported
    @Override
    public String format(VerbatimTimePeriod verbatimTimePeriod) {
      return "";
    }
  }
}
