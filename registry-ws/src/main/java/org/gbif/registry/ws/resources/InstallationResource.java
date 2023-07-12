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
package org.gbif.registry.ws.resources;

import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.Explode;

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.annotation.Trim;
import org.gbif.api.documentation.CommonParameters;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.PostPersist;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.metasync.MetasyncHistory;
import org.gbif.api.model.registry.search.InstallationRequestSearchParams;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.MetasyncHistoryService;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.StartMetasyncMessage;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.MetaSyncHistoryMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.params.BaseListParams;
import org.gbif.registry.persistence.mapper.params.DatasetListParams;
import org.gbif.registry.persistence.mapper.params.InstallationListParams;
import org.gbif.registry.persistence.service.MapperServiceLocator;
import org.gbif.registry.service.WithMyBatis;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;

@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Technical installations",
    description =
        "A **technical installation** serves datasets.  They usually represent installations of the "
            + "[GBIF IPT](https://www.gbif.org/ipt) or other HTTP-accessible data repositories.\n\n"
            + "The installation API provides CRUD and discovery services for installations.\n\n"
            + "Please note deletion of installations is logical, meaning installation entries remain registered forever and only get a "
            + "deleted timestamp. On the other hand, deletion of an installation's contacts, endpoints, identifiers, tags, "
            + "machine tags, comments, and metadata descriptions is physical, meaning the entries are permanently removed.",
    extensions =
        @io.swagger.v3.oas.annotations.extensions.Extension(
            name = "Order",
            properties = @ExtensionProperty(name = "Order", value = "0500")))
