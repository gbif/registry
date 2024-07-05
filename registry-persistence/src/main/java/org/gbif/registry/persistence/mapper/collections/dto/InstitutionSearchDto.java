package org.gbif.registry.persistence.mapper.collections.dto;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.collections.MasterSourceType;

@EqualsAndHashCode(callSuper = true)
@Data
public class InstitutionSearchDto extends BaseSearchDto {

  private List<String> types = new ArrayList<>();
  private List<String> institutionalGovernances = new ArrayList<>();
  private List<String> disciplines = new ArrayList<>();
  private BigDecimal latitude;
  private BigDecimal longitude;
  private Integer foundingDate;
  private Integer numberSpecimens;
  private MasterSourceType masterSource;
  private Integer occurrenceCount;
  private Integer typeSpecimenCount;

}
