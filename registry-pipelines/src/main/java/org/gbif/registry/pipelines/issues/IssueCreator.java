/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.pipelines.issues;

import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.occurrence.Occurrence;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.model.occurrence.search.OccurrenceSearchRequest;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.metrics.ws.client.CubeWsClient;
import org.gbif.occurrence.ws.client.OccurrenceWsSearchClient;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.pipelines.issues.GithubApiClient.Issue;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apache.avro.generic.GenericRecord;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class IssueCreator {

  private static final int NUM_SAMPLE_IDS = 10;
  private static final String IDS_VALIDATION_FAILED_TITLE =
      "Identifiers validation failed for dataset %s";

  private static final String ISSUE_BODY_INTRO = "Identifier validation failed for the dataset ";
  private static final String COMMENT_BODY_INTRO =
      "Identifier validation still  failing for the dataset ";

  private static final String INGESTION_HISTORY_LINK_NAME = "the registry UI";
  private static final String INGESTION_HISTORY_LINK_TEMPLATE = "%s/dataset/%s/ingestion-history";
  private static final String DATASET_LINK_TEMPLATE = "%s/dataset/%s";
  private static final String ORGANIZATION_LINK_TEMPLATE = "%s/organization/%s";
  private static final String INSTALLATION_LINK_TEMPLATE = "%s/installation/%s";
  private static final String EXECUTION_LINK_TEMPLATE = "%s/pipelines/history/execution/%s/step";
  private static final String COUNTRY_LABEL_TEMPLATE = "Country %s";
  private static final String ATTEMPT_LABEL_TEMPLATE = "Attempt %s";
  private static final String PUBLISHER_LABEL_TEMPLATE = "pub: %s";
  private static final String INSTALLATION_LABEL_TEMPLATE = "inst: %s";
  private static final String ENVIRONMENT_LABEL_TEMPLATE = "env: %s";
  private static final UnaryOperator<String> PORTAL_URL_NORMALIZER =
      url -> {
        if (url != null && url.endsWith("/")) {
          return url.substring(0, url.length() - 1);
        }
        return url;
      };
  private static final String NEW_LINE = "\n";
  private static final String CODE_BLOCK_SEPARATOR = "```";
  private static final String DATASET_NOT_FOUND_FOR_KEY = "Dataset not found for key: ";
  private final DatasetMapper datasetMapper;
  private final OrganizationMapper organizationMapper;
  private final InstallationMapper installationMapper;
  private final String registryUrl;
  private final String apiRootUrl;
  private final CubeWsClient cubeWsClient;
  private final OccurrenceWsSearchClient occurrenceWsSearchClient;
  private final IssuesConfig issuesConfig;

  @Autowired
  public IssueCreator(
      DatasetMapper datasetMapper,
      OrganizationMapper organizationMapper,
      InstallationMapper installationMapper,
      IssuesConfig issuesConfig,
      @Value("${pipelines.registryUrl}") String registryUrl,
      @Value("${api.root.url}") String apiRootUrl) {
    this.datasetMapper = datasetMapper;
    this.organizationMapper = organizationMapper;
    this.installationMapper = installationMapper;
    this.issuesConfig = issuesConfig;
    this.registryUrl = registryUrl;
    this.apiRootUrl = apiRootUrl;
    this.cubeWsClient =
        new ClientBuilder()
            .withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport())
            .withUrl(apiRootUrl)
            .build(CubeWsClient.class);
    this.occurrenceWsSearchClient =
        new ClientBuilder()
            .withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport())
            .withUrl(apiRootUrl)
            .build(OccurrenceWsSearchClient.class);
  }

  public static String getCurrentTimestamp() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  }

  @SneakyThrows
  private static Configuration getHdfsConfiguration(String hdfsSiteConfig) {
    Configuration config = new Configuration();

    // check if the hdfs-site.xml is provided
    if (!Strings.isNullOrEmpty(hdfsSiteConfig)) {
      File hdfsSite = new File(hdfsSiteConfig);
      if (hdfsSite.exists() && hdfsSite.isFile()) {
        log.info("using hdfs-site.xml");
        config.addResource(hdfsSite.toURI().toURL());
      } else {
        log.warn("hdfs-site.xml does not exist");
      }
    }
    return config;
  }

  public Issue createIdsValidationFailedIssue(
      UUID datasetKey, int attempt, long executionKey, String cause) {
    Dataset dataset = datasetMapper.getLightweight(datasetKey);

    if (dataset == null) {
      throw new IllegalArgumentException(DATASET_NOT_FOUND_FOR_KEY + datasetKey);
    }

    Organization organization =
        organizationMapper.getLightweight(dataset.getPublishingOrganizationKey());
    Installation installation = installationMapper.getLightweight(dataset.getInstallationKey());

    boolean errorLoadingNewIds = false;
    boolean errorLoadingExistingIds = false;
    List<String> newIds = new ArrayList<>();
    List<String> oldIds = new ArrayList<>();

    try {
      newIds = readIdentifiersFromAvro(datasetKey, attempt);
    } catch (Exception e) {
      errorLoadingNewIds = true;
      log.error("Error reading identifiers from avro dataset", e);
    }

    try {
      oldIds = readIdentifiersFromES(datasetKey);
    }  catch (Exception e) {
      errorLoadingExistingIds = true;
      log.error("Error reading identifiers from ES dataset", e);
    }

    String body =
        buildIssueBody(
            ISSUE_BODY_INTRO,
            datasetKey,
            attempt,
            executionKey,
            cause,
            dataset,
            organization,
            installation,
            newIds,
            oldIds,
            errorLoadingNewIds,
            errorLoadingExistingIds);
    return Issue.builder()
        .title(String.format(IDS_VALIDATION_FAILED_TITLE, dataset.getTitle()))
        .body(body)
        .labels(
            Sets.newHashSet(
                datasetKey.toString(),
                String.format(ATTEMPT_LABEL_TEMPLATE, attempt),
                String.format(COUNTRY_LABEL_TEMPLATE, organization.getCountry()),
                String.format(PUBLISHER_LABEL_TEMPLATE, organization.getKey()),
                String.format(INSTALLATION_LABEL_TEMPLATE, installation.getKey()),
                String.format(ENVIRONMENT_LABEL_TEMPLATE, issuesConfig.environment),
                getCurrentTimestamp()))
        .build();
  }

  @NotNull
  private String buildIssueBody(
      String intro,
      UUID datasetKey,
      int attempt,
      long executionKey,
      String cause,
      Dataset dataset,
      Organization organization,
      Installation installation,
      List<String> newIds,
      List<String> oldIds,
      boolean errorLoadingNewIds,
      boolean errorLoadingExistingIds
      ) {

    long occCount =
        cubeWsClient.get(new LinkedMultiValueMap<>(Collections.singletonMap("datasetKey", Collections.singletonList(datasetKey.toString()))));

    StringBuilder body = new StringBuilder();
    body.append(intro)
        .append(
            createRegistryLink(DATASET_LINK_TEMPLATE, dataset.getTitle(), datasetKey.toString()))
        .append(":")
        .append(NEW_LINE)
        .append("- Crawler attempt: ")
        .append(attempt)
        .append(NEW_LINE)
        .append("- Publishing organization: ")
        .append(
            createRegistryLink(
                ORGANIZATION_LINK_TEMPLATE,
                organization.getTitle(),
                organization.getKey().toString()))
        .append(NEW_LINE)
        .append("- Publishing country: ")
        .append(organization.getCountry())
        .append(NEW_LINE)
        .append("- Installation: ")
        .append(
            createRegistryLink(
                INSTALLATION_LINK_TEMPLATE,
                installation.getTitle(),
                installation.getKey().toString()))
        .append(NEW_LINE)
        .append("- Number of occurrences indexed for this dataset at the time of the issue: ")
        .append(occCount)
        .append(NEW_LINE)
        .append("- Cause: ")
        .append(cause)
        .append(NEW_LINE)
        .append("- Pipelines execution steps: ")
        .append(createApiLink(EXECUTION_LINK_TEMPLATE, null, String.valueOf(executionKey)))
        .append(NEW_LINE)
        .append(createIdsBlock(newIds, true))
        .append(NEW_LINE)
        .append(createIdsBlock(oldIds, false))
        .append(NEW_LINE)
        .append(buildPublisherEmailSection(dataset))
        .append(NEW_LINE)
        .append(NEW_LINE)
        .append("You can skip/fix identifier validation using ")
        .append(
            createRegistryLink(
                INGESTION_HISTORY_LINK_TEMPLATE,
                INGESTION_HISTORY_LINK_NAME,
                datasetKey.toString()))
        .append(".");

    if (errorLoadingNewIds) {
      body.append(NEW_LINE).append(NEW_LINE).append("**WARNING - Error loading new identifiers from Parquet files.**");
    }
    if (errorLoadingExistingIds) {
      body.append(NEW_LINE).append(NEW_LINE).append("**WARNING - Error loading new identifiers from Elastic.**");
    }

    return body.toString();
  }

  public GithubApiClient.IssueComment createIdsValidationFailedIssueComment(
      UUID datasetKey, int attempt, long executionKey, String cause) {
    Dataset dataset = datasetMapper.getLightweight(datasetKey);

    if (dataset == null) {
      throw new IllegalArgumentException(DATASET_NOT_FOUND_FOR_KEY + datasetKey);
    }

    boolean errorLoadingNewIds = false;
    boolean errorLoadingExistingIds = false;
    List<String> newIds = new ArrayList<>();
    List<String> oldIds = new ArrayList<>();

    try {
      newIds = readIdentifiersFromAvro(datasetKey, attempt);
    } catch (Exception e) {
      errorLoadingNewIds = true;
      log.error("Error reading identifiers from Avro dataset", e);
    }
    try {
      oldIds = readIdentifiersFromES(datasetKey);
    }  catch (Exception e) {
      errorLoadingExistingIds = true;
      log.error("Error reading identifiers from ES dataset", e);
    }

    Organization organization =
        organizationMapper.getLightweight(dataset.getPublishingOrganizationKey());
    Installation installation = installationMapper.getLightweight(dataset.getInstallationKey());

    String body =
        buildIssueBody(
            COMMENT_BODY_INTRO,
            datasetKey,
            attempt,
            executionKey,
            cause,
            dataset,
            organization,
            installation,
            newIds,
            oldIds,
            errorLoadingNewIds,
            errorLoadingExistingIds);

    return GithubApiClient.IssueComment.builder().body(body).build();
  }

  public Set<String> updateLabels(
      GithubApiClient.IssueResult issueResult, UUID datasetKey, int attempt) {
    Dataset dataset = datasetMapper.getLightweight(datasetKey);

    if (dataset == null) {
      throw new IllegalArgumentException(DATASET_NOT_FOUND_FOR_KEY + datasetKey);
    }

    Organization organization =
        organizationMapper.getLightweight(dataset.getPublishingOrganizationKey());
    Installation installation = installationMapper.getLightweight(dataset.getInstallationKey());

    Set<String> existingLabels =
        issueResult.getLabels().stream()
            .map(GithubApiClient.IssueResult.Label::getName)
            .collect(Collectors.toSet());
    existingLabels.add(String.format(ATTEMPT_LABEL_TEMPLATE, attempt));
    existingLabels.add(String.format(COUNTRY_LABEL_TEMPLATE, organization.getCountry()));
    existingLabels.add(String.format(PUBLISHER_LABEL_TEMPLATE, organization.getKey()));
    existingLabels.add(String.format(INSTALLATION_LABEL_TEMPLATE, installation.getKey()));
    existingLabels.add(IssueCreator.getCurrentTimestamp());

    return existingLabels;
  }

  private String createRegistryLink(String template, String linkName, String key) {
    return createMarkdownLink(template, registryUrl, key, linkName);
  }

  private String createApiLink(String template, String linkName, String key) {
    return createMarkdownLink(template, apiRootUrl, key, linkName);
  }

  @NotNull
  private String createMarkdownLink(String template, String baseUrl, String key, String linkName) {
    URI uri = URI.create(String.format(template, PORTAL_URL_NORMALIZER.apply(baseUrl), key));
    return "[" + (linkName != null ? linkName : uri) + "](" + uri + ")";
  }

  @SneakyThrows
  private String buildPublisherEmailSection(Dataset dataset) {
    ClassPathResource templateFile =
        new ClassPathResource("/templates/ids_validation_publisher_email");

    String content = IOUtils.toString(templateFile.getInputStream(), StandardCharsets.UTF_8);
    content =
        content.replace(
            "${datasetLink}",
            createRegistryLink(
                DATASET_LINK_TEMPLATE, dataset.getTitle(), dataset.getKey().toString()));
    content = content.replace("${doi}", dataset.getDoi().getUrl().toString());

    StringBuilder sb = new StringBuilder();
    sb.append("<details>")
        .append(NEW_LINE)
        .append("<summary>Publisher email</summary>")
        .append(NEW_LINE)
        .append(NEW_LINE)
        .append(content)
        .append(NEW_LINE)
        .append("</details>")
        .append(NEW_LINE);

    return sb.toString();
  }

  private String createIdsBlock(List<String> ids, boolean newIds) {
    StringBuilder sb = new StringBuilder();

    sb.append(NEW_LINE).append(CODE_BLOCK_SEPARATOR).append(NEW_LINE);
    sb.append(newIds ? "New" : "Old").append(" IDs sample:").append(NEW_LINE).append(NEW_LINE);

    boolean first = true;
    for (String newId : ids) {
      if (!first) {
        sb.append(NEW_LINE);
      }
      first = false;
      sb.append(newId);
    }

    sb.append(NEW_LINE).append(CODE_BLOCK_SEPARATOR).append(NEW_LINE);

    return sb.toString();
  }

  @SneakyThrows
  private List<String> readIdentifiersFromAvro(UUID datasetKey, int attempt) {

    List<String> identifiers = new ArrayList<>();
    FileSystem fs =
      FileSystem.get(
        URI.create(issuesConfig.hdfsPrefix), getHdfsConfiguration(issuesConfig.hdfsSiteConfig));

    org.apache.hadoop.fs.Path absentIdentifiersPath =
      new org.apache.hadoop.fs.Path(
        issuesConfig.hdfsPrefix
          + issuesConfig.ingestDirectoryBasePath
          + "/"
          + datasetKey
          + "/"
          + attempt
          + "/identifiers_absent");

    org.apache.hadoop.fs.Path invalidIdentifiersPath =
      new org.apache.hadoop.fs.Path(
        issuesConfig.hdfsPrefix
          + issuesConfig.ingestDirectoryBasePath
          + "/"
          + datasetKey
          + "/"
          + attempt
          + "/identifiers_invalid");

    org.apache.hadoop.fs.Path validIdentifiersPath =
      new org.apache.hadoop.fs.Path(
        issuesConfig.hdfsPrefix
          + issuesConfig.ingestDirectoryBasePath
          + "/"
          + datasetKey
          + "/"
          + attempt
          + "/identifiers_valid");

    for (org.apache.hadoop.fs.Path identifiersPath :
      List.of(absentIdentifiersPath, invalidIdentifiersPath, validIdentifiersPath)) {
      try {
        RemoteIterator<LocatedFileStatus> iterator = fs.listFiles(identifiersPath, false);
        while (iterator.hasNext() && identifiers.size() < NUM_SAMPLE_IDS) {
          LocatedFileStatus fileStatus = iterator.next();
          if (fileStatus.isFile() && fileStatus.getPath().getName().endsWith(".parquet")) {
            // Use AvroParquetReader to read parquet files containing Avro GenericRecords
            org.apache.hadoop.fs.Path parquetPath = fileStatus.getPath();
            try (ParquetReader<GenericRecord> reader =
                   AvroParquetReader.<GenericRecord>builder(parquetPath).withConf(fs.getConf()).build()) {
              GenericRecord record;
              while ((record = reader.read()) != null && identifiers.size() < NUM_SAMPLE_IDS) {
                if (record.get("id") != null) {
                  identifiers.add(record.get("id").toString());
                }
              }
            }
          }
        }
      } catch (FileNotFoundException ex) {
        throw new IllegalArgumentException(
          "Identifier parquet files not found for dataset " + datasetKey + " and attempt " + attempt
            + ". Error " + ex.getMessage(), ex);
      }
    }
    return identifiers;
  }

  private List<String> readIdentifiersFromES(UUID datasetKey) {
    OccurrenceSearchRequest request = new OccurrenceSearchRequest();
    request.setLimit(NUM_SAMPLE_IDS);
    request.addDatasetKeyFilter(datasetKey);
    SearchResponse<Occurrence, OccurrenceSearchParameter> response =
        occurrenceWsSearchClient.search(request);
    return response.getResults().stream()
        .map(o -> o.getVerbatimField(DwcTerm.occurrenceID))
        .filter(v -> !Strings.isNullOrEmpty(v))
        .collect(Collectors.toList());
  }
}
