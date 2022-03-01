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

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.annotation.Trim;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.metasync.MetasyncHistory;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.MetasyncHistoryService;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.common.messaging.api.messages.StartMetasyncMessage;
import org.gbif.registry.domain.ws.InstallationRequestSearchParams;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.MetaSyncHistoryMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.EDITOR_ROLE;
import static org.gbif.registry.security.UserRoles.IPT_ROLE;

@Validated
@Primary
@RestController
@RequestMapping(value = "installation", produces = MediaType.APPLICATION_JSON_VALUE)
public class InstallationResource extends BaseNetworkEntityResource<Installation>
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
    @Autowired(required = false) MessagePublisher messagePublisher,
    RestrictionsHandler restrictionsHandler) {
    super(
        mapperServiceLocator.getInstallationMapper(),
        mapperServiceLocator,
        Installation.class,
        eventManager,
        withMyBatis,
        restrictionsHandler);
    this.datasetMapper = mapperServiceLocator.getDatasetMapper();
    this.installationMapper = mapperServiceLocator.getInstallationMapper();
    this.organizationMapper = mapperServiceLocator.getOrganizationMapper();
    this.metasyncHistoryMapper = mapperServiceLocator.getMetaSyncHistoryMapper();
    this.messagePublisher = messagePublisher;
  }

  @GetMapping("{key}")
  @NullToNotFound("/installation/{key}")
  @Override
  public Installation get(@PathVariable UUID key) {
    return super.get(key);
  }

  /**
   * All network entities support simple (!) search with "&q=". This is to support the console user
   * interface, and is in addition to any complex, faceted search that might additionally be
   * supported, such as dataset search.
   */
  @GetMapping
  public PagingResponse<Installation> list(
      @Valid InstallationRequestSearchParams request, Pageable page) {
    if (request.getType() != null) {
      return listByType(request.getType(), page);
    } else if (request.getIdentifierType() != null && request.getIdentifier() != null) {
      return listByIdentifier(request.getIdentifierType(), request.getIdentifier(), page);
    } else if (request.getIdentifier() != null) {
      return listByIdentifier(request.getIdentifier(), page);
    } else if (request.getMachineTagNamespace() != null) {
      return listByMachineTag(
          request.getMachineTagNamespace(),
          request.getMachineTagName(),
          request.getMachineTagValue(),
          page);
    } else if (Strings.isNullOrEmpty(request.getQ())) {
      return list(page);
    } else {
      return search(request.getQ(), page);
    }
  }

  @GetMapping("{key}/dataset")
  @Override
  public PagingResponse<Dataset> getHostedDatasets(
      @PathVariable("key") UUID installationKey, Pageable page) {
    return new PagingResponse<>(
        page,
        datasetMapper.countDatasetsByInstallation(installationKey),
        datasetMapper.listDatasetsByInstallation(installationKey, page));
  }

  @GetMapping("deleted")
  @Override
  public PagingResponse<Installation> listDeleted(Pageable page) {
    return pagingResponse(
        page, installationMapper.countDeleted(), installationMapper.deleted(page));
  }

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

  @PostMapping(value = "metasync", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Transactional
  @Secured(ADMIN_ROLE)
  @Override
  public void createMetasync(@RequestBody @Trim MetasyncHistory metasyncHistory) {
    metasyncHistoryMapper.create(metasyncHistory);
  }

  @GetMapping("metasync")
  @Override
  public PagingResponse<MetasyncHistory> listMetasync(Pageable page) {
    return new PagingResponse<>(
        page, (long) metasyncHistoryMapper.count(), metasyncHistoryMapper.list(page));
  }

  @GetMapping("{installationKey}/metasync")
  @Override
  public PagingResponse<MetasyncHistory> listMetasync(
      @PathVariable UUID installationKey, Pageable page) {
    return new PagingResponse<>(
        page,
        (long) metasyncHistoryMapper.countByInstallation(installationKey),
        metasyncHistoryMapper.listByInstallation(installationKey, page));
  }

  @GetMapping("suggest")
  @Override
  public List<KeyTitleResult> suggest(@RequestParam(value = "q", required = false) String q) {
    return installationMapper.suggest(q);
  }

  @Override
  public PagingResponse<Installation> listByType(InstallationType type, Pageable page) {
    long total = installationMapper.countWithFilter(type);
    return pagingResponse(page, total, installationMapper.listWithFilter(type, page));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE, IPT_ROLE})
  @Override
  public UUID create(Installation entity) {
    getRestrictionsHandler().checkDenyPublisher(entity.getOrganizationKey());
    return super.create(entity);
  }

  @Override
  public void update(Installation entity) {
    getRestrictionsHandler().checkDenyPublisher(entity.getOrganizationKey());
    super.update(entity);
  }
}
