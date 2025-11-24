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
package org.gbif.registry.search.dataset;

import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.vocabulary.Continent;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetSubtype;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.api.vocabulary.Extension;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.search.dataset.common.EsFieldMapper;

import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class DatasetEsFieldMapper implements EsFieldMapper<DatasetSearchParameter> {

  private static final ImmutableBiMap<DatasetSearchParameter, String> SEARCH_TO_ES_MAPPING =
      ImmutableBiMap.<DatasetSearchParameter, String>builder()
          .put(DatasetSearchParameter.TAXON_KEY, "taxonKey")
          .put(DatasetSearchParameter.CONTINENT, "continent")
          .put(DatasetSearchParameter.COUNTRY, "country")
          .put(DatasetSearchParameter.PUBLISHING_COUNTRY, "publishingCountry")
          .put(DatasetSearchParameter.PUBLISHING_ORG, "publishingOrganizationKey")
          .put(DatasetSearchParameter.ENDORSING_NODE_KEY, "endorsingNodeKey")
          .put(DatasetSearchParameter.YEAR, "year")
          .put(DatasetSearchParameter.DECADE, "decade")
          .put(DatasetSearchParameter.INSTALLATION_KEY, "installationKey")
          .put(DatasetSearchParameter.HOSTING_ORG, "hostingOrganizationKey")
          .put(DatasetSearchParameter.HOSTING_COUNTRY, "hostingCountry")
          .put(DatasetSearchParameter.KEYWORD, "keyword")
          .put(DatasetSearchParameter.LICENSE, "license")
          .put(DatasetSearchParameter.MODIFIED_DATE, "modified")
          .put(DatasetSearchParameter.PROJECT_ID, "project.identifier")
          .put(DatasetSearchParameter.RECORD_COUNT, "occurrenceCount")
          .put(DatasetSearchParameter.SUBTYPE, "subtype")
          .put(DatasetSearchParameter.TYPE, "type")
          .put(DatasetSearchParameter.DATASET_TITLE, "title")
          .put(DatasetSearchParameter.DOI, "doi")
          .put(DatasetSearchParameter.NETWORK_KEY, "networkKeys")
          .put(DatasetSearchParameter.ENDPOINT_TYPE, "endpoints.type")
          .put(DatasetSearchParameter.CATEGORY, "category.lineage")
          .put(DatasetSearchParameter.DWCA_EXTENSION, "dwca.extensions")
          .put(DatasetSearchParameter.DWCA_CORE_TYPE, "dwca.coreType")
          .put(DatasetSearchParameter.CONTACT_USER_ID, "contacts.userId.keyword")
          .put(DatasetSearchParameter.CONTACT_EMAIL, "contacts.email.keyword")
          .build();

  public static final Map<String, Integer> CARDINALITIES =
      ImmutableMap.<String, Integer>builder()
          .put("license", License.values().length)
          .put("country", Country.values().length)
          .put("publishingCountry", Country.values().length)
          .put("continent", Continent.values().length)
          .put("type", DatasetType.values().length)
          .put("subtype", DatasetSubtype.values().length)
          .put("endpoints.type", EndpointType.values().length)
          .put("dwcaExtensions", Extension.values().length)
          .build();

  private static final String[] EXCLUDE_FIELDS = new String[] {"all"};

  private static final String[] DATASET_TITLE_SUGGEST_FIELDS =
      new String[] {"title", "type", "subtype", "description"};

  private static final String[] DATASET_HIGHLIGHT_FIELDS = new String[] {"title", "description"};

  private static final SortOptions[] SORT =
      new SortOptions[] {
        SortOptions.of(sort -> sort.field(field -> field.field("dataScore").order(SortOrder.Asc))),
        SortOptions.of(sort -> sort.field(field -> field.field("created").order(SortOrder.Desc)))
      };

  public static final List<String> DATE_FIELDS = ImmutableList.of("modified", "created", "pubDate");

  @Override
  public DatasetSearchParameter get(String esField) {
    return SEARCH_TO_ES_MAPPING.inverse().get(esField);
  }

  @Override
  public boolean isDateField(String esFieldName) {
    return DATE_FIELDS.contains(esFieldName);
  }

  @Override
  public Integer getCardinality(String esFieldName) {
    return CARDINALITIES.get(esFieldName);
  }

  @Override
  public String get(DatasetSearchParameter datasetSearchParameter) {
    return SEARCH_TO_ES_MAPPING.get(datasetSearchParameter);
  }

  @Override
  public String[] excludeFields() {
    return EXCLUDE_FIELDS;
  }

  @Override
  public SortOptions[] sorts() {
    return SORT;
  }

  @Override
  public String[] includeSuggestFields(DatasetSearchParameter searchParameter) {
    if (DatasetSearchParameter.DATASET_TITLE == searchParameter) {
      return DATASET_TITLE_SUGGEST_FIELDS;
    }
    return new String[] {SEARCH_TO_ES_MAPPING.get(searchParameter)};
  }

  @Override
  public String[] highlightingFields() {
    return DATASET_HIGHLIGHT_FIELDS;
  }

  @Override
  public String[] getMappedFields() {
    return new String[] {
      "title",
      "type",
      "subtype",
      "description",
      "publishingOrganizationKey",
      "publishingOrganizationTitle",
      "publishingCountry",
      "endorsingNodeKey",
      "hostingOrganizationKey",
      "hostingOrganizationTitle",
      "hostingCountry",
      "license",
      "project.identifier",
      "nameUsagesCount",
      "occurrenceCount",
      "keyword",
      "decade",
      "countryCoverage",
      "doi",
      "networkKeys",
      "networkTitle",
      "category",
      "networkTitle",
      "contacts.userId.keyword",
      "contacts.email.keyword"
    };
  }

  @Override
  public Query fullTextQuery(String q) {
    return Query.of(
        qu ->
            qu.functionScore(
                fs ->
                    fs.query(
                            inner ->
                                inner.multiMatch(
                                    mm ->
                                        mm.query(q)
                                            .fields(
                                                List.of(
                                                    "doi^25",
                                                    "title^20",
                                                    "keyword^10",
                                                    "description^8",
                                                    "publishingOrganizationTitle^5",
                                                    "hostingOrganizationTitle^5",
                                                    "networkTitle^4",
                                                    "metadata^3",
                                                    "projectId^2",
                                                    "category.lineage^5",
                                                    "all^1"))
                                            .tieBreaker(0.2)
                                            .minimumShouldMatch("25%")
                                            .fuzziness("AUTO")
                                            .slop(100)))
                        .functions(
                            fns ->
                                fns.fieldValueFactor(
                                    fvf ->
                                        fvf.field("dataScore")
                                            .modifier(FieldValueFactorModifier.Ln2p)
                                            .missing(0.0)))
                        .boostMode(FunctionBoostMode.Multiply)));
  }
}
