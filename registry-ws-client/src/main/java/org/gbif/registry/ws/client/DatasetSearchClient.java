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
package org.gbif.registry.ws.client;
import feign.Headers;
import feign.QueryMap;
import feign.RequestLine;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestRequest;
import org.gbif.api.model.registry.search.DatasetSuggestResult;
import org.gbif.api.service.registry.DatasetSearchService;

import java.util.List;
import java.util.Set;

public interface DatasetSearchClient extends DatasetSearchService {

  // ---------------------------------------------------------------------------
  // SEARCH
  // ---------------------------------------------------------------------------

  @RequestLine(
    "GET /dataset/search"
      + "?type={type}"
      + "&subtype={subtype}"
      + "&publishingOrg={publishingOrg}"
      + "&hostingOrg={hostingOrg}"
      + "&keyword={keyword}"
      + "&decade={decade}"
      + "&publishingCountry={publishingCountry}"
      + "&country={country}"
      + "&continent={continent}"
      + "&license={license}"
      + "&projectId={projectId}"
      + "&taxonKey={taxonKey}"
      + "&recordCount={recordCount}"
      + "&year={year}"
      + "&modifiedDate={modifiedDate}"
      + "&datasetTitle={datasetTitle}"
      + "&collectionKey={collectionKey}"
      + "&institutionKey={institutionKey}"
      + "&contactUserId={contactUserId}"
      + "&contactEmail={contactEmail}"
      + "&category={category}"
  )
  @Headers("Accept: application/json")
  SearchResponse<DatasetSearchResult, DatasetSearchParameter> search(
    @QueryMap DatasetSearchRequest datasetSearchRequest,
    Set<String> type,
    Set<String> subtype,
    Set<String> publishingOrg,
    Set<String> hostingOrg,
    Set<String> keyword,
    Set<String> decade,
    Set<String> publishingCountry,
    Set<String> country,
    Set<String> continent,
    Set<String> license,
    Set<String> projectId,
    Set<String> taxonKey,
    Set<String> recordCount,
    Set<String> year,
    Set<String> modifiedDate,
    Set<String> datasetTitle,
    Set<String> collectionKey,
    Set<String> institutionKey,
    Set<String> contactUserId,
    Set<String> contactEmail,
    Set<String> category
  );

  default SearchResponse<DatasetSearchResult, DatasetSearchParameter> search(
    DatasetSearchRequest datasetSearchRequest) {
    return search(
      datasetSearchRequest,
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.TYPE),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.SUBTYPE),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.PUBLISHING_ORG),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.HOSTING_ORG),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.KEYWORD),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.DECADE),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.PUBLISHING_COUNTRY),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.COUNTRY),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.CONTINENT),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.LICENSE),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.PROJECT_ID),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.TAXON_KEY),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.RECORD_COUNT),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.YEAR),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.MODIFIED_DATE),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.DATASET_TITLE),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.COLLECTION_KEY),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.INSTITUTION_KEY),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.CONTACT_USER_ID),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.CONTACT_EMAIL),
      datasetSearchRequest.getParameters().get(DatasetSearchParameter.CATEGORY)
    );
  }

  // ---------------------------------------------------------------------------
  // SUGGEST
  // ---------------------------------------------------------------------------

  @RequestLine(
    "GET /dataset/suggest"
      + "?type={type}"
      + "&subtype={subtype}"
      + "&publishingOrg={publishingOrg}"
      + "&hostingOrg={hostingOrg}"
      + "&keyword={keyword}"
      + "&decade={decade}"
      + "&publishingCountry={publishingCountry}"
      + "&country={country}"
      + "&continent={continent}"
      + "&license={license}"
      + "&projectId={projectId}"
      + "&taxonKey={taxonKey}"
      + "&recordCount={recordCount}"
      + "&year={year}"
      + "&modifiedDate={modifiedDate}"
      + "&datasetTitle={datasetTitle}"
      + "&collectionKey={collectionKey}"
      + "&institutionKey={institutionKey}"
      + "&contactUserId={contactUserId}"
      + "&contactEmail={contactEmail}"
      + "&category={category}"
  )
  @Headers("Accept: application/json")
  List<DatasetSuggestResult> suggest(
    @QueryMap DatasetSuggestRequest datasetSuggestRequest,
    Set<String> type,
    Set<String> subtype,
    Set<String> publishingOrg,
    Set<String> hostingOrg,
    Set<String> keyword,
    Set<String> decade,
    Set<String> publishingCountry,
    Set<String> country,
    Set<String> continent,
    Set<String> license,
    Set<String> projectId,
    Set<String> taxonKey,
    Set<String> recordCount,
    Set<String> year,
    Set<String> modifiedDate,
    Set<String> datasetTitle,
    Set<String> collectionKey,
    Set<String> institutionKey,
    Set<String> contactUserId,
    Set<String> contactEmail,
    Set<String> category
  );

  default List<DatasetSuggestResult> suggest(DatasetSuggestRequest datasetSuggestRequest) {
    return suggest(
      datasetSuggestRequest,
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.TYPE),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.SUBTYPE),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.PUBLISHING_ORG),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.HOSTING_ORG),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.KEYWORD),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.DECADE),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.PUBLISHING_COUNTRY),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.COUNTRY),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.CONTINENT),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.LICENSE),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.PROJECT_ID),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.TAXON_KEY),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.RECORD_COUNT),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.YEAR),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.MODIFIED_DATE),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.DATASET_TITLE),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.COLLECTION_KEY),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.INSTITUTION_KEY),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.CONTACT_USER_ID),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.CONTACT_EMAIL),
      datasetSuggestRequest.getParameters().get(DatasetSearchParameter.CATEGORY)
    );
  }
}

