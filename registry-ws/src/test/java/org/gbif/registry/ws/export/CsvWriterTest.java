package org.gbif.registry.ws.export;

import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.occurrence.DownloadStatistics;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CsvWriterTest {

  @Test
  public void downloadStatisticsTest() {

    List<DownloadStatistics> stats = Arrays.asList(
      new DownloadStatistics(UUID.randomUUID(), 10, 10, LocalDate.of(2020,1,1)),
      new DownloadStatistics(UUID.randomUUID(), 10, 10, LocalDate.of(2021, 2,1)));

    StringWriter writer = new StringWriter();

    CsvWriter.downloadStatisticsTsvWriter(stats, ExportFormat.TSV)
    .export(writer);

    String export = writer.toString();
    String[] lines = export.split("\\n");

    //Number of lines is header + list.size
    assertEquals(stats.size() + 1, lines.length);

    //Each line has 4 tabs
    assertEquals((stats.size() + 1) * 4, export.chars().filter(ch -> ch == '\t').count());

    //Year test
    assertEquals("2020", lines[1].split("\\t")[3]);
    assertEquals("2021", lines[2].split("\\t")[3]);

    //Month test
    assertEquals("1", lines[1].split("\\t")[4]);
    assertEquals("2", lines[2].split("\\t")[4]);
  }
}