@Validated
@Primary
@RestController
@RequestMapping(value = "installation", produces = MediaType.APPLICATION_JSON_VALUE)
public class InstallationResource
    extends BaseNetworkEntityResource<Installation, InstallationListParams>
    implements InstallationService, MetasyncHistoryService {

  private static final Logger LOG = LoggerFactory.getLogger(InstallationResource.class);

  private final DatasetMapper datasetMapper;
  private final InstallationMapper installationMapper;
  private final OrganizationMapper organizationMapper;
  private final MetaSyncHistoryMapper metasyncHistoryMapper;

  /** The messagePublisher can be optional. */
  private final MessagePublisher messagePublisher;

  public InstallationResource(
      MapperServiceLocator mapperServiceLocator,
      EventManager eventManager,
      WithMyBatis withMyBatis,
      @Autowired(required = false) MessagePublisher messagePublisher) {
    super(
        mapperServiceLocator.getInstallationMapper(),
        mapperServiceLocator,
        Installation.class,
        eventManager,
        withMyBatis);
    this.datasetMapper = mapperServiceLocator.getDatasetMapper();
    this.installationMapper = mapperServiceLocator.getInstallationMapper();
    this.organizationMapper = mapperServiceLocator.getOrganizationMapper();
    this.metasyncHistoryMapper = mapperServiceLocator.getMetaSyncHistoryMapper();
    this.messagePublisher = messagePublisher;
  }

  @Operation(
      operationId = "getInstallation",
      summary = "Get details of a single installation",
      description = "Details of a single installation.  Also works for deleted installations.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0200")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Installation found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}")
  @NullToNotFound("/installation/{key}")
  @Override
  public Installation get(@PathVariable UUID key) {
    return super.get(key);
  }

  /**
   * Creates a new installation.
   *
   * @param installation installation
   * @return key of entity created
   */
  // Method overridden only for documentation.
  @Operation(
      operationId = "createInstallation",
      summary = "Create a new installation",
      description =
          "Creates a new installation.  Note contacts, endpoints, identifiers, tags, machine tags, comments and "
              + "metadata descriptions must be added in subsequent requests.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0201")))
  @ApiResponse(
      responseCode = "201",
      description = "Installation created, new installation's UUID returned")
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Override
  public UUID create(@RequestBody @Trim Installation installation) {
    return super.create(installation);
  }

  /**
   * Updates the installation.
   *
   * @param installation installation
   */
  // Method overridden only for documentation.
  @Operation(
      operationId = "updateInstallation",
      summary = "Update an existing installation",
      description =
          "Updates the existing installation.  Note contacts, endpoints, identifiers, tags, machine tags, comments and "
              + "metadata descriptions are not changed with this method.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0202")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Installation updated")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping(value = "{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PostPersist.class, Default.class})
  @Override
  public void update(
      @PathVariable("key") UUID key, @Valid @RequestBody @Trim Installation installation) {
    super.update(key, installation);
  }

  @Override
  protected PagingResponse<Installation> list(BaseListParams params) {
    InstallationListParams p = InstallationListParams.from(params);
    return new PagingResponse<>(
        p.getPage(), installationMapper.count(p), installationMapper.list(p));
  }

  /**
   * Deletes the installation.
   *
   * @param key key of installation to delete
   */
  // Method overridden only for documentation.
  @Operation(
      operationId = "deleteInstallation",
      summary = "Delete an installation",
      description =
          "Marks an installation as deleted.  Note contacts, endpoints, identifiers, tags, machine tags, comments and "
              + "metadata descriptions are not changed.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0203")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Installation deleted")
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}")
  @Override
  public void delete(@PathVariable UUID key) {
    super.delete(key);
  }

  /**
   * All network entities support simple (!) search with "&q=". This is to support the console user
   * interface, and is in addition to any complex, faceted search that might additionally be
   * supported, such as dataset search.
   */
  @Operation(
      operationId = "listInstallations",
      summary = "List all installations",
      description = "Lists all current installations (deleted installations are not listed).",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0100")))
  @SimpleSearchParameters
  @CommonParameters.QParameter
  @Parameter(
      name = "type",
      description = "Filter by the type of installation.",
      schema = @Schema(implementation = InstallationType.class),
      in = ParameterIn.QUERY)
  @ApiResponse(responseCode = "200", description = "Installation search successful")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping
  @Override
  public PagingResponse<Installation> list(InstallationRequestSearchParams request) {
    return listInternal(request, false);
  }

  private PagingResponse<Installation> listInternal(
      InstallationRequestSearchParams request, boolean deleted) {
    InstallationListParams listParams =
        InstallationListParams.builder()
            .query(parseQuery(request.getQ()))
            .type(request.getType())
            .endorsedByNodeKey(request.getEndorsedByNodeKey())
            .organizationKey(request.getOrganizationKey())
            .from(parseFrom(request.getModified()))
            .to(parseTo(request.getModified()))
            .deleted(deleted)
            .identifier(request.getIdentifier())
            .identifierType(request.getIdentifierType())
            .mtNamespace(request.getMachineTagNamespace())
            .mtName(request.getMachineTagName())
            .mtValue(request.getMachineTagValue())
            .page(request.getPage())
            .build();

    long total = installationMapper.count(listParams);
    return pagingResponse(request.getPage(), total, installationMapper.list(listParams));
  }

  @Operation(
      operationId = "getInstallationDatasets",
      summary = "List installation's datasets",
      description = "Lists the datasets served by this installation.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0232")))
  @Docs.DefaultEntityKeyParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "List of datasets")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/dataset")
  @Override
  public PagingResponse<Dataset> getHostedDatasets(
      @PathVariable("key") UUID installationKey, Pageable page) {
    return new PagingResponse<>(
        page,
        new Long(
            datasetMapper.count(
                DatasetListParams.builder().installationKey(installationKey).build())),
        datasetMapper.list(
            DatasetListParams.builder().installationKey(installationKey).page(page).build()));
  }

  @Operation(
      operationId = "getDeletedInstallations",
      summary = "List deleted installations",
      description = "Lists deleted installations.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0500")))
  @SimpleSearchParameters
  @Parameter(
      name = "type",
      description = "Filter by the type of installation.",
      schema = @Schema(implementation = InstallationType.class),
      in = ParameterIn.QUERY)
  @CommonParameters.QParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "List of deleted installations")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("deleted")
  @Override
  public PagingResponse<Installation> listDeleted(InstallationRequestSearchParams searchParams) {
    return listInternal(searchParams, true);
  }

  @Operation(
      operationId = "getNonPublishingInstallations",
      summary = "List non-publishing installations",
      description = "Lists all installations serving 0 datasets.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0520")))
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "List of non-publishing installations")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("nonPublishing")
  @Override
  public PagingResponse<Installation> listNonPublishing(Pageable page) {
    return pagingResponse(
        page, installationMapper.countNonPublishing(), installationMapper.nonPublishing(page));
  }

  /**
   * This is a REST only (e.g. not part of the Java API) method that allows the registry console to
   * trigger the synchronization of the installation. This simply emits a message to rabbitmq
   * requesting the sync, and applies necessary security.
   */
  @Hidden
  @PostMapping("{key}/synchronize")
  @Secured(ADMIN_ROLE)
  public void synchronize(@PathVariable("key") UUID installationKey) {
    if (messagePublisher != null) {
      LOG.info("Requesting synchronizing installation[{}]", installationKey);
      try {
        messagePublisher.send(new StartMetasyncMessage(installationKey));
      } catch (IOException e) {
        LOG.error("Unable to send message requesting synchronization", e);
      }

    } else {
      LOG.warn(
          "Registry is configured to run without messaging capabilities.  Unable to synchronize installation[{}]",
          installationKey);
    }
  }

  /**
   * This is a REST only (e.g. not part of the Java API) method that allows you to get the locations
   * of installations as GeoJSON. This method exists primarily to produce the content for the
   * "locations of organizations hosting an IPT". The response holds the distinct organizations
   * running the installations of the specified type.
   */
  @Hidden
  @GetMapping("location/{type}")
  public String organizationsAsGeoJSON(@PathVariable InstallationType type) {
    List<Organization> orgs = organizationMapper.hostingInstallationsOf(type, true);

    // to increment the count on duplicates
    Map<Organization, AtomicInteger> counts = Maps.newHashMap();
    for (Organization o : orgs) {
      if (counts.containsKey(o)) {
        counts.get(o).incrementAndGet();
      } else {
        counts.put(o, new AtomicInteger(1));
      }
    }

    JSONObject featureCollection = new JSONObject();
    try {
      featureCollection.put("type", "FeatureCollection");

      List<JSONObject> features = Lists.newArrayList();
      for (Organization o : counts.keySet()) {
        JSONObject feature = new JSONObject();
        feature.put("type", "Feature");
        feature.put(
            "properties",
            ImmutableMap.<String, Object>of(
                "key", o.getKey(),
                "title", o.getTitle(),
                "count", counts.get(o).get()));
        JSONObject geom = new JSONObject();
        geom.put("type", "Point");
        geom.put("coordinates", ImmutableList.<BigDecimal>of(o.getLongitude(), o.getLatitude()));
        feature.put("geometry", geom);
        features.add(feature);
      }
      featureCollection.put("features", features);
    } catch (JSONException e) {
      LOG.error("Unable to build GeoJSON", e);
    }

    return featureCollection.toString();
  }

  @Hidden
  @PostMapping(value = "{installationKey}/metasync", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Transactional
  @Secured(ADMIN_ROLE)
  public void createMetasync(
      @PathVariable UUID installationKey,
      @RequestBody @Valid @NotNull @Trim MetasyncHistory metasyncHistory) {
    checkArgument(
        installationKey.equals(metasyncHistory.getInstallationKey()),
        "Metasync must have the same key as the installation");
    this.createMetasync(metasyncHistory);
  }

  @Hidden
  @PostMapping(value = "metasync", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Transactional
  @Secured(ADMIN_ROLE)
  @Override
  public void createMetasync(@RequestBody @Trim MetasyncHistory metasyncHistory) {
    metasyncHistoryMapper.create(metasyncHistory);
  }

  @Hidden
  @GetMapping("metasync")
  @Override
  public PagingResponse<MetasyncHistory> listMetasync(Pageable page) {
    return new PagingResponse<>(
        page, (long) metasyncHistoryMapper.count(), metasyncHistoryMapper.list(page));
  }

  @Hidden
  @GetMapping("{installationKey}/metasync")
  @Override
  public PagingResponse<MetasyncHistory> listMetasync(
      @PathVariable UUID installationKey, Pageable page) {
    return new PagingResponse<>(
        page,
        (long) metasyncHistoryMapper.countByInstallation(installationKey),
        metasyncHistoryMapper.listByInstallation(installationKey, page));
  }

  @Operation(
      operationId = "suggestInstallations",
      summary = "Suggest installations.",
      description =
          "Search that returns up to 20 matching installations. Results are ordered by relevance. "
              + "The response is smaller than an installation search.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0103")))
  @CommonParameters.QParameter
  @Pageable.OffsetLimitParameters
  @ApiResponse(responseCode = "200", description = "Node search successful")
  @ApiResponse(responseCode = "400", description = "Invalid search query provided")
  @GetMapping("suggest")
  @Override
  public List<KeyTitleResult> suggest(@RequestParam(value = "q", required = false) String q) {
    return installationMapper.suggest(q);
  }

  @Override
  public PagingResponse<Installation> listByType(InstallationType type, Pageable page) {
    InstallationListParams listParams =
        InstallationListParams.builder().type(type).page(page).build();
    long total = installationMapper.count(listParams);
    return pagingResponse(page, total, installationMapper.list(listParams));
  }
}
