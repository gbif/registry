package org.gbif.registry.ws.resources;

import com.google.common.base.Strings;
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
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.ContactMapper;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.EndpointMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.NodeMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.ws.Trim;
import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.ws.server.interceptor.NullToNotFound;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;

@RestController
@RequestMapping("node")
public class NodeResource extends BaseNetworkEntityResource<Node> implements NodeService {

  private final NodeMapper nodeMapper;
  private final OrganizationMapper organizationMapper;
  private final InstallationMapper installationMapper;
  private final DatasetMapper datasetMapper;
  private final Augmenter nodeAugmenter;

  public NodeResource(
      NodeMapper nodeMapper,
      IdentifierMapper identifierMapper,
      CommentMapper commentMapper,
      ContactMapper contactMapper,
      EndpointMapper endpointMapper,
      MachineTagMapper machineTagMapper,
      TagMapper tagMapper,
      OrganizationMapper organizationMapper,
      DatasetMapper datasetMapper,
      InstallationMapper installationMapper,
      EventManager eventManager,
      Augmenter nodeAugmenter,
      EditorAuthorizationService userAuthService,
      WithMyBatis withMyBatis) {
    super(nodeMapper, commentMapper, contactMapper, endpointMapper, identifierMapper, machineTagMapper, tagMapper,
        Node.class, eventManager, userAuthService, withMyBatis);
    this.nodeMapper = nodeMapper;
    this.organizationMapper = organizationMapper;
    this.nodeAugmenter = nodeAugmenter;
    this.datasetMapper = datasetMapper;
    this.installationMapper = installationMapper;
  }

  @GetMapping("{key}")
  @Nullable
  @NullToNotFound
  @Override
  public Node get(@PathVariable UUID key) {
    return nodeAugmenter.augment(super.get(key));
  }


  /**
   * All network entities support simple (!) search with "&q=".
   * This is to support the console user interface, and is in addition to any complex, faceted search that might
   * additionally be supported, such as dataset search.
   */
  @RequestMapping(method = RequestMethod.GET)
  public PagingResponse<Node> list(
      @Nullable @RequestParam(value = "identifierType", required = false) IdentifierType identifierType,
      @Nullable @RequestParam(value = "identifier", required = false) String identifier,
      @Nullable @RequestParam(value = "machineTagNamespace", required = false) String namespace,
      @Nullable @RequestParam(value = "machineTagName", required = false) String name,
      @Nullable @RequestParam(value = "machineTagValue", required = false) String value,
      @Nullable @RequestParam(value = "q", required = false) String query,
      @Nullable Pageable page
  ) {
    // This is getting messy: http://dev.gbif.org/issues/browse/REG-426
    if (identifierType != null && identifier != null) {
      return listByIdentifier(identifierType, identifier, page);
    } else if (identifier != null) {
      return listByIdentifier(identifier, page);
    } else if (namespace != null) {
      return listByMachineTag(namespace, name, value, page);
    } else if (Strings.isNullOrEmpty(query)) {
      return list(page);
    } else {
      return search(query, page);
    }
  }

  /**
   * Decorates the Nodes in the response with the Augmenter.
   */
  private PagingResponse<Node> decorateResponse(PagingResponse<Node> response) {
    for (Node n : response.getResults()) {
      nodeAugmenter.augment(n);
    }
    return response;
  }

  @Override
  public PagingResponse<Node> search(String query, @Nullable Pageable page) {
    return decorateResponse(super.search(query, page));
  }

  @Override
  public PagingResponse<Node> list(@Nullable Pageable page) {
    return decorateResponse(super.list(page));
  }

  @Override
  public PagingResponse<Node> listByIdentifier(IdentifierType type, String identifier, @Nullable Pageable page) {
    return decorateResponse(super.listByIdentifier(type, identifier, page));
  }

  @Override
  public PagingResponse<Node> listByIdentifier(String identifier, @Nullable Pageable page) {
    return decorateResponse(super.listByIdentifier(identifier, page));
  }

  @GetMapping("{key}/organization")
  @Override
  public PagingResponse<Organization> endorsedOrganizations(@PathVariable("key") UUID nodeKey, Pageable page) {
    return new PagingResponse<>(page, organizationMapper.countOrganizationsEndorsedBy(nodeKey),
        organizationMapper.organizationsEndorsedBy(nodeKey, page));
  }

  @GetMapping("pendingEndorsement")
  @Override
  public PagingResponse<Organization> pendingEndorsements(Pageable page) {
    return new PagingResponse<>(page, organizationMapper.countPendingEndorsements(null),
        organizationMapper.pendingEndorsements(null, page));
  }

  @GetMapping("{key}/pendingEndorsement")
  @Override
  public PagingResponse<Organization> pendingEndorsements(@PathVariable("key") UUID nodeKey, Pageable page) {
    return new PagingResponse<>(page, organizationMapper.countPendingEndorsements(nodeKey),
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
  public PagingResponse<Dataset> endorsedDatasets(@PathVariable("key") UUID nodeKey, Pageable page) {
    return pagingResponse(page, datasetMapper.countDatasetsEndorsedBy(nodeKey),
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
  public void deleteContact(@PathVariable("key") UUID targetEntityKey, @PathVariable int contactKey) {
    throw new UnsupportedOperationException("Contacts are manually managed in the Directory");
  }

  @Override
  public int addContact(@PathVariable("key") UUID targetEntityKey, @NotNull @Valid @Trim Contact contact) {
    throw new UnsupportedOperationException("Contacts are manually managed in the Directory");
  }

  @GetMapping("{key}/installation")
  @Override
  public PagingResponse<Installation> installations(@PathVariable("key") UUID nodeKey, Pageable page) {
    return pagingResponse(page, installationMapper.countInstallationsEndorsedBy(nodeKey),
        installationMapper.listInstallationsEndorsedBy(nodeKey, page));
  }

  @GetMapping("suggest")
  @Override
  public List<KeyTitleResult> suggest(@Nullable @RequestParam(value = "q", required = false) String label) {
    return nodeMapper.suggest(label);
  }
}
