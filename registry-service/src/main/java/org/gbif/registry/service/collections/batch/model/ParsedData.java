package org.gbif.registry.service.collections.batch.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ParsedData<T> {
  T entity;
  List<String> errors;
}
