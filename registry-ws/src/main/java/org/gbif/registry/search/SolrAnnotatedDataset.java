package org.gbif.registry.search;

import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetSubtype;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.common.search.model.FacetField;
import org.gbif.common.search.model.FacetField.Method;
import org.gbif.common.search.model.FullTextSearchField;
import org.gbif.common.search.model.Key;
import org.gbif.common.search.model.SearchMapping;
import org.gbif.common.search.model.SuggestMapping;
import org.gbif.common.search.model.WildcardPadding;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;
import org.apache.solr.client.solrj.beans.Field;

/**
 * Annotated version of the dataset search result providing the required mapping to the SOLR schema and translation
 * between String to UUID as SOLR does not support UUID well.
 */
@SearchMapping(
  facets = {
    @FacetField(name = "TYPE", field = "dataset_type", sort = FacetField.SortOrder.INDEX, method = Method.ENUM),
    @FacetField(name = "SUBTYPE", field = "dataset_subtype", sort = FacetField.SortOrder.INDEX, method = Method.ENUM),
    @FacetField(name = "KEYWORD", field = "keyword", method = Method.ENUM),
    @FacetField(name = "PUBLISHING_ORG", field = "publishing_organization_key", method = Method.ENUM),
    @FacetField(name = "HOSTING_ORG", field = "hosting_organization_key", method = Method.ENUM),
    @FacetField(name = "DECADE", field = "decade", sort = FacetField.SortOrder.INDEX, method = Method.ENUM),
    @FacetField(name = "COUNTRY", field = "country", sort = FacetField.SortOrder.INDEX, method = Method.ENUM),
    @FacetField(name = "PUBLISHING_COUNTRY", field = "publishing_country", sort = FacetField.SortOrder.INDEX,
      method = Method.ENUM)
  },
  fulltextFields = {
    @FullTextSearchField(field = "dataset_title", highlightField = "dataset_title", exactMatchScore = 10.0d,
      partialMatchScore = 1.0d),
    @FullTextSearchField(field = "keyword", partialMatching = WildcardPadding.NONE, exactMatchScore = 4.0d),
    @FullTextSearchField(field = "publishing_organization_title", highlightField = "publishing_organization_title",
      partialMatching = WildcardPadding.NONE, exactMatchScore = 2.0d),
    @FullTextSearchField(field = "hosting_organization_title", partialMatching = WildcardPadding.NONE,
      exactMatchScore = 2.0d),
    @FullTextSearchField(field = "description", partialMatching = WildcardPadding.NONE),
    @FullTextSearchField(field = "metadata", partialMatching = WildcardPadding.NONE, exactMatchScore = 0.5d)
  })
@SuggestMapping(field = "dataset_title_ngram", phraseQueryField = "dataset_title_nedge")
public class SolrAnnotatedDataset extends DatasetSearchResult {

  @Field("country")
  public void setCountryCoverage(List<Integer> countryOrdinals) {
    Set<Country> countries = Sets.newHashSet();
    for (Integer ordinal : countryOrdinals) {
      if (ordinal != null) {
        countries.add(Country.values()[ordinal]);
      }
    }
    super.setCountryCoverage(countries);
  }

  @Field("publishing_country")
  public void setPublishingCountry(Integer countryOrdinal) {
    super.setPublishingCountry(countryOrdinal == null ? Country.UNKNOWN : Country.values()[countryOrdinal]);
  }

  @Field("decade")
  @Override
  public void setDecades(List<Integer> decades) {
    super.setDecades(decades);
  }

  @Field("description")
  @Override
  public void setDescription(String description) {
    super.setDescription(description);
  }

  @Field("metadata")
  @Override
  public void setFullText(String metadata) {
    super.setFullText(metadata);
  }

  @Field("hosting_organization_key")
  public void setHostingOrganizationKey(String hostingOrganizationKey) {
    setHostingOrganizationKey(UUID.fromString(hostingOrganizationKey));
  }

  @Override
  @Field("hosting_organization_title")
  public void setHostingOrganizationTitle(String hostingOrganizationTitle) {
    super.setHostingOrganizationTitle(hostingOrganizationTitle);
  }

  @Field("key")
  @Key
  public void setKey(String key) {
    setKey(UUID.fromString(key));
  }

  @Override
  @Field("keyword")
  public void setKeywords(List<String> keywords) {
    super.setKeywords(keywords);
  }

  @Field("publishing_organization_key")
  public void setPublishingOrganizationKey(String publishingOrganizationKey) {
    setPublishingOrganizationKey(UUID.fromString(publishingOrganizationKey));
  }

  @Override
  @Field("publishing_organization_title")
  public void setPublishingOrganizationTitle(String publishingOrganizationTitle) {
    super.setPublishingOrganizationTitle(publishingOrganizationTitle);
  }

  @Field("dataset_subtype")
  public void setSubtype(Integer datasetSubtypeOrdinal) {
    setSubtype(datasetSubtypeOrdinal == null ? null : DatasetSubtype.values()[datasetSubtypeOrdinal]);
  }

  @Field("dataset_title")
  @Override
  public void setTitle(String title) {
    super.setTitle(title);
  }

  @Field("dataset_type")
  public void setType(Integer datasetTypeOrdinal) {
    setType(datasetTypeOrdinal == null ? null : DatasetType.values()[datasetTypeOrdinal]);
  }
}
