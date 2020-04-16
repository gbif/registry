/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.directory.Augmenter;
import org.gbif.registry.domain.ws.NodeRequestSearchParams;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.NodeMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.service.MapperServiceLocator;
import org.gbif.registry.security.EditorAuthorizationService;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;

import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;

@Validated
@RestController
@RequestMapping(value = "node", produces = MediaType.APPLICATION_JSON_VALUE)
public class NodeResource extends BaseNetworkEntityResource<Node> implements NodeService {

  private final NodeMapper nodeMapper;
  private final OrganizationMapper organizationMapper;
  private final InstallationMapper installationMapper;
  private final DatasetMapper datasetMapper;
  private final Augmenter nodeAugmenter;

  public NodeResource(
      MapperServiceLocator mapperServiceLocator,
      EventManager eventManager,
      Augmenter nodeAugmenter,
      EditorAuthorizationService userAuthService,
      WithMyBatis withMyBatis) {
    super(
        mapperServiceLocator.getNodeMapper(),
        mapperServiceLocator,
        Node.class,
        eventManager,
        userAuthService,
        withMyBatis);
    this.nodeMapper = mapperServiceLocator.getNodeMapper();
    this.organizationMapper = mapperServiceLocator.getOrganizationMapper();
    this.nodeAugmenter = nodeAugmenter;
    this.datasetMapper = mapperServiceLocator.getDatasetMapper();
    this.installationMapper = mapperServiceLocator.getInstallationMapper();
  }

  @GetMapping("{key}")
  @NullToNotFound("/node/{key}")
  @Override
  public Node get(@PathVariable UUID key) {
    return nodeAugmenter.augment(super.get(key));
  }

  /**
   * All network entities support simple (!) search with "&q=". This is to support the console user
   * interface, and is in addition to any complex, faceted search that might additionally be
   * supported, such as dataset search.
   */
  @GetMapping
  public PagingResponse<Node> list(@Valid NodeRequestSearchParams request, Pageable page) {
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

  /** Decorates the Nodes in the response with the Augmenter. */
  private PagingResponse<Node> decorateResponse(PagingResponse<Node> response) {
    for (Node n : response.getResults()) {
      nodeAugmenter.augment(n);
    }
    return response;
  }

  @Override
  public PagingResponse<Node> search(String query, Pageable page) {
    return decorateResponse(super.search(query, page));
  }

  @Override
  public PagingResponse<Node> list(Pageable page) {
    return decorateResponse(super.list(page));
  }

  @Override
  public PagingResponse<Node> listByIdentifier(
      IdentifierType type, String identifier, Pageable page) {
    return decorateResponse(super.listByIdentifier(type, identifier, page));
  }

  @Override
  public PagingResponse<Node> listByIdentifier(String identifier, Pageable page) {
    return decorateResponse(super.listByIdentifier(identifier, page));
  }

  @GetMapping("{key}/organization")
  @Override
  public PagingResponse<Organization> endorsedOrganizations(
      @PathVariable("key") UUID nodeKey, Pageable page) {
    return new PagingResponse<>(
        page,
        organizationMapper.countOrganizationsEndorsedBy(nodeKey),
        organizationMapper.organizationsEndorsedBy(nodeKey, page));
  }

  @GetMapping("pendingEndorsement")
  @Override
  public PagingResponse<Organization> pendingEndorsements(Pageable page) {
    return new PagingResponse<>(
        page,
        organizationMapper.countPendingEndorsements(null),
        organizationMapper.pendingEndorsements(null, page));
  }

  @GetMapping("{key}/pendingEndorsement")
  @Override
  public PagingResponse<Organization> pendingEndorsements(
      @PathVariable("key") UUID nodeKey, Pageable page) {
    return new PagingResponse<>(
        page,
        organizationMapper.countPendingEndorsements(nodeKey),
        organizationMapper.pendingEndorsements(nodeKey, page));
  }

  @GetMapping("country/{key}")
  @Nullable
  public Node getByCountry(@PathVariable("key") String isoCode) {
    return getByCountry(Country.fromIsoCode(isoCode));
  }

  @Nullable
  @Override
  public Node getByCountry(Country country) {
    return nodeAugmenter.augment(nodeMapper.getByCountry(country));
  }

  @GetMapping("country")
  @Override
  public List<Country> listNodeCountries() {
    return nodeMapper.listNodeCountries();
  }

  @GetMapping("activeCountries")
  @Override
  public List<Country> listActiveCountries() {
    return nodeMapper.listActiveCountries();
  }

  @GetMapping("{key}/dataset")
  @Override
  public PagingResponse<Dataset> endorsedDatasets(
      @PathVariable("key") UUID nodeKey, Pageable page) {
    return pagingResponse(
        page,
        datasetMapper.countDatasetsEndorsedBy(nodeKey),
        datasetMapper.listDatasetsEndorsedBy(nodeKey, page));
  }

  @GetMapping("{key}/contact")
  @Override
  public List<Contact> listContacts(@PathVariable("key") UUID targetEntityKey) {
    throw new UnsupportedOperationException("Contacts are manually managed in the Directory");
  }

  @DeleteMapping("{key}/contact/{contactKey}")
  @Secured(ADMIN_ROLE)
  @Override
  public void deleteContact(
      @PathVariable("key") UUID targetEntityKey, @PathVariable int contactKey) {
    throw new UnsupportedOperationException("Contacts are manually managed in the Directory");
  }

  @PostMapping(value = "{key}/contact", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public int addContact(@PathVariable("key") UUID targetEntityKey, @RequestBody Contact contact) {
    throw new UnsupportedOperationException("Contacts are manually managed in the Directory");
  }

  @GetMapping("{key}/installation")
  @Override
  public PagingResponse<Installation> installations(
      @PathVariable("key") UUID nodeKey, Pageable page) {
    return pagingResponse(
        page,
        installationMapper.countInstallationsEndorsedBy(nodeKey),
        installationMapper.listInstallationsEndorsedBy(nodeKey, page));
  }

  @GetMapping("suggest")
  @Override
  public List<KeyTitleResult> suggest(@RequestParam(value = "q", required = false) String label) {
    return nodeMapper.suggest(label);
  }
}
