package org.gbif.registry.pipelines;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import static org.gbif.api.model.pipelines.PipelineStep.MetricInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MetricsHandlerTest {

  private static final String METRICS_RESPONSE =
      "{"
          + "    \"took\": 25,"
          + "    \"timed_out\": false,"
          + "    \"_shards\": {"
          + "        \"total\": 80,"
          + "        \"successful\": 80,"
          + "        \"skipped\": 0,"
          + "        \"failed\": 0"
          + "    },"
          + "    \"hits\": {"
          + "        \"total\": 95,"
          + "        \"max_score\": 0.0,"
          + "        \"hits\": []"
          + "    },"
          + "    \"aggregations\": {"
          + "        \"unique_name\": {"
          + "            \"doc_count_error_upper_bound\": 0,"
          + "            \"sum_other_doc_count\": 0,"
          + "            \"buckets\": ["
          + "                {"
          + "                    \"key\": \"driver.PipelinesOptionsFactory.Beam.Metrics.Merging_to_json_ParMultiDo_Anonymous.org_gbif_pipelines_transforms_converters_GbifJsonTransform.avroToJsonCount\","
          + "                    \"doc_count\": 93,"
          + "                    \"max_value\": {"
          + "                        \"value\": 2009360.0"
          + "                    }"
          + "                },"
          + "                {"
          + "                    \"key\": \"driver.PipelinesOptionsFactory.Beam.Metrics.Filter_records_without_gbifId_ParMultiDo_FilterMissedGbifIdTransform.org_gbif_pipelines_transforms_FilterMissedGbifIdTransform.missedGbifIdCount\","
          + "                    \"doc_count\": 2,"
          + "                    \"max_value\": {"
          + "                        \"value\": 236.0"
          + "                    }"
          + "                }"
          + "            ]"
          + "        }"
          + "    }"
          + "}";

  @Test
  public void getMetricsTest() throws IOException {
    MetricsHandler metricsHandler = new MetricsHandler(null, null);

    Set<MetricInfo> metrics = metricsHandler.parseResponse(METRICS_RESPONSE);

    assertEquals(2, metrics.size());

    MetricInfo expectedMetric1 =
        new MetricInfo("converters_GbifJsonTransform.avroToJsonCount", "2009360.0");
    assertTrue(metrics.contains(expectedMetric1));
    MetricInfo expectedMetric2 =
        new MetricInfo("FilterMissedGbifIdTransform.missedGbifIdCount", "236.0");
    assertTrue(metrics.contains(expectedMetric2));
  }
}
