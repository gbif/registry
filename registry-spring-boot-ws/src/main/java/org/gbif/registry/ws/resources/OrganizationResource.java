package org.gbif.registry.ws.resources;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.ConfirmationKeyParameter;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.ContactMapper;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.EndpointMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.registry.ws.security.SecurityContextCheck;
import org.gbif.registry.ws.surety.OrganizationEndorsementService;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.annotation.Trim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.APP_ROLE;
import static org.gbif.registry.ws.security.UserRoles.EDITOR_ROLE;

@RestController
@RequestMapping("organization")
public class OrganizationResource extends BaseNetworkEntityResource<Organization> implements OrganizationService {

  private static final Logger LOG = LoggerFactory.getLogger(OrganizationResource.class);

  protected static final int MINIMUM_PASSWORD_SIZE = 12;
  protected static final int MAXIMUM_PASSWORD_SIZE = 15;
  private static final String PASSWORD_ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

  private final DatasetMapper datasetMapper;
  private final OrganizationMapper organizationMapper;
  private final InstallationMapper installationMapper;
  private final OrganizationEndorsementService<UUID> organizationEndorsementService;
  private final EditorAuthorizationService userAuthService;

  public OrganizationResource(
      OrganizationMapper organizationMapper,
      ContactMapper contactMapper,
      EndpointMapper endpointMapper,
      MachineTagMapper machineTagMapper,
      TagMapper tagMapper,
      IdentifierMapper identifierMapper,
      CommentMapper commentMapper,
      DatasetMapper datasetMapper,
      InstallationMapper installationMapper,
      OrganizationEndorsementService<UUID> organizationEndorsementService,
      EventManager eventManager,
      EditorAuthorizationService userAuthService,
      WithMyBatis withMyBatis) {
    super(organizationMapper,
        commentMapper,
        contactMapper,
        endpointMapper,
        identifierMapper,
        machineTagMapper,
        tagMapper,
        Organization.class,
        eventManager,
        userAuthService,
        withMyBatis);
    this.datasetMapper = datasetMapper;
    this.organizationMapper = organizationMapper;
    this.installationMapper = installationMapper;
    this.organizationEndorsementService = organizationEndorsementService;
    this.userAuthService = userAuthService;
  }

  /**
   * This method overrides the create method for an organization, populating the password field with a randomly
   * generated string before passing on to the superclass create method.
   *
   * @param organization organization
   * @return key of entity created
   */
  @Override
  @Secured({ADMIN_ROLE, EDITOR_ROLE, APP_ROLE})
  @Trim
  @RequestMapping(method = RequestMethod.POST)
  public UUID createBase(@RequestBody @NotNull @Trim Organization organization) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    organization.setPassword(generatePassword());
    UUID newOrganization = super.create(organization);

