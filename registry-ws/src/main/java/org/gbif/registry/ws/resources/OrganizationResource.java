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
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.ConfirmationKeyParameter;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.EndorsementStatus;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.PostPersist;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.domain.ws.OrganizationRequestSearchParams;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.service.MapperServiceLocator;
import org.gbif.registry.security.EditorAuthorizationService;
import org.gbif.registry.security.SecurityContextCheck;
import org.gbif.registry.service.WithMyBatis;
import org.gbif.registry.ws.surety.OrganizationEndorsementService;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.APP_ROLE;
import static org.gbif.registry.security.UserRoles.EDITOR_ROLE;

@SuppressWarnings("UnstableApiUsage")
@io.swagger.v3.oas.annotations.tags.Tag(
  name = "Organizations",
  description = "A **publishing organization** is an institution endorsed by a GBIF Node to publish datasets to GBIF.\n\n" +
    "The organization API provides CRUD and discovery services for organizations. Its most prominent use on the GBIF " +
    "portal is to drive the [data publisher search](https://www.gbif.org/publisher/search).\n\n" +
    "Please note deletion of organizations is logical, meaning organization entries remain registered forever and only get a " +
    "deleted timestamp. On the other hand, deletion of an organizations's contacts, endpoints, identifiers, tags, " +
    "machine tags, comments, and metadata descriptions is physical, meaning the entries are permanently removed.",
  extensions = @io.swagger.v3.oas.annotations.extensions.Extension(
    name = "Order", properties = @ExtensionProperty(name = "Order", value = "0200")))
