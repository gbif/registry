package org.gbif.registry.search;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestResult;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetSubtype;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.search.util.TimeSeriesExtractor;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * A utility builder to convert datasets into solr input documents.
 */
class DatasetDocConverter {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetDocConverter.class);

  private final TimeSeriesExtractor timeSeriesExtractor = new TimeSeriesExtractor(1000, 2400, 1800, 2050);
  private final SAXParserFactory saxFactory = SAXParserFactory.newInstance();

  /**
   * Creates a SolrAnnotatedDataset from the given dataset, copying only the relevant fields for Solr from
   * the given dataset.
   *
   * @param d The Dataset which will be copied into this object
   */
  public SolrInputDocument build(Dataset d, @Nullable InputStream metadataXml, @Nullable Organization publisher, @Nullable Organization host) {
    SolrInputDocument doc = new SolrInputDocument();

    try {
      doc.addField("key", fromUUID(d.getKey()));
      doc.addField("title", d.getTitle());
      doc.addField("type", fromEnum(d.getType()));
      doc.addField("subtype", fromEnum(d.getSubtype()));
      doc.addField("description", d.getDescription());
      doc.addField("license", fromEnum(d.getLicense()));

      if (publisher != null) {
        doc.addField("publishing_organization_key", fromUUID(publisher.getKey()));
        doc.addField("publishing_organization_title", publisher.getTitle());
        doc.addField("publishing_country", fromEnum(publisher.getCountry()));
      } else {
        doc.addField("publishing_country", fromEnum(Country.UNKNOWN));
      }

      if (host != null) {
        doc.addField("hosting_organization_key", fromUUID(host.getKey()));
        doc.addField("hosting_organization_title", host.getTitle());
      }

      if (d.getProject() != null && d.getProject().getIdentifier() != null) {
        doc.addField("project_id", d.getProject().getIdentifier());
      }

      doc.addField("country_coverage", fromEnums(d.getCountryCoverage()));

      // keywords - use both registry tags and EML keywords
      Set<String> kw = Sets.newHashSet();
      for (Tag t : d.getTags()) {
        kw.add(t.getValue());
      }
      for (KeywordCollection kc : d.getKeywordCollections()) {
        kw.addAll(kc.getKeywords());
      }
      doc.addField("keyword", kw);

      // decade series
      List<Integer> decades = timeSeriesExtractor.extractDecades(d.getTemporalCoverages());
      doc.addField("decade", decades);

      // index entire metadata documents
      indexMetadata(doc, d.getKey(), metadataXml);

    } catch (Exception e) {
      LOG.error("Error converting dataset {} to solr document", d.getKey(), e);
      throw new RuntimeException(e);
    }

    return doc;
  }

  public DatasetSearchResult toSearchResult(SolrDocument doc) {
    DatasetSearchResult d = new DatasetSearchResult();

    d.setKey(toUUID(doc.getFieldValue("key")));
    d.setTitle(str(doc.getFieldValue("title")));
    d.setType(toEnum(DatasetType.class, (Integer) doc.getFieldValue("type")));
    d.setSubtype(toEnum(DatasetSubtype.class, (Integer) doc.getFieldValue("subtype")));
    d.setPublishingOrganizationKey(toUUID(doc.getFieldValue("publishing_organization_key")));
    d.setPublishingOrganizationTitle(str(doc.getFieldValue("publishing_organization_title")));
    d.setPublishingCountry(toEnum(Country.class, (Integer)doc.getFieldValue("publishing_country")));
    d.setHostingOrganizationKey(toUUID(doc.getFieldValue("hosting_organization_key")));
    d.setHostingOrganizationTitle(str(doc.getFieldValue("hosting_organization_title")));
    d.setDescription(str(doc.getFieldValue("description")));
    d.setDecades(ints(doc, "decade"));
    d.setCountryCoverage(enums(Country.class, doc, "country_coverage"));
    d.setKeywords(strings(doc, "keyword"));
    d.setLicense(toEnum(License.class, (Integer)doc.getFieldValue("license")));
    d.setProjectIdentifier(str("project_id"));
    d.setRecordCount((Integer)doc.getFieldValue("record_count"));

    return d;
  }

  public DatasetSuggestResult toSuggestResult(SolrDocument doc) {
    DatasetSuggestResult d = new DatasetSuggestResult();

    d.setKey(toUUID(doc.getFieldValue("key")));
    d.setTitle(str(doc.getFieldValue("title")));
    d.setType(toEnum(DatasetType.class, (Integer) doc.getFieldValue("type")));
    d.setSubtype(toEnum(DatasetSubtype.class, (Integer) doc.getFieldValue("subtype")));
    d.setDescription(str(doc.getFieldValue("description")));

    return d;
  }

  private void indexMetadata(SolrInputDocument doc, UUID key, InputStream stream) {
    try {
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
      LOG.warn("Cannot parse original metadata xml for dataset {}", key);

    } catch (Exception e) {
      LOG.error("Unable to index metadata document for dataset {}", key, e);

    } finally {
      Closeables.closeQuietly(stream);
    }
  }

  private static String str(@Nullable Object obj) {
    return obj == null ? null : obj.toString();
  }

  private static Integer fromEnum(Enum value) {
    if (value != null) {
      return value.ordinal();
    }
    return null;
  }

  private static Set<Integer> fromEnums(Iterable<? extends Enum> data) {
    Set<Integer>  ints = Sets.newHashSet();
    if (data != null) {
      for (Enum en : data) {
        if (en != null) {
          ints.add(fromEnum(en));
        }
      }
    }
    return ints;
  }

  private static <T extends Enum<?>> T toEnum(SolrDocument doc, Class<T> vocab, String field) {
    return toEnum(vocab, (Integer) doc.getFieldValue(field));
  }

  private static <T extends Enum<?>> T toEnum(Class<T> vocab, Integer ordinal) {
    if (ordinal != null) {
      T[] values = vocab.getEnumConstants();
      return values[ordinal];
    }
    return null;
  }

  private static String fromUUID(UUID value) {
    if (value != null) {
      return value.toString();
    }
    return null;
  }

  private static UUID toUUID(Object value) {
    if (value != null) {
      return UUID.fromString(value.toString());
    }
    return null;
  }

  private static List<String> strings(SolrDocument doc, String field) {
    List<String> data = Lists.newArrayList();
    if (doc.getFieldValues(field) != null) {
      for (Object val : doc.getFieldValues(field)) {
        if (val != null) {
          data.add((String) val);
        }
      }
    }
    return data;
  }

  private static List<Integer> ints(SolrDocument doc, String field) {
    List<Integer> data = Lists.newArrayList();
    if (doc.getFieldValues(field) != null) {
      for (Object val : doc.getFieldValues(field)) {
        if (val != null) {
          data.add((Integer) val);
        }
      }
    }
    return data;
  }

  private static <T extends Enum<?>> Set<T> enums(Class<T> vocab, SolrDocument doc, String field) {
    Set<T> data = Sets.newHashSet();
    if (doc.getFieldValues(field) != null) {
      for (Object val : doc.getFieldValues(field)) {
        data.add(vocab.getEnumConstants()[(Integer) val]);
      }
    }
    return data;
  }

}
