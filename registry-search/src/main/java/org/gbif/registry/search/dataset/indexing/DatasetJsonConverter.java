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
package org.gbif.registry.search.dataset.indexing;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.occurrence.Occurrence;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.model.occurrence.search.OccurrenceSearchRequest;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.search.dataset.indexing.checklistbank.ChecklistbankPersistenceService;
import org.gbif.registry.search.dataset.indexing.ws.GbifWsClient;
import org.gbif.registry.search.dataset.indexing.ws.JacksonObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Lazy
public class DatasetJsonConverter {

  private static final int MAX_FACET_LIMIT = 1200000;

  // Collections
  private static final String PROCESSING_NAMESPACE = "processing.gbif.org";
  private static final String INSTITUTION_TAG_NAME = "institutionCode";
  private static final String COLLECTION_TAG_NAME = "collectionCode";

  // Gridded datasets
  private static final String GRIDDED_DATASET_NAMESPACE = "griddedDataSet.jwaller.gbif.org";
  private static final String GRIDDED_DATASET_NAME = "griddedDataset";

  private final SAXParserFactory saxFactory = SAXParserFactory.newInstance();
  private final TimeSeriesExtractor timeSeriesExtractor =
      new TimeSeriesExtractor(1000, 2400, 1800, 2050);

  private final List<Consumer<ObjectNode>> consumers = new ArrayList<>();

  private ChecklistbankPersistenceService checklistbankPersistenceService;

  private final GbifWsClient gbifWsClient;

  private final ObjectMapper mapper;

  private final RestHighLevelClient occurrenceEsClient;

  private final String occurrenceIndex;

  private Long occurrenceCount;

  private Long nameUsagesCount;

  private Long getOccurrenceCount() {
    if (occurrenceCount == null) {
      occurrenceCount = gbifWsClient.getOccurrenceRecordCount();
    }
    return Optional.ofNullable(occurrenceCount).orElse(1L);
  }

  private Long getNameUsagesCount() {
    if (nameUsagesCount == null) {
      nameUsagesCount = gbifWsClient.speciesSearch(new NameUsageSearchRequest(0, 0)).getCount();
    }
    return Optional.ofNullable(nameUsagesCount).orElse(1L);
  }

  @Autowired
  private DatasetJsonConverter(
      GbifWsClient gbifWsClient,
      @Autowired(required = false) ChecklistbankPersistenceService checklistbankPersistenceService,
      @Qualifier("apiMapper") ObjectMapper mapper,
      @Qualifier("occurrenceEsClient") RestHighLevelClient occurrenceEsClient,
      @Value("${elasticsearch.occurrence.index}") String occurrenceIndex) {
    this.gbifWsClient = gbifWsClient;
    this.checklistbankPersistenceService = checklistbankPersistenceService;
    this.mapper = mapper;
    this.occurrenceEsClient = occurrenceEsClient;
    this.occurrenceIndex = occurrenceIndex;
    consumers.add(this::metadataConsumer);
    consumers.add(this::addTitles);
    consumers.add(this::enumTransforms);
    consumers.add(this::addOccurrenceSpeciesCounts);
  }

  public static DatasetJsonConverter create(
      GbifWsClient gbifWsClient,
      ChecklistbankPersistenceService checklistbankPersistenceService,
      RestHighLevelClient occurrenceEsClient,
      String occurrenceIndex) {
    return new DatasetJsonConverter(
        gbifWsClient,
        checklistbankPersistenceService,
        JacksonObjectMapper.get(),
        occurrenceEsClient,
        occurrenceIndex);
  }

  public ObjectNode convert(Dataset dataset) {
    ObjectNode datasetAsJson = mapper.valueToTree(dataset);
    consumers.forEach(c -> c.accept(datasetAsJson));
    addDecades(dataset, datasetAsJson);
    addKeyword(dataset, datasetAsJson);
    addCountryCoverage(dataset, datasetAsJson);
    if (checklistbankPersistenceService != null) {
      addTaxonKeys(dataset, datasetAsJson);
    }
    addMachineTags(dataset, datasetAsJson);
    // addOccurrenceCoverage(dataset, datasetAsJson);
    return datasetAsJson;
  }

  @SneakyThrows
  public String convertAsJsonString(Dataset dataset) {
    return mapper.writeValueAsString(convert(dataset));
  }

