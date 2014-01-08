package org.gbif.registry.search;

import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetSubtype;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.common.search.model.FacetField;
import org.gbif.common.search.model.FullTextSearchField;
import org.gbif.common.search.model.Key;
import org.gbif.common.search.model.SearchMapping;
import org.gbif.common.search.model.SuggestMapping;
import org.gbif.common.search.model.WildcardPadding;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Optional;

import com.google.common.base.Enums;

import com.google.common.collect.Sets;
import org.apache.solr.client.solrj.beans.Field;

/**
 * Annotated version of the dataset search result providing the required mapping to the SOLR schema and translation
 * between String to UUID as SOLR does not support UUID well.
 */
@SearchMapping(
  facets = {
    @FacetField(name = "TYPE", field = "dataset_type", sort = FacetField.SortOrder.INDEX),
    @FacetField(name = "SUBTYPE", field = "dataset_subtype", sort = FacetField.SortOrder.INDEX),
    @FacetField(name = "KEYWORD", field = "keyword"),
    @FacetField(name = "OWNING_ORG", field = "owning_organization_key"),
    @FacetField(name = "HOSTING_ORG", field = "hosting_organization_key"),
    @FacetField(name = "DECADE", field = "decade", sort = FacetField.SortOrder.INDEX),
    @FacetField(name = "COUNTRY", field = "country", sort = FacetField.SortOrder.INDEX),
    @FacetField(name = "PUBLISHING_COUNTRY", field = "publishing_country", sort = FacetField.SortOrder.INDEX)
  },
  fulltextFields = {
    @FullTextSearchField(field = "dataset_title", highlightField = "dataset_title", exactMatchScore = 10.0d,
      partialMatchScore = 1.0d),
    @FullTextSearchField(field = "keyword", partialMatching = WildcardPadding.NONE, exactMatchScore = 4.0d),
    @FullTextSearchField(field = "country", partialMatching = WildcardPadding.NONE, exactMatchScore = 3.0d),
    @FullTextSearchField(field = "publishing_country", partialMatching = WildcardPadding.NONE, exactMatchScore = 1.0d),
    @FullTextSearchField(field = "owning_organization_title", highlightField = "owning_organization_title",
      partialMatching = WildcardPadding.NONE, exactMatchScore = 2.0d),
    @FullTextSearchField(field = "hosting_organization_title", partialMatching = WildcardPadding.NONE,
      exactMatchScore = 2.0d),
    @FullTextSearchField(field = "description", partialMatching = WildcardPadding.NONE),
    @FullTextSearchField(field = "full_text", partialMatching = WildcardPadding.NONE, exactMatchScore = 0.5d)
  })
@SuggestMapping(field = "dataset_title_ngram", phraseQueryField = "dataset_title_nedge")
public class SolrAnnotatedDataset extends DatasetSearchResult {

  @Field("country")
  public void setCountryCoverage(List<String> isoCountryCodes) {
    Set<Country> countries = Sets.newHashSet();
    for (String iso : isoCountryCodes) {
      Country c = Country.fromIsoCode(iso);
      if (c != null) {
        countries.add(c);
      }
    }
    super.setCountryCoverage(countries);
  }

  @Field("publishing_country")
  public void setPublishingCountry(String iso) {
    Country c = Country.fromIsoCode(iso);
    if (c == null) {
      //tries parse the country by name
      Optional<Country> optCountry = Enums.getIfPresent(Country.class, iso);
      c = optCountry.isPresent()? optCountry.get() : Country.UNKNOWN;
    }
    super.setPublishingCountry(c);
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

  @Field("full_text")
  @Override
  public void setFullText(String fullText) {
    super.setFullText(fullText);
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

  @Field("owning_organization_key")
  public void setOwningOrganizationKey(String owningOrganizationKey) {
    setOwningOrganizationKey(UUID.fromString(owningOrganizationKey));
  }

  @Override
  @Field("owning_organization_title")
  public void setOwningOrganizationTitle(String owningOrganizationTitle) {
    super.setOwningOrganizationTitle(owningOrganizationTitle);
  }

  @Field("dataset_subtype")
  public void setSubtype(String datasetSubtype) {
    setSubtype(DatasetSubtype.valueOf(datasetSubtype));
  }

  @Field("dataset_title")
  @Override
  public void setTitle(String title) {
    super.setTitle(title);
  }

  @Field("dataset_type")
  public void setType(String datasetType) {
    setType(DatasetType.valueOf(datasetType));
  }
}
