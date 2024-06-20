package org.gbif.registry.persistence.mapper.collections.dto;

import lombok.Data;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.TypeStatus;

import java.util.Date;
import java.util.List;

@Data
public class DescriptorRecordDto {

  private long key;
  private Long descriptorKey;
  private String scientificName;
  private Country country;
  private Integer individualCount;
  private List<String> identifiedBy;
  private Date dateIdentified;
  private List<TypeStatus> typeStatus;
  private List<String> recordedBy;
  private String discipline;
  private String objectClassification;
  private List<String> issues;
  private List<VerbatimDto> verbatim;
}
