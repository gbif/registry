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
  private String objectClassificationName;
  private List<String> issues;
  private List<VerbatimDto> verbatim;
  private String usageKey;
  private String usageName;
  private String usageRank;
  private List<RankedName> taxonClassification;
  private List<String> taxonKeys;
  private String kingdomKey;
  private String kingdomName;
  private String phylumKey;
  private String phylumName;
  private String classKey;
  private String className;
  private String orderKey;
  private String orderName;
  private String familyKey;
  private String familyName;
  private String genusKey;
  private String genusName;
  private String speciesKey;
  private String speciesName;
}
