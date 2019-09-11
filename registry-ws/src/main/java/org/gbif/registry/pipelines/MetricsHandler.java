package org.gbif.registry.pipelines;

import org.gbif.api.model.pipelines.StepType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.api.model.pipelines.PipelineStep.MetricInfo;

public class MetricsHandler {

  private static final Logger LOG = LoggerFactory.getLogger(MetricsHandler.class);

  private static final String UNIQUE_NAME_AGG = "unique_name";
  private static final String MAX_VALUE_AGG = "max_value";
  private static final String METRIC_QUERY_TEMPLATE =
      "{"
          + "  \"size\": 0,"
          + "  \"query\": {"
          + "    \"bool\": {"
          + "      \"must\": ["
          + "        {"
          + "          \"range\": {"
          + "            \"@timestamp\": {"
          + "              \"gte\": \"%s\""
          + "            }"
          + "          }"
          + "        },"
          + "        {"
          + "          \"match\": {"
          + "            \"datasetId\": {"
          + "              \"query\": \"%s\""
          + "            }"
          + "          }"
          + "        },"
          + "        {"
          + "          \"match\": {"
          + "            \"attempt\": {"
          + "              \"query\": \"%s\""
          + "            }"
          + "          }"
          + "        },"
          + "        {"
          + "          \"match\": {"
          + "            \"step\": {"
          + "              \"query\": \"%s\""
          + "            }"
          + "          }"
          + "        },"
          + "        {"
          + "          \"match\": {"
          + "            \"type\": {"
          + "              \"query\": \"GAUGE\""
          + "            }"
          + "          }"
          + "        },"
          + "        {"
          + "          \"match_phrase_prefix\": {"
          + "            \"name\": {"
          + "              \"query\": \"driver.PipelinesOptionsFactory\""
          + "            }"
          + "          }"
          + "        }"
          + "      ]"
          + "    }"
          + "  },"
          + "  \"aggregations\": {"
          + "    \""
          + UNIQUE_NAME_AGG
          + "\": {"
          + "      \"terms\": {"
          + "        \"field\": \"name.keyword\","
          + "        \"size\": 10"
          + "      },"
          + "      \"aggregations\": {"
          + "        \""
          + MAX_VALUE_AGG
          + "\": {"
          + "          \"max\": {"
          + "            \"field\": \"value\""
          + "          }"
          + "        }"
          + "      }"
          + "    }"
          + "  }"
          + "}";

  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String METRIC_NAME_FILTER = "org_gbif_pipelines_transforms_";

  private final OkHttpClient client;
  private final String esHost;
  private final String env;

  @Inject
  public MetricsHandler(@Named("esHost") String esHost, @Named("envPrefix") String envPrefix) {
    this.client = new OkHttpClient();
    this.esHost = esHost;
    this.env = envPrefix;
  }

  public Set<MetricInfo> getMetricInfo(
      UUID datasetKey,
      int attempt,
      StepType stepType,
      LocalDateTime startedDate,
      LocalDateTime finishedTime) {

    RequestBody body =
        RequestBody.create(
            JSON,
            String.format(
                METRIC_QUERY_TEMPLATE, startedDate, datasetKey, attempt, stepType.name()));

    Request request =
        new Request.Builder().url(getIndexUrl(startedDate, finishedTime)).post(body).build();

    try {
      return parseResponse(client.newCall(request).execute().body().bytes());
    } catch (Exception e) {
      LOG.warn(
          "Couldn't get metrics from ES for dataset {}, attempt {}, step {} and started date {}",
          datasetKey,
          attempt,
          stepType,
          startedDate,
          e);
    }

    return Collections.emptySet();
  }

  private String getIndexUrl(LocalDateTime startedDate, LocalDateTime finishedTime) {
    String index = esHost + env + "-pipeline-metric-" + startedDate.getYear() + ".";

    if (startedDate.getMonthValue() == finishedTime.getMonthValue()) {
      index += startedDate.getMonthValue() + ".";
      index +=
          startedDate.getDayOfMonth() == finishedTime.getDayOfMonth()
              ? startedDate.getDayOfMonth()
              : "*";
    } else {
      index += "*";
    }

    return index;
  }

  @VisibleForTesting
  Set<MetricInfo> parseResponse(byte[] bodyResponse) throws IOException {
    if (bodyResponse == null) {
      return Collections.emptySet();
    }

    JsonNode jsonResponse = OBJECT_MAPPER.readTree(bodyResponse);

    JsonNode buckets = jsonResponse.get("aggregations").get(UNIQUE_NAME_AGG).get("buckets");

    Set<MetricInfo> metrics = new HashSet<>();
    for (JsonNode bucket : buckets) {
      String key = bucket.get("key").asText();
      metrics.add(
          new MetricInfo(
              key.substring(key.indexOf(METRIC_NAME_FILTER) + METRIC_NAME_FILTER.length()),
              bucket.get(MAX_VALUE_AGG).get("value").asText()));
    }

    return metrics;
  }
}