@Validated
@Primary
@RestController
@RequestMapping(value = "organization", produces = MediaType.APPLICATION_JSON_VALUE)
public class OrganizationResource extends BaseNetworkEntityResource<Organization>
    implements OrganizationService {

  public static final int MINIMUM_PASSWORD_SIZE = 12;
  public static final int MAXIMUM_PASSWORD_SIZE = 15;
  private static final String ALLOWED_CHARACTERS =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

  private final DatasetMapper datasetMapper;
  private final OrganizationMapper organizationMapper;
  private final InstallationMapper installationMapper;
  private final OrganizationEndorsementService<UUID> organizationEndorsementService;
  private final EditorAuthorizationService userAuthService;

  public OrganizationResource(
      MapperServiceLocator mapperServiceLocator,
      OrganizationEndorsementService<UUID> organizationEndorsementService,
      EventManager eventManager,
      EditorAuthorizationService userAuthService,
      WithMyBatis withMyBatis) {
    super(
        mapperServiceLocator.getOrganizationMapper(),
        mapperServiceLocator,
        Organization.class,
        eventManager,
        withMyBatis);
    this.datasetMapper = mapperServiceLocator.getDatasetMapper();
    this.organizationMapper = mapperServiceLocator.getOrganizationMapper();
    this.installationMapper = mapperServiceLocator.getInstallationMapper();
    this.organizationEndorsementService = organizationEndorsementService;
    this.userAuthService = userAuthService;
  }

  @Operation(
    operationId = "getOrganizatinon",
    summary = "Get details of a single publishing organization",
    description = "Details of a single publishing organization.  Also works for deleted publishing organizations.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0300")),
    tags = "BASIC")
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "Organization found and returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}")
  @NullToNotFound("/organization/{key}")  // TODO TODO TODO
  @Override
  public Organization get(@PathVariable UUID key) {
    return super.get(key);
  }

  /**
   * This method overrides the create method for an organization, populating the password field with
   * a randomly generated string before passing on to the superclass create method.
   *
   * @param organization organization
   * @return key of entity created
   */
  @Operation(
    operationId = "createOrganization",
    summary = "Create a new publishing organization",
    description = "Creates a new publishing organization.  Note contacts, endpoints, identifiers, tags, machine tags, comments and " +
      "metadata descriptions must be added in subsequent requests.")
  @ApiResponse(
    responseCode = "201",
    description = "Publishing organization created, new publishing organization's UUID returned")
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Secured({ADMIN_ROLE, EDITOR_ROLE, APP_ROLE})
  @Trim
  @Override
  public UUID create(@RequestBody @Trim Organization organization) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    organization.setPassword(generatePassword());
    UUID newOrganization = super.create(organization);

    if (organization.isEndorsementApproved()) {
      organizationMapper.changeEndorsementStatus(newOrganization, EndorsementStatus.ENDORSED);
    }

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
   * Updates the organization.
   *
   * @param organization organization
   */
  // Method overridden only for documentation.
  @Operation(
    operationId = "updateOrganization",
    summary = "Update an existing organization",
    description = "Updates the existing publishing organization.  Note contacts, endpoints, identifiers, tags, machine tags, comments and " +
      "metadata descriptions are not changed with this method.")
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "204",
    description = "Organization updated")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping(value = "{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PostPersist.class, Default.class})
  @Override
  public void update(@PathVariable("key") UUID key, @Valid @RequestBody @Trim Organization organization) {
    super.update(key, organization);
  }

  /**
   * Deletes the organization.
   *
   * @param key key of organization to delete
   */
  // Method overridden only for documentation.
  @Operation(
    operationId = "deleteOrganization",
    summary = "Delete a publishing organization",
    description = "Marks a publishing organization as deleted.  Note contacts, endpoints, identifiers, tags, machine tags, comments and " +
      "metadata descriptions are not changed.")
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "204",
    description = "Publishing organization deleted")
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}")
  @Override
  public void delete(@PathVariable UUID key) {
    super.delete(key);
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
      randomIndex = random.nextInt(ALLOWED_CHARACTERS.length());
      password.append(ALLOWED_CHARACTERS.charAt(randomIndex));
    }
    return password.toString();
  }

  @Override
  public PagingResponse<Organization> search(String query, Pageable page) {
    final OrganizationRequestSearchParams request = new OrganizationRequestSearchParams();
    request.setQ(query);
    return list(null, request, page);
  }

  /**
   * All network entities support simple (!) search with "&q=". This is to support the console user
   * interface, and is in addition to any complex, faceted search that might additionally be
   * supported, such as dataset search.
   */
  @Operation(
    operationId = "listOrganizations",
    summary = "List all publishing organizations",
    description = "Lists all current publishing organizations (deleted organizations are not listed).",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "0100")),
    tags = "BASIC")
  @SimpleSearchParameters
  @Parameters(
    value = {
      @Parameter(
        name = "isEndorsed",
        description = "Whether the organization is endorsed by a node.",
        schema = @Schema(implementation = Boolean.class),
        in = ParameterIn.QUERY)
      // TODO: Should networkKey be documented?
      // @Parameter(
      //   name = "networkKey",
      //   in = ParameterIn.QUERY)
    })
  @ApiResponse(
    responseCode = "200",
    description = "Organization search successful")
  @ApiResponse(
    responseCode = "400",
    description = "Invalid search query provided")
  @GetMapping
  public PagingResponse<Organization> list(
      @Nullable Country country, @Valid OrganizationRequestSearchParams request, Pageable page) {
    // Hack: Intercept identifier search
    if (request.getIdentifierType() != null && request.getIdentifier() != null) {
      return listByIdentifier(request.getIdentifierType(), request.getIdentifier(), page);
    } else if (request.getIdentifier() != null) {
      return listByIdentifier(request.getIdentifier(), page);
    }

    // Intercept machine tag search
    if (!Strings.isNullOrEmpty(request.getMachineTagNamespace())
        || !Strings.isNullOrEmpty(request.getMachineTagName())
        || !Strings.isNullOrEmpty(request.getMachineTagValue())) {
      return listByMachineTag(
          request.getMachineTagNamespace(),
          request.getMachineTagName(),
          request.getMachineTagValue(),
          page);
    }

    // short-circuited list all
    if (country == null
        && request.getIsEndorsed() == null
        && Strings.isNullOrEmpty(request.getQ())
        && request.getNetworkKey() == null) {
      return list(page);
    }

    // This uses to Organization Mapper overloaded option of search which will scope (AND) the
    // query, country and endorsement.
    String query =
        request.getQ() != null
            ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(request.getQ()))
            : request.getQ();
    long total =
        organizationMapper.count(query, country, request.getIsEndorsed(), request.getNetworkKey());
    page = page == null ? new PagingRequest() : page;
    return new PagingResponse<>(
        page.getOffset(),
        page.getLimit(),
        total,
        organizationMapper.search(
            query, country, request.getIsEndorsed(), request.getNetworkKey(), page));
  }

  @Operation(
    operationId = "getHostedDatasets",
    summary = "List hosted datasets",
    description = "Lists the hosted datasets (datasets hosted by installations hosted by the organization).")
  @Docs.DefaultEntityKeyParameter
  @Docs.DefaultOffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "List of hosted datasets")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/hostedDataset")
  @Override
  public PagingResponse<Dataset> hostedDatasets(
      @PathVariable("key") UUID organizationKey, Pageable page) {
    return pagingResponse(
        page,
        datasetMapper.countDatasetsHostedBy(organizationKey),
        datasetMapper.listDatasetsHostedBy(organizationKey, page));
  }

  @Operation(
    operationId = "getPublishedDatasets",
    summary = "List published datasets",
    description = "Lists the published datasets (datasets published by the organization).")
  @Docs.DefaultEntityKeyParameter
  @Docs.DefaultOffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "List of published datasets")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/publishedDataset")
  @Override
  public PagingResponse<Dataset> publishedDatasets(
      @PathVariable("key") UUID organizationKey, Pageable page) {
    return pagingResponse(
        page,
        datasetMapper.countDatasetsPublishedBy(organizationKey),
        datasetMapper.listDatasetsPublishedBy(organizationKey, page));
  }

  /**
   * This is an HTTP only method to provide the count for the homepage of the portal. The homepage
   * count excludes non-publishing and non-endorsed datasets.
   */
  @Hidden
  @GetMapping("count")
  public int countOrganizations() {
    return organizationMapper.countPublishing();
  }

  @Operation(
    operationId = "getOrganizationInstallations",
    summary = "List organization's installations",
    description = "Lists the technical installations registered to this organization.")
  @Docs.DefaultEntityKeyParameter
  @Docs.DefaultOffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "List of technical installations")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/installation")
  @Override
  public PagingResponse<Installation> installations(
      @PathVariable("key") UUID organizationKey, Pageable page) {
    return pagingResponse(
        page,
        installationMapper.countInstallationsByOrganization(organizationKey),
        installationMapper.listInstallationsByOrganization(organizationKey, page));
  }

  @Override
  public PagingResponse<Organization> listByCountry(Country country, Pageable page) {
    return pagingResponse(
        page,
        organizationMapper.countOrganizationsByCountry(country),
        organizationMapper.organizationsByCountry(country, page));
  }

  @Operation(
    operationId = "getDeletedOrganizations",
    summary = "List deleted organizations",
    description = "Lists deleted organizations.")
  @Docs.DefaultOffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "List of deleted organizations")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("deleted")
  @Override
  public PagingResponse<Organization> listDeleted(Pageable page) {
    return pagingResponse(
        page, organizationMapper.countDeleted(), organizationMapper.deleted(page));
  }

  @Operation(
    operationId = "getPendingOrganizations",
    summary = "List pending organizations",
    description = "Lists organizations whose endorsement is pending.")
  @Docs.DefaultOffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "List of pending organizations")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("pending")
  @Override
  public PagingResponse<Organization> listPendingEndorsement(Pageable page) {
    return pagingResponse(
        page,
        organizationMapper.countPendingEndorsements(null),
        organizationMapper.pendingEndorsements(null, page));
  }

  @Operation(
    operationId = "getNonPublishingOrganizations",
    summary = "List non-publishing organizations",
    description = "Lists organizations publishing 0 datasets (excluding deleted datasets).")
  @Docs.DefaultOffsetLimitParameters
  @ApiResponse(
    responseCode = "200",
    description = "List of non-publishing organizations")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("nonPublishing")
  @Override
  public PagingResponse<Organization> listNonPublishing(Pageable page) {
    return pagingResponse(
        page, organizationMapper.countNonPublishing(), organizationMapper.nonPublishing(page));
  }

  @Operation(
    operationId = "suggestOrganizations",
    summary = "Suggest organizations.",
    description = "Search that returns up to 20 matching publishing organizations. Results are ordered by relevance. " +
      "The response is smaller than an organization search.",
    extensions = @Extension(name = "Order", properties = @ExtensionProperty(name = "Order", value = "1300")),
    tags = "BASIC"
  )
  @Docs.DefaultQParameter
  @ApiResponse(
    responseCode = "200",
    description = "Organization search successful")
  @ApiResponse(
    responseCode = "400",
    description = "Invalid search query provided")
  @GetMapping("suggest")
  @Override
  public List<KeyTitleResult> suggest(@RequestParam(value = "q", required = false) String label) {
    return organizationMapper.suggest(label);
  }

  /**
   * This is an HTTP only method to retrieve the shared token (password) for an organization.
   *
   * @param organizationKey organization key
   * @return password if set, warning message if not set, or null if organization doesn't exist
   */
  @Hidden
  @GetMapping(value = "{key}/password", produces = MediaType.TEXT_PLAIN_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public String retrievePassword(@PathVariable("key") UUID organizationKey) {
    Organization o = get(organizationKey);
    if (o == null) {
      return null;
    }
    // Organization.password is never null according to database schema. API doesn't mirror this
    // though.
    return o.getPassword();
  }

  @Hidden
  @PostMapping("{key}/endorsement/status/{status}")
  @Secured(ADMIN_ROLE)
  public void changeEndorsementStatus(
      @PathVariable("key") UUID organizationKey, @PathVariable("status") EndorsementStatus status) {
    // TODO: 19/08/2020 send an email?
    organizationEndorsementService.changeEndorsementStatus(organizationKey, status);
  }

  /** Confirm the endorsement of an organization. This endpoint is used by email endorsement. */
  @Hidden
  @PostMapping(path = "{key}/endorsement", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured(APP_ROLE)
  public ResponseEntity<Void> confirmEndorsement(
      @PathVariable("key") UUID organizationKey,
      @RequestBody @Valid @NotNull ConfirmationKeyParameter confirmationKeyParameter) {
    return (confirmEndorsement(organizationKey, confirmationKeyParameter.getConfirmationKey())
            ? ResponseEntity.noContent()
            : ResponseEntity.status(HttpStatus.BAD_REQUEST))
        .build();
  }

  @Hidden
  @PostMapping("{key}/endorsement/{confirmationKey}")
  @Secured(APP_ROLE)
  @Override
  public boolean confirmEndorsement(
      @PathVariable("key") UUID organizationKey,
      @PathVariable("confirmationKey") UUID confirmationKey) {
    return organizationEndorsementService.confirmEndorsement(organizationKey, confirmationKey);
  }

  /**
   * Confirm the endorsement of an organization. This endpoint is used by the registry console
   * endorsement.
   */
  @Hidden
  @PutMapping("{key}/endorsement")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public ResponseEntity<Void> confirmEndorsementEndpoint(
      @PathVariable("key") UUID organizationKey) {
    boolean isEndorsed = confirmEndorsement(organizationKey);
    return (isEndorsed ? ResponseEntity.noContent() : ResponseEntity.status(HttpStatus.BAD_REQUEST))
        .build();
  }

  @Override
  public boolean confirmEndorsement(UUID organizationKey) {
    Organization organization = super.get(organizationKey);
    checkNotNull(organization, "Organization not found");
    return organizationEndorsementService.confirmEndorsement(organizationKey);
  }

  /**
   * Revoke the endorsement from an organization. This endpoint is used by the registry console
   * endorsement.
   */
  @Hidden
  @DeleteMapping("{key}/endorsement")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public ResponseEntity<Void> revokeEndorsementEndpoint(@PathVariable("key") UUID organizationKey) {
    return (revokeEndorsement(organizationKey)
            ? ResponseEntity.noContent()
            : ResponseEntity.status(HttpStatus.BAD_REQUEST))
        .build();
  }

  @Override
  public boolean revokeEndorsement(UUID organizationKey) {
    Organization organization = super.get(organizationKey);
    checkNotNull(organization, "Organization not found");
    return organizationEndorsementService.revokeEndorsement(organizationKey);
  }

  @Hidden
  @GetMapping("{key}/endorsement/user/{user}")
  public ResponseEntity<Void> userAllowedToEndorseOrganization(
      @PathVariable("key") UUID organizationKey, @PathVariable("user") String username) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Organization organization = super.get(organizationKey);
    checkNotNull(organization, "Organization not found");

    if (!SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE)
        && !userAuthService.allowedToModifyEntity(username, organization.getEndorsingNodeKey())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
