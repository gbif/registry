package org.gbif.registry.persistence.mapper.collections.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.gbif.api.vocabulary.collections.MasterSourceType;

@SuppressWarnings("MissingOverride")
@EqualsAndHashCode(callSuper = true)
@Data
public class CollectionSearchDto extends SearchDto {

  private List<String> contentTypes = new ArrayList<>();
  private boolean personalCollection;
  private List<String> preservationTypes = new ArrayList<>();
  private String accessionStatus;
  private Integer numberSpecimens;
  private String taxonomicCoverage;
  private String geographicCoverage;
  private MasterSourceType masterSource;
  private String department;
  private String division;
  private Integer occurrenceCount;
  private Integer typeSpecimenCount;
  private String temporalCoverage;
  private Float queryRank;
  private Float queryDescriptorRank;
}