  private void metadataConsumer(ObjectNode dataset) {
    try (InputStream stream =
        gbifWsClient.getMetadataDocument(UUID.fromString(dataset.get("key").asText()))) {
      if (stream != null) {
        FullTextSaxHandler handler = new FullTextSaxHandler();
        SAXParser p = saxFactory.newSAXParser();
        // parse does close the stream
        p.parse(stream, handler);
        dataset.put("metadata", handler.getFullText());
      }
    } catch (ParserConfigurationException e) {
      throw new IllegalStateException("XML Parser not working on this system", e);
    } catch (SAXException e) {
      log.warn("Cannot parse original metadata xml for dataset {}", dataset);
    } catch (Exception e) {
      log.error("Unable to index metadata document for dataset {}", dataset, e);
    }
  }

  private void addTitles(ObjectNode dataset) {
    if (dataset.has("title")) {
      dataset.put("titleAutocomplete", dataset.get("title").asText());
    }
    if (dataset.has("installationKey")) {
      Installation installation =
          gbifWsClient.getInstallation(dataset.get("installationKey").asText());
      if (Objects.nonNull(installation)) {
        dataset.put("installationTitle", installation.getTitle());
        dataset.put("installationTitleAutocomplete", installation.getTitle());
        if (Objects.nonNull(installation.getOrganizationKey())) {
          Organization hostingOrg =
              gbifWsClient.getOrganization(installation.getOrganizationKey().toString());
          if (Objects.nonNull(hostingOrg)) {
            dataset.put("hostingOrganizationKey", hostingOrg.getKey().toString());
            dataset.put("hostingOrganizationTitle", hostingOrg.getTitle());
            dataset.put("hostingOrganizationTitleAutocomplete", hostingOrg.getTitle());
          }
        }
      }
    }
    if (dataset.has("publishingOrganizationKey")) {
      Organization publisher =
          gbifWsClient.getOrganization(dataset.get("publishingOrganizationKey").asText());
      if (Objects.nonNull(publisher)) {
        dataset.put("publishingOrganizationTitle", publisher.getTitle());
        dataset.put("publishingOrganizationTitleAutocomplete", publisher.getTitle());
        if (Objects.nonNull(publisher.getCountry())) {
          dataset.put("publishingCountry", publisher.getCountry().getIso2LetterCode());
        }
      } else {
        dataset.put("publishingCountry", Country.UNKNOWN.getIso2LetterCode());
      }
    }
  }

  private void addRecordCounts(ObjectNode dataset, Long datasetOccurrenceCount) {
    int scale = 12;
    String datasetKey = dataset.get("key").textValue();
    dataset.put("occurrenceCount", datasetOccurrenceCount);

    double occurrencePercentage =
        Optional.ofNullable(datasetOccurrenceCount).map(Long::doubleValue).orElse(0D)
            / Math.max(getOccurrenceCount(), 1);
    double nameUsagesPercentage = 0D;

    // Contribution of occurrence records
    dataset.put(
        "occurrencePercentage",
        new Double(
            BigDecimal.valueOf(occurrencePercentage)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue()));
    DatasetMetrics datasetMetrics = gbifWsClient.getDatasetSpeciesMetrics(datasetKey);

    if (Objects.nonNull(datasetMetrics)) {
      nameUsagesPercentage = datasetMetrics.getUsagesCount() / getNameUsagesCount().doubleValue();
      nameUsagesPercentage =
          Double.isInfinite(nameUsagesPercentage) || Double.isNaN(nameUsagesPercentage)
              ? 0D
              : nameUsagesPercentage;
      dataset.put("nameUsagesCount", datasetMetrics.getUsagesCount());
    } else {
      dataset.put("nameUsagesCount", 0);
    }

    // Contribution of NameUsages
    dataset.put(
        "nameUsagesPercentage",
        new Double(
            BigDecimal.valueOf(nameUsagesPercentage)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue()));

    // How much a dataset contributes in terms of records to GBIF data
    dataset.put(
        "dataScore",
        new Double(
            BigDecimal.valueOf((1 - occurrencePercentage) + (1 - nameUsagesPercentage))
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue()));
  }

  private void enumTransforms(ObjectNode dataset) {
    Optional.ofNullable(dataset.get("license"))
        .ifPresent(
            licenseUrl ->
                License.fromLicenseUrl(licenseUrl.asText())
                    .ifPresent(license -> dataset.put("license", license.name())));
  }

  private void addDecades(Dataset dataset, ObjectNode datasetJsonNode) {
    // decade series
    List<Integer> decades = timeSeriesExtractor.extractDecades(dataset.getTemporalCoverages());
    datasetJsonNode
        .putArray("decade")
        .addAll(decades.stream().map(IntNode::new).collect(Collectors.toList()));
  }

  private void addKeyword(Dataset dataset, ObjectNode datasetJsonNode) {

    Collection<JsonNode> keywords =
        Stream.concat(
                dataset.getTags().stream().map(Tag::getValue).map(TextNode::valueOf),
                dataset.getKeywordCollections().stream()
                    .map(KeywordCollection::getKeywords)
                    .flatMap(Set::stream)
                    .map(TextNode::valueOf))
            .collect(Collectors.toList());
    datasetJsonNode.putArray("keyword").addAll(keywords);
  }

