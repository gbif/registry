package org.gbif.registry.ws.export;

import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.occurrence.DownloadStatistics;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.vocabulary.Country;

import java.io.Writer;
import java.util.Date;
import java.util.UUID;


import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.CsvContext;

@Data
@Builder
public class CsvWriter<T> {

  private final String[] header;

  private final String[] fields;

  private final CellProcessor[] processors;

  private final Iterable<T> pager;

  private final ExportFormat preference;


  private CsvPreference csvPreference() {
    if (ExportFormat.CSV == preference) {
      return CsvPreference.STANDARD_PREFERENCE;
    } else if (ExportFormat.TSV == preference) {
      return CsvPreference.TAB_PREFERENCE;
    }
    throw new IllegalArgumentException("Export format not supported " + preference);
  }

  @SneakyThrows
  public void export(Writer writer) {
    try (ICsvBeanWriter beanWriter = new CsvBeanWriter(writer, csvPreference())) {
      beanWriter.writeHeader(header);
      for (T o : pager) {
        beanWriter.write(o, fields, processors);
      }
      writer.flush();
    }
  }

  /**
   * Creates and CsvWriter/exporter DownloadStatistics.
   */
  public static CsvWriter<DownloadStatistics> downloadStatisticsTsvWriter(Iterable<DownloadStatistics> pager,
                                                                          ExportFormat preference) {

    return CsvWriter.<DownloadStatistics>builder()
              .fields(new String[]{"datasetKey", "totalRecords", "numberDownloads", "year", "month"})
              .header(new String[]{"dataset_key", "total_records", "number_downloads", "year", "month"})
              .processors(new CellProcessor[]{new UUIDProcessor(),
                                              new Optional(new ParseInt()),
                                              new Optional(new ParseInt()),
                                              new Optional(new ParseInt()),
                                              new Optional(new ParseInt())})
              .preference(preference)
              .pager(pager)
              .build();
  }

  /**
   * Null aware UUID processor.
   */
  private static class UUIDProcessor implements CellProcessor {
    @Override
    public String execute(Object value, CsvContext csvContext) {
      return value != null ? ((UUID) value).toString() : "";
    }
  }

}
