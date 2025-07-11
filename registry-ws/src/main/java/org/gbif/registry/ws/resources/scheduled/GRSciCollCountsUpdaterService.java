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
package org.gbif.registry.ws.resources.scheduled;

import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.occurrence.Occurrence;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.model.occurrence.search.OccurrenceSearchRequest;
import org.gbif.occurrence.ws.client.OccurrenceWsSearchClient;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.params.Count;
import org.gbif.registry.service.collections.utils.Vocabularies;
import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Endpoint(id = "grscicollCounts")
@Slf4j
public class GRSciCollCountsUpdaterService {

  private final InstitutionMapper institutionMapper;
  private final CollectionMapper collectionMapper;
  private final ConceptClient conceptClient;
  private final OccurrenceWsSearchClient occurrenceWsSearchClient;

  @Autowired
  public GRSciCollCountsUpdaterService(
      InstitutionMapper institutionMapper,
      CollectionMapper collectionMapper,
      ConceptClient conceptClient,
      @Value("${api.root.url}") String apiRootUrl) {
    this.institutionMapper = institutionMapper;
    this.collectionMapper = collectionMapper;
    this.conceptClient = conceptClient;
    this.occurrenceWsSearchClient =
        new ClientBuilder()
            .withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport())
            .withUrl(apiRootUrl)
            .build(OccurrenceWsSearchClient.class);
  }

  @WriteOperation
  public void updateGRSciCollCountsEndpoint() {
    updateCounts();
  }

  @Scheduled(cron = "${grscicoll.counts.cron:0 0 8 * * 1-5}")
  @Transactional
  public void scheduleUpdateCounts() {
    updateCounts();
  }

  private void updateCounts() {
    log.info("Updating GRSciColl counts");

    List<UUID> institutionKeys = institutionMapper.getAllKeys();
    List<UUID> collectionKeys = collectionMapper.getAllKeys();
    long facetCount = Math.max(institutionKeys.size(), collectionKeys.size());

    OccurrenceSearchRequest request = new OccurrenceSearchRequest();
    request.setLimit(0);
    request.addFacets(
        OccurrenceSearchParameter.INSTITUTION_KEY, OccurrenceSearchParameter.COLLECTION_KEY);
    request.setFacetLimit((int) facetCount);
    SearchResponse<Occurrence, OccurrenceSearchParameter> occurrenceCountsResponse =
        occurrenceWsSearchClient.search(request);

    // all entities are initialised to zero because the occurrence response doesn't return values
    // with count zero and if they used to have records we need to update them to zero
    Map<UUID, Count> institutionsCounts =
        institutionKeys.stream()
            .collect(
                Collectors.toMap(
                    k -> k,
                    k -> {
                      Count c = new Count();
                      c.setKey(k);
                      c.setOccurrenceCount(0);
                      c.setTypeSpecimenCount(0);
                      return c;
                    }));
    Map<UUID, Count> collectionsCounts =
        collectionKeys.stream()
            .collect(
                Collectors.toMap(
                    k -> k,
                    k -> {
                      Count c = new Count();
                      c.setKey(k);
                      c.setOccurrenceCount(0);
                      c.setTypeSpecimenCount(0);
                      return c;
                    }));

    Function<OccurrenceSearchParameter, Map<UUID, Count>> mapCountsSupplier =
        p -> {
          if (p.equals(OccurrenceSearchParameter.INSTITUTION_KEY)) {
            return institutionsCounts;
          } else if (p.equals(OccurrenceSearchParameter.COLLECTION_KEY)) {
            return collectionsCounts;
          } else {
            throw new IllegalStateException("Invalid facet: " + p);
          }
        };

    for (Facet<OccurrenceSearchParameter> f : occurrenceCountsResponse.getFacets()) {
      Map<UUID, Count> countsMap = mapCountsSupplier.apply(f.getField());
      for (Facet.Count c : f.getCounts()) {
        UUID key = UUID.fromString(c.getName());
        Count count = countsMap.computeIfAbsent(key, k -> new Count());
        count.setKey(key);
        count.setOccurrenceCount(c.getCount());
      }
    }

    // add type specimens to the request and make the call
    List<String> typeStatusValues =
        Vocabularies.getVocabularyConcepts(Vocabularies.TYPE_STATUS, conceptClient);

    typeStatusValues.stream()
        .filter(t -> !t.equalsIgnoreCase("NotAType"))
        .forEach(request::addTypeStatusFilter);
    SearchResponse<Occurrence, OccurrenceSearchParameter> typeSpecimenCountsResponse =
        occurrenceWsSearchClient.search(request);

    for (Facet<OccurrenceSearchParameter> f : typeSpecimenCountsResponse.getFacets()) {
      Map<UUID, Count> countsMap = mapCountsSupplier.apply(f.getField());
      for (Facet.Count c : f.getCounts()) {
        UUID key = UUID.fromString(c.getName());
        Count count = countsMap.computeIfAbsent(key, k -> new Count());
        count.setKey(key);
        count.setTypeSpecimenCount(c.getCount());
      }
    }

    institutionMapper.updateCounts(institutionsCounts.values());
    collectionMapper.updateCounts(collectionsCounts.values());
  }
}
