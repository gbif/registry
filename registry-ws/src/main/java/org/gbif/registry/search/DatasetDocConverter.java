package org.gbif.registry.search;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.NetworkEntityService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.search.util.TimeSeriesExtractor;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.sun.jersey.api.NotFoundException;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * A utility builder to convert datasets into solr input documents.
 */
class DatasetDocConverter {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetDocConverter.class);

  private final DatasetService datasetService;
  private final NetworkEntityService<Organization> organizationService;
  private final NetworkEntityService<Installation> installationService;
  private final TimeSeriesExtractor timeSeriesExtractor = new TimeSeriesExtractor(1000, 2400, 1800, 2050);
  private final SAXParserFactory saxFactory = SAXParserFactory.newInstance();

  /**
   * Holds a map of {@link org.gbif.api.model.checklistbank.NameUsage} properties (Java fields) to Solr fields name.
   */
  private final Map<String, String> fieldPropertyMap = ImmutableMap.<String, String>builder()
    .put("key", "key")
    .put("dataset_title", "title")
    .put("description", "description")
    // enums
    .put("dataset_type", "type")
    .put("dataset_subtype", "subtype")
    .put("license", "license")
    .build();
  private final Set<String> enumFields = ImmutableSet.of("dataset_type", "dataset_subtype","license");

  public DatasetDocConverter(
    NetworkEntityService<Organization> organizationService, NetworkEntityService<Installation> installationService,
    DatasetService datasetService) {
    this.organizationService = organizationService;
    this.installationService = installationService;
    this.datasetService = datasetService;
  }

  /**
   * Creates a SolrAnnotatedDataset from the given dataset, copying only the relevant fields for Solr from
   * the given dataset.
   *
   * @param d The Dataset which will be copied into this object
   */
  public SolrInputDocument build(Dataset d) {
    SolrInputDocument doc = new SolrInputDocument();
    // Uses the pre-initialized field-property map to find the corresponding Solr field of a Java field.
    try {
      for (String field : fieldPropertyMap.keySet()) {
        String property = fieldPropertyMap.get(field);
        Object val = PropertyUtils.getProperty(d, property);
        if (val != null) {
          // enum values are stored as ordinal ints
          if (enumFields.contains(field)) {
            doc.addField(field, ((Enum) val).ordinal());
          } else {
            doc.addField(field, val);
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Error converting dataset {} to solr document", d.getKey(), e);
      throw new RuntimeException(e);
    }

    // keywords
    List<String> kw = Lists.newArrayList();
    for (Tag t : d.getTags()) {
      kw.add(t.getValue());
    }
    doc.addField("keyword", kw);

    // decade series
    List<Integer> decades = timeSeriesExtractor.extractDecades(d.getTemporalCoverages());
    doc.addField("decade", decades);

    // hosting org and publishing org title have to be retrieved via service calls
    indexOrgs(doc, d);

    // index entire metadata documents
    indexMetadata(doc, d);

    // TODO: populate country coverage and continents:
    // http://dev.gbif.org/issues/browse/POR-522

    return doc;
  }

  private void indexMetadata(SolrInputDocument doc, Dataset d) {
    InputStream stream = null;
    try {
      stream = datasetService.getMetadataDocument(d.getKey());

      if (stream != null) {
        FullTextSaxHandler handler = new FullTextSaxHandler();
        SAXParser p = saxFactory.newSAXParser();
        // parse does close the stream
        p.parse(stream, handler);
        doc.addField("metadata", handler.getFullText());
      }
    } catch (ParserConfigurationException e) {
      throw new IllegalStateException("XML Parser not working on this system", e);

    } catch (SAXException e) {
      LOG.warn("Cannot parse original metadata xml for dataset {}", d.getKey());

    } catch (Exception e) {
      LOG.error("Unable to index metadata document for dataset {}", d.getKey(), e);

    } finally {
      Closeables.closeQuietly(stream);
    }
  }

  private void indexOrgs(SolrInputDocument doc, Dataset d){
    // see http://dev.gbif.org/issues/browse/REG-405 which explains why we defend against NotFoundExceptions below

    Organization publisher = null;
    try {
      publisher = d.getPublishingOrganizationKey() != null ? organizationService.get(d.getPublishingOrganizationKey()) : null;
    } catch (NotFoundException e) {
      // server side, interceptors may trigger on a @nulltoNotFoundException which we code defensively for, but smells
      LOG.warn("Service reports organization[{}] cannot be found for dataset[{}]", d.getPublishingOrganizationKey(),
               d.getKey());
    }

    Installation installation = null;
    try {
      installation = d.getInstallationKey() != null ? installationService.get(d.getInstallationKey()) : null;
    } catch (NotFoundException e) {
      // server side, interceptors may trigger on a @nulltoNotFoundException which we code defensively for, but smells
      LOG.warn("Service reports installation[{}] cannot be found for dataset[{}]", d.getInstallationKey(), d.getKey());
    }

    Organization host = null;
    try {
      host = installation != null && installation.getOrganizationKey() != null ? organizationService.get(installation
                                                                                                           .getOrganizationKey()) : null;
    } catch (NotFoundException e) {
      // server side, interceptors may trigger on a @nulltoNotFoundException which we code defensively for, but smells
      LOG.warn("Service reports organization[{}] cannot be found for installation[{}]",
               installation.getOrganizationKey(), installation.getKey());
    }

    if (publisher != null) {
      doc.addField("publishing_organization_key", publisher.getKey().toString());
      doc.addField("publishing_organization_title", publisher.getTitle());
      if (publisher.getCountry() != null) {
        doc.addField("publishing_country", publisher.getCountry().ordinal());
      }
    } else {
      doc.addField("publishing_country", Country.UNKNOWN.ordinal());
    }
    if (host != null) {
      doc.addField("hosting_organization_key", host.getKey().toString());
      doc.addField("hosting_organization_title", host.getTitle());
    }

  }
}
