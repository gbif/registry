package org.gbif.registry.service.collections.batch.model;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.common.export.ExportFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import lombok.Builder;

@Builder
public class EntitiesParserResult<T extends CollectionEntity> implements ParserResult<T> {

  private Map<String, ParsedData<T>> parsedDataMap;
  private List<String> duplicates;
  private List<String> fileErrors = new ArrayList<>();
  private Map<String, Integer> fileHeadersIndex;
  private ExportFormat format;

  @Override
  public Map<String, ParsedData<T>> getParsedDataMap() {
    return parsedDataMap;
  }

  @Override
  public List<String> getDuplicates() {
    return duplicates;
  }

  @Override
  public List<String> getFileErrors() {
    return fileErrors;
  }

  @Override
  public Map<String, Integer> getFileHeadersIndex() {
    return fileHeadersIndex;
  }

  @Override
  public Function<T, String> getEntityKeyExtractor() {
    return e -> e.getKey() != null ? e.getKey().toString() : null;
  }

  @Override
  public ExportFormat getFormat() {
    return format;
  }
}
