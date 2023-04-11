package org.gbif.registry.service.collections.batch.model;

import org.apache.commons.math3.analysis.function.Exp;

import org.gbif.api.model.common.export.ExportFormat;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface ParserResult<T> {
  Map<String, ParsedData<T>> getParsedDataMap();

  List<String> getDuplicates();

  List<String> getFileErrors();

  Map<String, Integer> getFileHeadersIndex();

  Function<T, String> getEntityKeyExtractor();

  ExportFormat getFormat();
}
