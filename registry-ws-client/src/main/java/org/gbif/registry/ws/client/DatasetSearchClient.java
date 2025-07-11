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

import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestRequest;
import org.gbif.api.model.registry.search.DatasetSuggestResult;
import org.gbif.api.service.registry.DatasetSearchService;

import java.util.List;
import java.util.Set;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

public interface DatasetSearchClient extends DatasetSearchService {

  @RequestMapping(
      method = RequestMethod.GET,
      value = "dataset/search",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  SearchResponse<DatasetSearchResult, DatasetSearchParameter> search(
      @SpringQueryMap DatasetSearchRequest datasetSearchRequest,
      @RequestParam(value = "type", required = false) Set<String> type,
      @RequestParam(value = "subtype", required = false) Set<String> subtype,
      @RequestParam(value = "publishingOrg", required = false) Set<String> publishingOrg,
      @RequestParam(value = "hostingOrg", required = false) Set<String> hostingOrg,
      @RequestParam(value = "keyword", required = false) Set<String> keyword,
      @RequestParam(value = "decade", required = false) Set<String> decade,
      @RequestParam(value = "publishingCountry", required = false) Set<String> publishingCountry,
      @RequestParam(value = "country", required = false) Set<String> country,
      @RequestParam(value = "continent", required = false) Set<String> continent,
      @RequestParam(value = "license", required = false) Set<String> license,
      @RequestParam(value = "projectId", required = false) Set<String> projectId,
      @RequestParam(value = "taxonKey", required = false) Set<String> taxonKey,
      @RequestParam(value = "recordCount", required = false) Set<String> recordCount,
      @RequestParam(value = "year", required = false) Set<String> year,
      @RequestParam(value = "modifiedDate", required = false) Set<String> modifiedDate,
      @RequestParam(value = "datasetTitle", required = false) Set<String> datasetTitle,
      @RequestParam(value = "collectionKey", required = false) Set<String> collectionKey,
      @RequestParam(value = "institutionKey", required = false) Set<String> institutionKey,
      @RequestParam(value = "contactUserId", required = false) Set<String> contactUserId,
      @RequestParam(value = "contactEmail", required = false) Set<String> contactEmail,
      @RequestParam(value = "category", required = false) Set<String> category);

  @Override
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
        datasetSearchRequest.getParameters().get(DatasetSearchParameter.CATEGORY),
        datasetSearchRequest.getParameters().get(DatasetSearchParameter.CONTACT_USER_ID),
        datasetSearchRequest.getParameters().get(DatasetSearchParameter.CONTACT_EMAIL));
  }

  @RequestMapping(
      method = RequestMethod.GET,
      value = "dataset/suggest",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<DatasetSuggestResult> suggest(
      @SpringQueryMap DatasetSuggestRequest datasetSuggestRequest,
      @RequestParam(value = "type", required = false) Set<String> type,
      @RequestParam(value = "subtype", required = false) Set<String> subtype,
      @RequestParam(value = "publishingOrg", required = false) Set<String> publishingOrg,
      @RequestParam(value = "hostingOrg", required = false) Set<String> hostingOrg,
      @RequestParam(value = "keyword", required = false) Set<String> keyword,
      @RequestParam(value = "decade", required = false) Set<String> decade,
      @RequestParam(value = "publishingCountry", required = false) Set<String> publishingCountry,
      @RequestParam(value = "country", required = false) Set<String> country,
      @RequestParam(value = "continent", required = false) Set<String> continent,
      @RequestParam(value = "license", required = false) Set<String> license,
      @RequestParam(value = "projectId", required = false) Set<String> projectId,
      @RequestParam(value = "taxonKey", required = false) Set<String> taxonKey,
      @RequestParam(value = "recordCount", required = false) Set<String> recordCount,
      @RequestParam(value = "year", required = false) Set<String> year,
      @RequestParam(value = "modifiedDate", required = false) Set<String> modifiedDate,
      @RequestParam(value = "datasetTitle", required = false) Set<String> datasetTitle,
      @RequestParam(value = "collectionKey", required = false) Set<String> collectionKey,
      @RequestParam(value = "institutionKey", required = false) Set<String> institutionKey,
      @RequestParam(value = "contactUserId", required = false) Set<String> contactUserId,
      @RequestParam(value = "contactEmail", required = false) Set<String> contactEmail,
      @RequestParam(value = "category", required = false) Set<String> category);

  @Override
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
        datasetSuggestRequest.getParameters().get(DatasetSearchParameter.CATEGORY));
  }
}
