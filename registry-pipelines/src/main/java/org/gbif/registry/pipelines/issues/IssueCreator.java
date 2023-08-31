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

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.metrics.ws.client.CubeWsClient;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.pipelines.PipelineProcessMapper;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import lombok.SneakyThrows;

import static org.gbif.registry.pipelines.issues.GithubApiService.Issue;

@Component
public class IssueCreator {
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
  private static final UnaryOperator<String> PORTAL_URL_NORMALIZER =
      url -> {
        if (url != null && url.endsWith("/")) {
          return url.substring(0, url.length() - 1);
        }
        return url;
      };
  private static final String NEW_LINE = "\n";
  private static final String CODE_BLOCK_SEPARATOR = "```";
  private final DatasetMapper datasetMapper;
  private final OrganizationMapper organizationMapper;
  private final InstallationMapper installationMapper;
  private final String registryUrl;
  private final String apiRootUrl;
  private final CubeWsClient cubeWsClient;
  private final PipelineProcessMapper pipelineProcessMapper;

  @Autowired
  public IssueCreator(
      DatasetMapper datasetMapper,
      OrganizationMapper organizationMapper,
      InstallationMapper installationMapper,
      PipelineProcessMapper pipelineProcessMapper,
      @Value("${pipelines.registryUrl}") String registryUrl,
      @Value("${api.root.url}") String apiRootUrl) {
    this.datasetMapper = datasetMapper;
    this.organizationMapper = organizationMapper;
    this.installationMapper = installationMapper;
    this.pipelineProcessMapper = pipelineProcessMapper;
    this.registryUrl = registryUrl;
    this.apiRootUrl = apiRootUrl;
    this.cubeWsClient =
        new ClientBuilder()
            .withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport())
            .withUrl(apiRootUrl)
            .build(CubeWsClient.class);
  }

  public Issue createIdsValidationFailedIssue(
      UUID datasetKey, int attempt, long executionKey, String cause) {
    Dataset dataset = datasetMapper.getLightweight(datasetKey);

    if (dataset == null) {
      throw new IllegalArgumentException("Dataset not found for key: " + datasetKey);
    }

    Organization organization =
        organizationMapper.getLightweight(dataset.getPublishingOrganizationKey());
    Installation installation = installationMapper.getLightweight(dataset.getInstallationKey());

    String body =
        buildIssueBody(
            ISSUE_BODY_INTRO,
            datasetKey,
            attempt,
            executionKey,
            cause,
            dataset,
            organization,
            installation);
    return Issue.builder()
        .title(String.format(IDS_VALIDATION_FAILED_TITLE, dataset.getTitle()))
        .body(body)
        .labels(
            Sets.newHashSet(
                datasetKey.toString(),
                String.format(ATTEMPT_LABEL_TEMPLATE, attempt),
                String.format(COUNTRY_LABEL_TEMPLATE, organization.getCountry()),
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
      Installation installation) {

    long occCount =
        cubeWsClient.count(Collections.singletonMap("datasetKey", datasetKey.toString()));

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

    return body.toString();
  }

  public static String getCurrentTimestamp() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  }

  public GithubApiService.IssueComment createIdsValidationFailedIssueComment(
      UUID datasetKey, int attempt, long executionKey, String cause) {
    Dataset dataset = datasetMapper.getLightweight(datasetKey);

    if (dataset == null) {
      throw new IllegalArgumentException("Dataset not found for key: " + datasetKey);
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
            installation);

    return GithubApiService.IssueComment.builder().body(body).build();
  }

  public Set<String> updateLabels(
      GithubApiService.IssueResult issueResult, UUID datasetKey, int attempt) {
    Dataset dataset = datasetMapper.getLightweight(datasetKey);

    if (dataset == null) {
      throw new IllegalArgumentException("Dataset not found for key: " + datasetKey);
    }

    Organization organization =
        organizationMapper.getLightweight(dataset.getPublishingOrganizationKey());

    Set<String> existingLabels =
        issueResult.getLabels().stream()
            .map(GithubApiService.IssueResult.Label::getName)
            .collect(Collectors.toSet());
    existingLabels.add(String.format(ATTEMPT_LABEL_TEMPLATE, attempt));
    existingLabels.add(String.format(COUNTRY_LABEL_TEMPLATE, organization.getCountry()));
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
}
