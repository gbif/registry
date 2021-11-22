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
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.registry.domain.ws.NetworkRequestSearchParams;
import org.gbif.registry.events.ChangedComponentEvent;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.NetworkMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.service.MapperServiceLocator;
import org.gbif.registry.service.WithMyBatis;
import org.gbif.ws.WebApplicationException;

import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;

import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.EDITOR_ROLE;
import static org.gbif.registry.security.UserRoles.IPT_ROLE;

@Validated
@Primary
@RestController
@RequestMapping(value = "network", produces = MediaType.APPLICATION_JSON_VALUE)
public class NetworkResource extends BaseNetworkEntityResource<Network> implements NetworkService {

  private final DatasetMapper datasetMapper;
  private final NetworkMapper networkMapper;
  private final OrganizationMapper organizationMapper;
  private final EventManager eventManager;

  public NetworkResource(
      MapperServiceLocator mapperServiceLocator,
      EventManager eventManager,
      WithMyBatis withMyBatis) {
    super(
        mapperServiceLocator.getNetworkMapper(),
        mapperServiceLocator,
        Network.class,
        eventManager,
        withMyBatis);
    this.eventManager = eventManager;
    this.datasetMapper = mapperServiceLocator.getDatasetMapper();
    this.networkMapper = mapperServiceLocator.getNetworkMapper();
    this.organizationMapper = mapperServiceLocator.getOrganizationMapper();
  }

  @GetMapping("{key}")
  @NullToNotFound("/network/{key}")
  @Override
  public Network get(@PathVariable UUID key) {
    return super.get(key);
  }

  /**
   * All network entities support simple (!) search with "&q=". This is to support the console user
   * interface, and is in addition to any complex, faceted search that might additionally be
   * supported, such as dataset search.
   */
  @GetMapping
  public PagingResponse<Network> list(@Valid NetworkRequestSearchParams request, Pageable page) {
    if (request.getIdentifierType() != null && request.getIdentifier() != null) {
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

  @GetMapping("{key}/constituents")
  @Override
  public PagingResponse<Dataset> listConstituents(
      @PathVariable("key") UUID networkKey, Pageable page) {
    return pagingResponse(
        page,
        (long) networkMapper.countDatasetsInNetwork(networkKey),
        datasetMapper.listDatasetsInNetwork(networkKey, page));
  }

  /**
   * Validates if the requested dataset exists.
   */
  private void existDatasetCheck(UUID datasetKey) {
    if (datasetMapper.get(datasetKey) == null) {
      throw new WebApplicationException(
          "Dataset " + datasetKey + " not found", HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Validates if the requested network exists.
   */
  private void existNetworkCheck(UUID networkKey) {
    if (networkMapper.get(networkKey) == null) {
      throw new WebApplicationException(
          "NetworkKey " + networkKey + " not found", HttpStatus.BAD_REQUEST);
    }
  }

  @PostMapping("{key}/constituents/{datasetKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE, IPT_ROLE})
  @Override
  public void addConstituent(@PathVariable("key") UUID networkKey, @PathVariable UUID datasetKey) {
    existDatasetCheck(datasetKey);
    existNetworkCheck(networkKey);
    networkMapper.addDatasetConstituent(networkKey, datasetKey);
    eventManager.post(ChangedComponentEvent.newInstance(datasetKey, Network.class, Dataset.class));
  }

  @DeleteMapping("{key}/constituents/{datasetKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE, IPT_ROLE})
  @Override
  public void removeConstituent(
      @PathVariable("key") UUID networkKey, @PathVariable UUID datasetKey) {
    existDatasetCheck(datasetKey);
    existNetworkCheck(networkKey);
    networkMapper.deleteDatasetConstituent(networkKey, datasetKey);
    eventManager.post(ChangedComponentEvent.newInstance(datasetKey, Network.class, Dataset.class));
  }

  @GetMapping("suggest")
  @Override
  public List<KeyTitleResult> suggest(@RequestParam(value = "q", required = false) String label) {
    return networkMapper.suggest(label);
  }

  @GetMapping("{key}/organization")
  @Override
  public PagingResponse<Organization> publishingOrganizations(
      @PathVariable("key") UUID networkKey, Pageable page) {
    return new PagingResponse<>(
        page,
        organizationMapper.countPublishingOrganizationsInNetwork(networkKey),
        organizationMapper.listPublishingOrganizationsInNetwork(networkKey, page));
  }
}
