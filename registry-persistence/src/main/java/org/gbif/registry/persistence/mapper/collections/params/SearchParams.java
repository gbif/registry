package org.gbif.registry.persistence.mapper.collections.params;

import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TypeStatus;

@Data
@Builder
public class SearchParams {

  @Nullable String q;
  Boolean highlight;
  String type;
  Boolean displayOnNHCPortal;
  @Nullable List<Country> countries;
  @Nullable List<Country> regionCountries;
  @Nullable String city;

  Integer limit;
  Integer offset;

  // collection fields

  // descriptors fields
  List<String> usageName;
  List<Integer> usageKey;
  List<Rank> usageRank;
  List<Integer> taxonKey;
  @Nullable List<Country> descriptorCountry;
  @Nullable RangeParam individualCount;
  @Nullable List<String> identifiedBy;
  @Nullable
  Date dateIdentified;
  @Nullable Date dateIdentifiedFrom;
  @Nullable Date dateIdentifiedBefore;
  @Nullable List<TypeStatus> typeStatus;
  @Nullable List<String> recordedBy;
  @Nullable List<String> discipline;
  @Nullable List<String> objectClassification;
  @Nullable List<String> issues;
}