  private void addCountryCoverage(Dataset dataset, ObjectNode datasetJsonNode) {
    if (Objects.nonNull(dataset.getCountryCoverage())) {
      datasetJsonNode
          .putArray("countryCoverage")
          .addAll(
              dataset.getCountryCoverage().stream()
                  .map(Country::getIso2LetterCode)
                  .map(TextNode::valueOf)
                  .collect(Collectors.toList()));
    }
  }

  private void addOccurrenceSpeciesCounts(ObjectNode datasetJsonNode) {
    String datasetKey = datasetJsonNode.get("key").textValue();
    Long count = gbifWsClient.getDatasetRecordCount(datasetKey);
    if (count == null) {
      log.warn("Datatset {} with 0 count", datasetKey);
    }
    addRecordCounts(datasetJsonNode, count);
  }

  private void addFacetsData(ObjectNode datasetJsonNode) {
    String datasetKey = datasetJsonNode.get("key").textValue();
    Set<OccurrenceSearchParameter> facets =
        EnumSet.of(
            OccurrenceSearchParameter.COUNTRY, OccurrenceSearchParameter.CONTINENT,
            OccurrenceSearchParameter.TAXON_KEY, OccurrenceSearchParameter.YEAR);
    OccurrenceSearchRequest occurrenceSearchRequest = new OccurrenceSearchRequest();
    occurrenceSearchRequest.setLimit(0);
    occurrenceSearchRequest.setOffset(0);
    occurrenceSearchRequest.setMultiSelectFacets(false);
    occurrenceSearchRequest.setFacetLimit(MAX_FACET_LIMIT);
    occurrenceSearchRequest.setFacetMinCount(1);
    occurrenceSearchRequest.setFacets(facets);
    occurrenceSearchRequest.addParameter(OccurrenceSearchParameter.DATASET_KEY, datasetKey);
    SearchResponse<Occurrence, OccurrenceSearchParameter> response =
        gbifWsClient.occurrenceSearch(occurrenceSearchRequest);
    addRecordCounts(datasetJsonNode, response.getCount());
    ArrayNode countryNode = datasetJsonNode.putArray("country");
    ArrayNode continentNode = datasetJsonNode.putArray("continent");
    ArrayNode taxonKeyNode = datasetJsonNode.putArray("taxonKey");
    ArrayNode yearNode = datasetJsonNode.putArray("year");
    response
        .getFacets()
        .forEach(
            facet -> {
              if (OccurrenceSearchParameter.COUNTRY == facet.getField()) {
                facet.getCounts().forEach(count -> countryNode.add(count.getName()));
              } else if (OccurrenceSearchParameter.CONTINENT == facet.getField()) {
                facet.getCounts().forEach(count -> continentNode.add(count.getName()));
              } else if (OccurrenceSearchParameter.TAXON_KEY == facet.getField()) {
                facet.getCounts().forEach(count -> taxonKeyNode.add(count.getName()));
              } else if (OccurrenceSearchParameter.YEAR == facet.getField()) {
                facet.getCounts().forEach(count -> yearNode.add(count.getName()));
              }
            });
  }

  private void addTaxonKeys(Dataset dataset, ObjectNode datasetObjectNode) {
    if (DatasetType.CHECKLIST == dataset.getType()) {
      ArrayNode taxonKeyNode =
          datasetObjectNode.has("taxonKey")
              ? (ArrayNode) datasetObjectNode.get("taxonKey")
              : datasetObjectNode.putArray("taxonKey");
      for (Integer taxonKey :
          checklistbankPersistenceService.getTaxonKeys(dataset.getKey().toString())) {
        taxonKeyNode.add(new IntNode(taxonKey));
      }
    }
  }

  private void addMachineTags(Dataset dataset, ObjectNode datasetObjectNode) {
    datasetObjectNode
        .putArray("institutionKey")
        .addAll(
            dataset.getMachineTags().stream()
                .filter(
                    mt ->
                        PROCESSING_NAMESPACE.equals(mt.getNamespace())
                            && INSTITUTION_TAG_NAME.equals(mt.getName()))
                .map(v -> new TextNode(v.getValue().split(":")[0]))
                .collect(Collectors.toList()));
    datasetObjectNode
        .putArray("collectionKey")
        .addAll(
            dataset.getMachineTags().stream()
                .filter(
                    mt ->
                        PROCESSING_NAMESPACE.equals(mt.getNamespace())
                            && COLLECTION_TAG_NAME.equals(mt.getName()))
                .map(v -> new TextNode(v.getValue().split(":")[0]))
                .collect(Collectors.toList()));

    // Gridded dataset
    dataset.getMachineTags().stream()
        .filter(
            mt ->
                GRIDDED_DATASET_NAMESPACE.equals(mt.getNamespace())
                    && GRIDDED_DATASET_NAME.equals(mt.getName()))
        .max(Comparator.comparing(MachineTag::getCreated))
        .ifPresent(
            mt -> {
              try {
                datasetObjectNode.set("gridDerivedMetadata", mapper.readTree(mt.getValue()));
              } catch (JsonProcessingException ex) {
                log.error("Error reading machine tag value", ex);
              }
            });
  }

