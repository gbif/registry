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
package org.gbif.registry.search.dataset.indexing.ws;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.occurrence.Occurrence;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/** Retrofit client to all the GBIF service call needed for dataset indexing. */
public interface GbifApiService {

  @GET("occurrence/count")
  Call<Long> getDatasetRecordCount(@Query("datasetKey") String datasetKey);

  @GET("occurrence/count")
  Call<Long> getOccurrenceRecordCount();

  @GET("dataset/{datasetKey}/metrics")
  Call<DatasetMetrics> getDatasetSpeciesMetrics(@Path("datasetKey") String datasetKey);

  @GET("species/search")
  Call<SearchResponse<NameUsage, NameUsageSearchParameter>> speciesSearch(
      @QueryMap Map<String, Object> options);

  @GET("occurrence/search")
  Call<SearchResponse<Occurrence, OccurrenceSearchParameter>> occurrenceSearch(
      @QueryMap Map<String, Object> options);
}
