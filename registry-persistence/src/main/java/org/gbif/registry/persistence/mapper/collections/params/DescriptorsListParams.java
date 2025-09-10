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
package org.gbif.registry.persistence.mapper.collections.params;

import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.collections.CollectionFacetParameter;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class DescriptorsListParams extends CollectionListParams {

  // descriptors fields
  List<String> usageName;
  List<String> usageKey;
  List<String> usageRank;
  List<String> taxonKey;
  @Nullable List<Country> descriptorCountry;
  @Nullable List<RangeParam<Integer>> individualCount;
  @Nullable List<String> identifiedBy;
  @Nullable List<RangeParam<LocalDate>> dateIdentified;
  @Nullable List<String> typeStatus;
  @Nullable List<String> recordedBy;
  @Nullable List<String> discipline;
  @Nullable List<String> objectClassification;
  @Nullable List<String> biome;
  @Nullable List<String> biomeType;
  @Nullable List<String> issues;

  // facets
  @Nullable CollectionFacetParameter facet;

  public boolean descriptorFacet() {
    return facet == CollectionFacetParameter.DESCRIPTOR_COUNTRY
        || facet == CollectionFacetParameter.KINGDOM_KEY
        || facet == CollectionFacetParameter.PHYLUM_KEY
        || facet == CollectionFacetParameter.CLASS_KEY
        || facet == CollectionFacetParameter.ORDER_KEY
        || facet == CollectionFacetParameter.FAMILY_KEY
        || facet == CollectionFacetParameter.GENUS_KEY
        || facet == CollectionFacetParameter.SPECIES_KEY
        || facet == CollectionFacetParameter.TYPE_STATUS
        || facet == CollectionFacetParameter.RECORDED_BY
        || facet == CollectionFacetParameter.OBJECT_CLASSIFICATION
        || facet == CollectionFacetParameter.BIOME
        || facet == CollectionFacetParameter.TAXON_KEY;
  }

  public boolean descriptorSearch() {
    return query != null
        || usageName != null
        || usageKey != null
        || usageRank != null
        || taxonKey != null
        || descriptorCountry != null
        || individualCount != null
        || identifiedBy != null
        || dateIdentified != null
        || typeStatus != null
        || recordedBy != null
        || discipline != null
        || objectClassification != null
        || biome != null
        || biomeType != null
        || issues != null;
  }

  public boolean descriptorSearchWithoutQuery() {
    return usageName != null
        || usageKey != null
        || usageRank != null
        || taxonKey != null
        || descriptorCountry != null
        || individualCount != null
        || identifiedBy != null
        || dateIdentified != null
        || typeStatus != null
        || recordedBy != null
        || discipline != null
        || objectClassification != null
        || biome != null
        || biomeType != null
        || issues != null;
  }

  public boolean isArrayFieldFacet() {
    return facet == CollectionFacetParameter.PRESERVATION_TYPE
        || facet == CollectionFacetParameter.CONTENT_TYPE
        || facet == CollectionFacetParameter.TYPE_STATUS
        || facet == CollectionFacetParameter.RECORDED_BY
        || facet == CollectionFacetParameter.TAXON_KEY;
  }
}
