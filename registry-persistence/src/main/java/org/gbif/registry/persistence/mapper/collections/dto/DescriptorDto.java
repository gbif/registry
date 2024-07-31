package org.gbif.registry.persistence.mapper.collections.dto;

import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.Data;
import org.gbif.api.v2.RankedName;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Rank;

@Data
public class DescriptorDto {

  private long key;
  private Long descriptorGroupKey;
  private Country country;
  private Integer individualCount;
  private List<String> identifiedBy;
  private Date dateIdentified;
  private List<String> typeStatus;
  private List<String> recordedBy;
  private String discipline;
  private String objectClassification;
  private List<String> issues;
  private List<VerbatimDto> verbatim;
  private Integer usageKey;
  private String usageName;
  private Rank usageRank;
  private List<RankedName> taxonClassification;
  private Set<Integer> taxonKeys;
}
