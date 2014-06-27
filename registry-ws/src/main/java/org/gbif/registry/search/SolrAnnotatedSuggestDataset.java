package org.gbif.registry.search;

import org.gbif.api.model.registry.search.DatasetSuggestResult;
import org.gbif.api.vocabulary.DatasetSubtype;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.common.search.model.Key;
import org.gbif.common.search.model.SuggestMapping;

import java.util.UUID;

import org.apache.solr.client.solrj.beans.Field;

/**
 * This class contains the annotations required by {@link org.gbif.api.service.common.SuggestService} and the Solr
 * result/object mapping.
 * This class holds the Solr response mapping and the annotation required by the suggest service.
 */
@SuggestMapping(field = "dataset_title_ngram", phraseQueryField = "dataset_title_nedge")
public class SolrAnnotatedSuggestDataset extends DatasetSuggestResult {

  @Field("description")
  @Override
  public void setDescription(String description) {
    super.setDescription(description);
  }

  @Field("key")
  @Key
  public void setKey(String key) {
    setKey(UUID.fromString(key));
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
