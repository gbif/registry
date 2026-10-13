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
package org.gbif.registry.search.dataset.common;

import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.registry.search.dataset.DatasetEsFieldMapper;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** createdDate must facet by day, not by raw timestamp. */
class CreatedDateFacetTest {

  private final EsSearchRequestBuilder<DatasetSearchParameter> requestBuilder =
      new EsSearchRequestBuilder<>(new DatasetEsFieldMapper());

  private final EsResponseParser<Object, Object, DatasetSearchParameter> responseParser =
      new EsResponseParser<>(
          new SearchResultConverter<>() {
            @Override
            public Object toSearchResult(
                co.elastic.clients.elasticsearch.core.search.Hit<ObjectNode> searchHit) {
              throw new UnsupportedOperationException();
            }

            @Override
            public Object toSearchSuggestResult(
                co.elastic.clients.elasticsearch.core.search.Hit<ObjectNode> searchHit) {
              throw new UnsupportedOperationException();
            }
          },
          new DatasetEsFieldMapper());

  @Test
  void createdDateFacetIsADayHistogram() {
    DatasetSearchRequest request = new DatasetSearchRequest();
    request.addFacets(DatasetSearchParameter.CREATED_DATE, DatasetSearchParameter.TYPE);

    SearchRequest esRequest = requestBuilder.buildSearchRequest(request, true, "dataset");

    Aggregation created = esRequest.aggregations().get("created");
    assertTrue(created.isDateHistogram());
    assertEquals(CalendarInterval.Day, created.dateHistogram().calendarInterval());
    assertEquals("yyyy-MM-dd", created.dateHistogram().format());
    assertEquals("_count", created.dateHistogram().order().get(0).name());
    assertEquals(SortOrder.Desc, created.dateHistogram().order().get(0).value());

    assertTrue(esRequest.aggregations().get("type").isTerms());
  }

  @Test
  void createdDateFacetNamesAreIsoDays() {
    DatasetSearchRequest request = new DatasetSearchRequest();
    request.addFacets(DatasetSearchParameter.CREATED_DATE);
    request.setFacetLimit(1);

    co.elastic.clients.elasticsearch.core.SearchResponse<ObjectNode> esResponse =
        co.elastic.clients.elasticsearch.core.SearchResponse.of(
            r ->
                r.took(1)
                    .timedOut(false)
                    .shards(s -> s.total(1).successful(1).skipped(0).failed(0))
                    .hits(
                        h ->
                            h.hits(List.of())
                                .total(t -> t.value(5).relation(TotalHitsRelation.Eq)))
                    .aggregations(
                        "created",
                        Aggregate.of(
                            a ->
                                a.dateHistogram(
                                    dh ->
                                        dh.buckets(
                                            b ->
                                                b.array(
                                                    List.of(
                                                        bucket(1757462400000L, "2026-09-10", 4),
                                                        bucket(1757548800000L, "2026-09-11", 1))))))));

    SearchResponse<Object, DatasetSearchParameter> response =
        responseParser.buildSearchResponse(esResponse, request);

    Facet<DatasetSearchParameter> facet = response.getFacets().get(0);
    assertEquals(DatasetSearchParameter.CREATED_DATE, facet.getField());
    assertEquals(1, facet.getCounts().size());
    assertEquals("2026-09-10", facet.getCounts().get(0).getName());
    assertEquals(4L, facet.getCounts().get(0).getCount());
  }

  private static DateHistogramBucket bucket(long key, String day, long count) {
    return DateHistogramBucket.of(b -> b.key(key).keyAsString(day).docCount(count));
  }
}