    if (SecurityContextCheck.checkUserInRole(authentication, APP_ROLE)) {
      // for trusted app, we accept contacts to include on the endorsement request
      Optional.ofNullable(organization.getContacts())
          .filter(c -> !c.isEmpty())
          .ifPresent(contacts -> contacts.forEach(c -> addContact(newOrganization, c)));
      Optional.ofNullable(organization.getComments())
          .filter(c -> !c.isEmpty())
          .ifPresent(comments -> comments.forEach(c -> addComment(newOrganization, c)));
      organizationEndorsementService.onNewOrganization(organization);
    }
    return newOrganization;
  }

  /**
   * Randomly generates a shared token (password) for an organization.
   *
   * @return generated password
   */
  @VisibleForTesting
  protected static String generatePassword() {
    Random random = new Random();
    // randomly calculate the size of the password, between 0 and MAXIMUM_PASSWORD_SIZE
    int size = random.nextInt(MAXIMUM_PASSWORD_SIZE);
    // ensure the size is at least greater than or equal to MINIMUM_PASSWORD_SIZE
    size = Math.max(size, MINIMUM_PASSWORD_SIZE);

    // generate the password
    StringBuilder password = new StringBuilder();
    int randomIndex;
    while (size-- > 0) {
      randomIndex = random.nextInt(PASSWORD_ALLOWED_CHARACTERS.length());
      password.append(PASSWORD_ALLOWED_CHARACTERS.charAt(randomIndex));
    }
    return password.toString();
  }

  @PutMapping("{key}")
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void updateBase(@PathVariable UUID key, @RequestBody @NotNull @Trim Organization organization) {
    checkArgument(key.equals(organization.getKey()), "Provided entity must have the same key as the resource URL");
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String nameFromContext = authentication != null ? authentication.getName() : null;

    Organization previousOrg = super.get(organization.getKey());

    // If the endorsement is being changed, and the user is only an EDITOR, they must have node permission.
    // If the node is being changed, and the user is only an EDITOR, they must have node permission on both nodes.
    boolean endorsementApprovedChanged = previousOrg.isEndorsementApproved() != organization.isEndorsementApproved()
        || !previousOrg.getEndorsingNodeKey().equals(organization.getEndorsingNodeKey());
    if (endorsementApprovedChanged
        && !SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE)
        && !(userAuthService.allowedToModifyEntity(nameFromContext, organization.getEndorsingNodeKey())
        || userAuthService.allowedToModifyEntity(nameFromContext, previousOrg.getEndorsingNodeKey()))) {
      LOG.warn("Endorsement status or node changed, edit forbidden for {} on {}", nameFromContext, key);
      throw new WebApplicationException(HttpStatus.FORBIDDEN);
    }

    if (!previousOrg.isEndorsementApproved() && organization.isEndorsementApproved()) {
      // here we consider the user has the right to endorse an organization without a key.
      confirmEndorsement(organization.getKey(), (UUID) null);
    }

    // let the parent class set the modifiedBy
    super.updateBase(key, organization);
  }

  @Override
  public PagingResponse<Organization> search(String query, @Nullable Pageable page) {
    return list(null, null, null, null, null, null, null, query, page);
  }

  /**
   * All network entities support simple (!) search with "&q=".
   * This is to support the console user interface, and is in addition to any complex, faceted search that might
   * additionally be supported, such as dataset search.
   */
  @RequestMapping(method = RequestMethod.GET)
  public PagingResponse<Organization> list(
      @Nullable Country country,
      @Nullable @RequestParam(value = "identifierType", required = false) IdentifierType identifierType,
      @Nullable @RequestParam(value = "identifier", required = false) String identifier,
      @Nullable @RequestParam(value = "isEndorsed", required = false) Boolean isEndorsed,
      @Nullable @RequestParam(value = "machineTagNamespace", required = false) String namespace,
      @Nullable @RequestParam(value = "machineTagName", required = false) String name,
      @Nullable @RequestParam(value = "machineTagValue", required = false) String value,
      @Nullable @RequestParam(value = "q", required = false) String query,
      @Nullable Pageable page) {

    // Hack: Intercept identifier search
    if (identifierType != null && identifier != null) {
      return listByIdentifier(identifierType, identifier, page);
    } else if (identifier != null) {
      return listByIdentifier(identifier, page);
    }

    // Intercept machine tag search
    if (!Strings.isNullOrEmpty(namespace) || !Strings.isNullOrEmpty(name) || !Strings.isNullOrEmpty(value)) {
      return listByMachineTag(namespace, name, value, page);
    }

    // short circuited list all
    if (country == null && isEndorsed == null && Strings.isNullOrEmpty(query)) {
      return list(page);
    }

    // This uses to Organization Mapper overloaded option of search which will scope (AND) the query, country and endorsement.
    query = query != null ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(query)) : query;
    long total = organizationMapper.count(query, country, isEndorsed);
    page = page == null ? new PagingRequest() : page;
    return new PagingResponse<>(page.getOffset(), page.getLimit(), total,
        organizationMapper.search(query, country, isEndorsed, page));
  }

  @GetMapping("{key}/hostedDataset")
  @Override
  public PagingResponse<Dataset> hostedDatasets(@PathVariable("key") UUID organizationKey, Pageable page) {
    return pagingResponse(page, datasetMapper.countDatasetsHostedBy(organizationKey),
        datasetMapper.listDatasetsHostedBy(organizationKey, page));
  }

  @GetMapping("{key}/publishedDataset")
  @Override
  public PagingResponse<Dataset> publishedDatasets(@PathVariable("key") UUID organizationKey, Pageable page) {
    return pagingResponse(page, datasetMapper.countDatasetsPublishedBy(organizationKey),
        datasetMapper.listDatasetsPublishedBy(organizationKey, page));
  }

  /**
   * This is an HTTP only method to provide the count for the homepage of the portal. The homepage count excludes
   * non publishing an non endorsed datasets.
   */
  @GetMapping("count")
  public int countOrganizations() {
    return organizationMapper.countPublishing();
  }

  @GetMapping("{key}/installation")
  @Override
  public PagingResponse<Installation> installations(@PathVariable("key") UUID organizationKey, Pageable page) {
    return pagingResponse(page, installationMapper.countInstallationsByOrganization(organizationKey),
        installationMapper.listInstallationsByOrganization(organizationKey, page));
  }

  @Override
  public PagingResponse<Organization> listByCountry(Country country, @Nullable Pageable page) {
    return pagingResponse(page, organizationMapper.countOrganizationsByCountry(country),
        organizationMapper.organizationsByCountry(country, page));
  }

  @GetMapping("deleted")
  @Override
  public PagingResponse<Organization> listDeleted(Pageable page) {
    return pagingResponse(page, organizationMapper.countDeleted(), organizationMapper.deleted(page));
  }

  @GetMapping("pending")
  @Override
  public PagingResponse<Organization> listPendingEndorsement(Pageable page) {
    return pagingResponse(page, organizationMapper.countPendingEndorsements(null),
        organizationMapper.pendingEndorsements(null, page));
  }

  @GetMapping("nonPublishing")
  @Override
  public PagingResponse<Organization> listNonPublishing(Pageable page) {
    return pagingResponse(page, organizationMapper.countNonPublishing(), organizationMapper.nonPublishing(page));
  }

  @GetMapping("suggest")
  @Override
  public List<KeyTitleResult> suggest(@Nullable @RequestParam(value = "q", required = false) String label) {
    return organizationMapper.suggest(label);
  }

  /**
   * This is an HTTP only method to retrieve the shared token (password) for an organization.
   *
   * @param organizationKey organization key
   * @return password if set, warning message if not set, or null if organization doesn't exist
   */
  @GetMapping(value = "{key}/password", produces = MediaType.TEXT_PLAIN_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public String retrievePassword(@PathVariable("key") UUID organizationKey) {
    Organization o = get(organizationKey);
    if (o == null) {
      return null;
    }
    // Organization.password is never null according to database schema. API doesn't mirror this though.
    return o.getPassword();
  }

  /**
   * Confirm the endorsement of an organization.
   */
  @PostMapping("{key}/endorsement")
//  @Validate
  @Secured(APP_ROLE)
  public ResponseEntity<Void> confirmEndorsement(
      @PathVariable("key") UUID organizationKey,
      @RequestBody @Valid @NotNull ConfirmationKeyParameter confirmationKeyParameter) {
    return (confirmEndorsement(organizationKey, confirmationKeyParameter.getConfirmationKey()) ?
        ResponseEntity.noContent() : ResponseEntity.status(HttpStatus.BAD_REQUEST)).build();
  }

  @Override
  public boolean confirmEndorsement(UUID organizationKey, UUID confirmationKey) {
    return organizationEndorsementService.confirmEndorsement(organizationKey, confirmationKey);
  }

  @Override
  protected List<UUID> owningEntityKeys(@NotNull Organization entity) {
    return Lists.newArrayList(entity.getEndorsingNodeKey());
  }
}