  private void addOccurrenceCoverage(Dataset dataset, ObjectNode datasetObjectNode) {
    try {
      SearchSourceBuilder searchSourceBuilder =
          new SearchSourceBuilder()
              .size(0)
              .query(
                  QueryBuilders.boolQuery()
                      .filter(QueryBuilders.termQuery("datasetKey", dataset.getKey().toString())))
              .aggregation(
                  AggregationBuilders.terms("countryCode")
                      .field("countryCode")
                      .size(200)
                      .shardSize(200)
                      .subAggregation(
                          AggregationBuilders.terms("taxonKey")
                              .size(120_000)
                              .shardSize(120_000)
                              .field("gbifClassification.taxonKey")
                              .subAggregation(
                                  AggregationBuilders.dateHistogram("eventDateSingle")
                                      .field("eventDateSingle")
                                      .dateHistogramInterval(new DateHistogramInterval("3650d")))));

      org.elasticsearch.action.search.SearchResponse searchResponse =
          occurrenceEsClient.search(
              new SearchRequest().source(searchSourceBuilder).indices(occurrenceIndex),
              RequestOptions.DEFAULT);

      List<JsonNode> coverages = new ArrayList<>();

      List<? extends Terms.Bucket> countryBuckets =
          getTermsBuckets(searchResponse.getAggregations(), "countryCode");
      if (!countryBuckets.isEmpty()) {
        countryBuckets.forEach(
            countryBucket -> {
              List<? extends Terms.Bucket> taxonBuckets =
                  getTermsBuckets(countryBucket.getAggregations(), "taxonKey");
              if (!taxonBuckets.isEmpty()) {
                taxonBuckets.forEach(
                    taxonKeyBucket -> {
                      List<? extends Histogram.Bucket> decadesBuckets =
                          getHistogramBuckets(taxonKeyBucket.getAggregations(), "eventDateSingle");
                      if (!decadesBuckets.isEmpty()) {
                        decadesBuckets.forEach(
                            decadeBucket -> {
                              ObjectNode atDecadeCoverage = mapper.createObjectNode();
                              atDecadeCoverage.set("country", toJson(countryBucket));
                              atDecadeCoverage.set("taxonKey", toJson(taxonKeyBucket));
                              atDecadeCoverage.set("decade", toJson(decadeBucket));
                              coverages.add(atDecadeCoverage);
                            });
                      } else {
                        ObjectNode atTaxonKeyCoverage = mapper.createObjectNode();
                        atTaxonKeyCoverage.set("country", toJson(countryBucket));
                        atTaxonKeyCoverage.set("taxonKey", toJson(taxonKeyBucket));
                        coverages.add(atTaxonKeyCoverage);
                      }
                    });
              } else {
                ObjectNode atCountryCoverage = mapper.createObjectNode();
                atCountryCoverage.set("country", toJson(countryBucket));
                coverages.add(atCountryCoverage);
              }
            });
      }
      datasetObjectNode.putArray("occurrenceCoverage").addAll(coverages);

    } catch (IOException ex) {
      log.error("Error retrieving occurrence coverage data", ex);
    }
  }

  private ObjectNode toJson(MultiBucketsAggregation.Bucket bucket) {
    return mapper
        .createObjectNode()
        .put("value", bucket.getKeyAsString())
        .put("count", bucket.getDocCount());
  }

  private List<? extends Terms.Bucket> getTermsBuckets(Aggregations aggs, String aggName) {
    return Optional.ofNullable(aggs)
        .map(aggregations -> aggregations.getAsMap().get(aggName))
        .map(agg -> ((Terms) agg).getBuckets())
        .orElse(Collections.emptyList());
  }

  private List<? extends Histogram.Bucket> getHistogramBuckets(Aggregations aggs, String aggName) {
    return Optional.ofNullable(aggs)
        .map(aggregations -> aggregations.getAsMap().get(aggName))
        .map(agg -> ((Histogram) agg).getBuckets())
        .orElse(Collections.emptyList());
  }
}
