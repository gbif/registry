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
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.MaintenanceUpdateFrequency;
import org.gbif.registry.search.dataset.indexing.checklistbank.ChecklistbankPersistenceService;
import org.gbif.registry.search.dataset.indexing.ws.GbifWsClient;
import org.gbif.registry.search.dataset.indexing.ws.JacksonObjectMapper;
import org.gbif.vocabulary.client.ConceptClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
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

  // Gridded datasets
  private static final String GRIDDED_DATASET_NAMESPACE = "griddedDataSet.jwaller.gbif.org";
  private static final String GRIDDED_DATASET_NAME = "griddedDataset";

  private final TimeSeriesExtractor timeSeriesExtractor =
      new TimeSeriesExtractor(1000, 2400, 1800, 2050);

  private final List<Consumer<ObjectNode>> consumers = new ArrayList<>();

  private ChecklistbankPersistenceService checklistbankPersistenceService;

  private final GbifWsClient gbifWsClient;

  private final ConceptClient conceptClient;

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
      ConceptClient conceptClient,
      @Autowired(required = false) ChecklistbankPersistenceService checklistbankPersistenceService,
      @Qualifier("apiMapper") ObjectMapper mapper,
      @Qualifier("occurrenceEsClient") RestHighLevelClient occurrenceEsClient,
      @Value("${elasticsearch.occurrence.index}") String occurrenceIndex) {
    this.gbifWsClient = gbifWsClient;
    this.conceptClient = conceptClient;
    this.checklistbankPersistenceService = checklistbankPersistenceService;
    this.mapper = mapper;
    this.occurrenceEsClient = occurrenceEsClient;
    this.occurrenceIndex = occurrenceIndex;
    consumers.add(this::maintenanceFieldsTransforms);
    consumers.add(this::addTitles);
    consumers.add(this::enumTransforms);
    consumers.add(this::addOccurrenceSpeciesCounts);
  }

  public static DatasetJsonConverter create(
      GbifWsClient gbifWsClient,
      ConceptClient conceptClient,
      ChecklistbankPersistenceService checklistbankPersistenceService,
      RestHighLevelClient occurrenceEsClient,
      String occurrenceIndex) {
    return new DatasetJsonConverter(
        gbifWsClient,
        conceptClient,
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
    addNetworks(dataset, datasetAsJson);
    addCategoriesWithParents(dataset, datasetAsJson);
    if (checklistbankPersistenceService != null) {
      addTaxonKeys(dataset, datasetAsJson);
    }
    addMachineTags(dataset, datasetAsJson);
    return datasetAsJson;
  }

  public void addNetworks(Dataset dataset, ObjectNode datasetJsonNode) {
    List<Network> networks = gbifWsClient.getNetworks(dataset.getKey());
    if (networks != null) {
      ArrayNode networkKeys = mapper.createArrayNode();
      ArrayNode networkTitles = mapper.createArrayNode();
      networks.forEach(
          network -> {
            networkKeys.add(new TextNode(network.getKey().toString()));
            networkTitles.add(new TextNode(network.getTitle()));
          });
      datasetJsonNode.putArray("networkKeys").addAll(networkKeys);
      datasetJsonNode.putArray("networkTitles").addAll(networkTitles);
    }
  }

  @SneakyThrows
  public String convertAsJsonString(Dataset dataset) {
    return mapper.writeValueAsString(convert(dataset));
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
            if (Objects.nonNull(hostingOrg.getCountry())) {
              dataset.put("hostingCountry", hostingOrg.getCountry().getIso2LetterCode());
            }
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
        if (Objects.nonNull(publisher.getEndorsingNodeKey())) {
          dataset.put("endorsingNodeKey", publisher.getEndorsingNodeKey().toString());
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
         Double.valueOf(
            BigDecimal.valueOf(occurrencePercentage)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue()));

    if (DatasetType.CHECKLIST.name().equals(dataset.get("type").asText())) {
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
    }

    // Contribution of NameUsages
    dataset.put(
        "nameUsagesPercentage",
        Double.valueOf(
            BigDecimal.valueOf(nameUsagesPercentage)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue()));

    // How much a dataset contributes in terms of records to GBIF data
    dataset.put(
        "dataScore",
        Double.valueOf(
            BigDecimal.valueOf((1 - occurrencePercentage) + (1 - nameUsagesPercentage))
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue()));
  }

  private void maintenanceFieldsTransforms(ObjectNode dataset) {
    Optional.ofNullable(dataset.get("maintenanceDescription"))
        .filter(p -> StringUtils.isBlank(p.asText()))
        .ifPresent(p -> dataset.put("maintenanceDescription", (String) null));
    Optional.ofNullable(dataset.get("maintenanceUpdateFrequency"))
        .filter(JsonNode::isNull)
        .ifPresent(
            p ->
                dataset.put(
                    "maintenanceUpdateFrequency", MaintenanceUpdateFrequency.UNKNOWN.toString()));
  }

  private void enumTransforms(ObjectNode dataset) {
    Optional.ofNullable(dataset.get("license"))
        .flatMap(
            licenseUrl -> {
              Optional<License> license = License.fromLicenseUrl(licenseUrl.asText());
              if (license.isPresent()) {
                return license;
              } else {
                return License.fromString(licenseUrl.asText());
              }
            })
        .ifPresent(license -> dataset.put("license", license.name()));
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
      log.warn("Dataset {} with 0 count", datasetKey);
    }
    addRecordCounts(datasetJsonNode, count);
  }

  private void addFacetsData(ObjectNode datasetJsonNode) {
    String datasetKey = datasetJsonNode.get("key").textValue();
    Set<OccurrenceSearchParameter> facets =
        Set.of(
            OccurrenceSearchParameter.COUNTRY, OccurrenceSearchParameter.CONTINENT,
            OccurrenceSearchParameter.TAXON_KEY, OccurrenceSearchParameter.YEAR);
    OccurrenceSearchRequest occurrenceSearchRequest = new OccurrenceSearchRequest();
    occurrenceSearchRequest.setLimit(0);
    occurrenceSearchRequest.setOffset(0);
    occurrenceSearchRequest.setFacetMultiSelect(false);
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

  private void addCategoriesWithParents(Dataset dataset, ObjectNode datasetJsonNode) {
    if (dataset.getCategory() != null && !dataset.getCategory().isEmpty()) {
      ArrayNode categoryArray = datasetJsonNode.putArray("category");

      for (String categoryName : dataset.getCategory()) {
        ObjectNode categoryObject = mapper.createObjectNode();
        categoryObject.put("concept", categoryName);

        try {
          Optional<VocabularyConcept> vocabularyConcept = VocabularyConceptFactory.createConceptFromName(
              categoryName, conceptClient, "DatasetCategory");

          ArrayNode lineageArray = categoryObject.putArray("lineage");
          vocabularyConcept.ifPresent(value -> value.getLineage().forEach(lineageArray::add));
        } catch (Exception e) {
          log.warn("Could not fetch lineage for category: {}", categoryName, e);
          ArrayNode lineageArray = categoryObject.putArray("lineage");
          lineageArray.add(categoryName);
        }

        categoryArray.add(categoryObject);
      }
    }
  }

  private void addMachineTags(Dataset dataset, ObjectNode datasetObjectNode) {
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
