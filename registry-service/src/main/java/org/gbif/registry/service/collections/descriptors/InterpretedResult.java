package org.gbif.registry.service.collections.descriptors;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InterpretedResult<T> {

  T result;
  List<String> issues;

  public static <T> InterpretedResult<T> empty() {
    return new InterpretedResult<>(null, null);
  }
}
