/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.metasync.MetasyncHistory;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.MetasyncHistoryService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.StartMetasyncMessage;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.ContactMapper;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.EndpointMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.MetasyncHistoryMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.ws.guice.Trim;
import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.registry.gdpr.GdprService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A MyBATIS implementation of the service.
 */
@Path("installation")
@Singleton
public class InstallationResource extends BaseNetworkEntityResource<Installation> implements InstallationService,
  MetasyncHistoryService {

  private static final Logger LOG = LoggerFactory.getLogger(InstallationResource.class);
  private final DatasetMapper datasetMapper;
  private final InstallationMapper installationMapper;
  private final OrganizationMapper organizationMapper;
  private final MetasyncHistoryMapper metasyncHistoryMapper;


  /**
   * The messagePublisher can be optional, and optional is not supported in constructor injection.
   */
  @Inject(optional = true)
  private final MessagePublisher messagePublisher = null;

  @Inject
  public InstallationResource(
    InstallationMapper installationMapper,
    ContactMapper contactMapper,
    EndpointMapper endpointMapper,
    IdentifierMapper identifierMapper,
    MachineTagMapper machineTagMapper,
    TagMapper tagMapper,
    CommentMapper commentMapper,
    DatasetMapper datasetMapper,
    OrganizationMapper organizationMapper,
    MetasyncHistoryMapper metasyncHistoryMapper,
    EventBus eventBus,
    EditorAuthorizationService userAuthService,
    GdprService gdprService) {
    super(installationMapper,
      commentMapper,
      contactMapper,
      endpointMapper,
      identifierMapper,
      machineTagMapper,
      tagMapper,
      Installation.class,
      eventBus,
      userAuthService,
      gdprService);
    this.datasetMapper = datasetMapper;
    this.installationMapper = installationMapper;
    this.organizationMapper = organizationMapper;
    this.metasyncHistoryMapper = metasyncHistoryMapper;
  }


  /**
   * All network entities support simple (!) search with "&q=".
   * This is to support the console user interface, and is in addition to any complex, faceted search that might
   * additionally be supported, such as dataset search.
   */
  @GET
  public PagingResponse<Installation> list(@Nullable @QueryParam("q") String query,
    @Nullable @QueryParam("identifierType") IdentifierType identifierType,
    @Nullable @QueryParam("identifier") String identifier,
    @Nullable @Context Pageable page) {
    // This is getting messy: http://dev.gbif.org/issues/browse/REG-426
    if (identifierType != null && identifier != null) {
      return listByIdentifier(identifierType, identifier, page);
    } else if (identifier != null) {
      return listByIdentifier(identifier, page);
    } else if (Strings.isNullOrEmpty(query)) {
      return list(page);
    } else {
      return search(query, page);
    }
  }

  @GET
  @Path("{key}/dataset")
  @Override
  public PagingResponse<Dataset> getHostedDatasets(@PathParam("key") UUID installationKey, @Context Pageable page) {
    return new PagingResponse<Dataset>(page, datasetMapper.countDatasetsByInstallation(installationKey),
      datasetMapper.listDatasetsByInstallation(installationKey, page));
  }

  @GET
  @Path("deleted")
  @Override
  public PagingResponse<Installation> listDeleted(@Context Pageable page) {
    return pagingResponse(page, installationMapper.countDeleted(), installationMapper.deleted(page));
  }

  @GET
  @Path("nonPublishing")
  @Override
  public PagingResponse<Installation> listNonPublishing(@Context Pageable page) {
    return pagingResponse(page, installationMapper.countNonPublishing(), installationMapper.nonPublishing(page));
  }

  /**
   * This is a REST only (e.g. not part of the Java API) method that allows the registry console to trigger the
   * synchronization of the installation. This simply emits a message to rabbitmq requesting the sync, and applies
   * necessary security.
   */
  @POST
  @Path("{key}/synchronize")
  @RolesAllowed(ADMIN_ROLE)
  public void synchronize(@PathParam("key") UUID installationKey) {
    if (messagePublisher != null) {
      LOG.info("Requesting synchronizing installation[{}]", installationKey);
      try {
        messagePublisher.send(new StartMetasyncMessage(installationKey));
      } catch (IOException e) {
        LOG.error("Unable to send message requesting synchronization", e);
      }

    } else {
      LOG.warn("Registry is configured to run without messaging capabilities.  Unable to synchronize installation[{}]",
        installationKey);
    }
  }

  /**
   * This is a REST only (e.g. not part of the Java API) method that allows you to get the locations of installations as
   * GeoJSON. This method exists primarily to produce the content for the "locations of organizations hosting an IPT".
   * The response holds the distinct organizations running the installations of the specified type.
   */
  @GET
  @Path("location/{type}")
  public String organizationsAsGeoJSON(@PathParam("type") InstallationType type) {
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
        feature.put("properties", ImmutableMap.<String, Object>of(
          "key", o.getKey(),
          "title", o.getTitle(),
          "count", counts.get(o).get()));
        JSONObject geom = new JSONObject();
        geom.put("type", "Point");
        geom.put("coordinates", ImmutableList.<BigDecimal>of(
          o.getLongitude(), o.getLatitude()));
        feature.put("geometry", geom);
        features.add(feature);
      }
      featureCollection.put("features", features);
    } catch (JSONException e) {
      LOG.error("Unable to build GeoJSON", e);
    }

    return featureCollection.toString();
  }

  @POST
  @Path("{installationKey}/metasync")
  @Trim
  @Transactional
  @RolesAllowed(ADMIN_ROLE)
  public void createMetasync(@PathParam("installationKey") UUID installationKey,
    @Valid @NotNull @Trim MetasyncHistory metasyncHistory) {
    checkArgument(installationKey.equals(metasyncHistory.getInstallationKey()),
      "Metasync must have the same key as the installation");
    this.createMetasync(metasyncHistory);
  }

  @Trim
  @Transactional
  @RolesAllowed(ADMIN_ROLE)
  @Override
  public void createMetasync(@Valid @NotNull @Trim MetasyncHistory metasyncHistory) {
    metasyncHistoryMapper.create(metasyncHistory);
  }

  @Path("metasync")
  @GET
  @Override
  public PagingResponse<MetasyncHistory> listMetasync(@Context Pageable page) {
    return new PagingResponse<MetasyncHistory>(page, (long) metasyncHistoryMapper.count(),
      metasyncHistoryMapper.list(page));
  }


  @GET
  @Path("{installationKey}/metasync")
  @Override
  public PagingResponse<MetasyncHistory> listMetasync(@PathParam("installationKey") UUID installationKey,
    @Context Pageable page) {
    return new PagingResponse<MetasyncHistory>(page, (long) metasyncHistoryMapper.countByInstallation(installationKey),
      metasyncHistoryMapper.listByInstallation(installationKey, page));
  }

  @Override
  protected UUID owningEntityKey(@NotNull Installation entity) {
    return entity.getOrganizationKey();
  }
}
