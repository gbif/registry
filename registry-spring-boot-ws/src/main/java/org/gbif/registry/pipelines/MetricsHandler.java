package org.gbif.registry.pipelines;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.gbif.api.model.pipelines.StepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.gbif.api.model.pipelines.PipelineStep.MetricInfo;

@Component
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

  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("00");
  private static final MediaType JSON = MediaType.parse("application/json");
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String METRIC_NAME_FILTER = "org_gbif_pipelines_transforms_";

  private final OkHttpClient client;
  private final String esHost;
  private final String env;

  public MetricsHandler(@Value("${pipelines.esHost}") String esHost, @Value("${pipelines.envPrefix}") String envPrefix) {
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
      return parseResponse(client.newCall(request).execute().body().string());
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
    StringBuilder url =
        new StringBuilder(esHost).append("/").append(env).append("-pipeline-metric-");
    url.append(startedDate.getYear()).append(".");

    if (startedDate.getMonthValue() == finishedTime.getMonthValue()) {
      url.append(DECIMAL_FORMAT.format(startedDate.getMonthValue())).append(".");
      url.append(
          startedDate.getDayOfMonth() == finishedTime.getDayOfMonth()
              ? DECIMAL_FORMAT.format(startedDate.getDayOfMonth())
              : "*");
    } else {
      url.append("*");
    }

    return url.append("/_search").toString();
  }

  @VisibleForTesting
  Set<MetricInfo> parseResponse(String bodyResponse) throws IOException {
    if (Strings.isNullOrEmpty(bodyResponse)) {
      return Collections.emptySet();
    }

    JsonNode jsonResponse = OBJECT_MAPPER.readTree(bodyResponse);

    if (jsonResponse == null || jsonResponse.get("aggregations") == null) {
      return Collections.emptySet();
    }

    JsonNode buckets = jsonResponse.get("aggregations").get(UNIQUE_NAME_AGG).get("buckets");

    Set<MetricInfo> metrics = new HashSet<>();
    for (JsonNode bucket : buckets) {
      String key = bucket.get("key").asText();

      String metricName =
          key.substring(key.indexOf(METRIC_NAME_FILTER) + METRIC_NAME_FILTER.length());
      if (Strings.isNullOrEmpty(metricName)) {
        continue;
      }

      metrics.add(new MetricInfo(metricName, bucket.get(MAX_VALUE_AGG).get("value").asText()));
    }

    return metrics;
  }
}
