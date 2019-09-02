package org.gbif.ws.mixin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Date;

public interface OccurrenceMixin extends LicenseMixin {

  @JsonSerialize(using = DateSerde.NoTimezoneDateJsonSerializer.class)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  Date getDateIdentified();

  @JsonSerialize(using = DateSerde.NoTimezoneDateJsonSerializer.class)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  Date getEventDate();
}
