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
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.ContactMapper;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.EndpointMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.ws.authorization.OrganizationAuthorization;
import org.gbif.registry.ws.guice.Trim;
import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.registry.ws.security.SecurityContextCheck;
import org.gbif.registry.ws.surety.OrganizationEndorsementService;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.bval.guice.Validate;
import org.mybatis.guice.transactional.Transactional;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.APP_ROLE;
import static org.gbif.registry.ws.security.UserRoles.EDITOR_ROLE;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A MyBATIS implementation of the service.
 *
 * Note: {@link OrganizationEndorsementService} is a composed object. Therefore it is not part of the API ({@link OrganizationService})
 * at the moment.
 *
 */
@Path("organization")
@Singleton
public class OrganizationResource extends BaseNetworkEntityResource<Organization> implements OrganizationService {

  protected static final int MINIMUM_PASSWORD_SIZE = 12;
  protected static final int MAXIMUM_PASSWORD_SIZE = 15;
  private static final String PASSWORD_ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
  private final DatasetMapper datasetMapper;
  private final OrganizationMapper organizationMapper;
  private final InstallationMapper installationMapper;

  private final OrganizationEndorsementService<UUID> organizationEndorsementService;

  @Inject
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
    EventBus eventBus,
    EditorAuthorizationService userAuthService) {
    super(organizationMapper,
      commentMapper,
      contactMapper,
      endpointMapper,
      identifierMapper,
      machineTagMapper,
      tagMapper,
      Organization.class,
      eventBus,
      userAuthService);

    this.datasetMapper = datasetMapper;
    this.organizationMapper = organizationMapper;
    this.installationMapper = installationMapper;
    this.organizationEndorsementService = organizationEndorsementService;
  }

  /**
   * This method overrides the create method for an organization, populating the password field with a randomly
   * generated string before passing on to the superclass create method.
   *
   * @param organization organization
   * @param security     SecurityContext (security related information)
   *
   * @return key of entity created
   */
  @POST
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE, APP_ROLE})
  @Trim
  @Override
  public UUID create(@NotNull @Trim Organization organization, @Context SecurityContext security) {
    organization.setPassword(generatePassword());
    UUID newOrganization = super.create(organization, security);

    if (security.isUserInRole(APP_ROLE)) {
      // for trusted app, we accept contacts to include on the endorsement request
      Optional.ofNullable(organization.getContacts())
              .filter( c -> !c.isEmpty())
              .ifPresent( contacts -> contacts.forEach(c -> addContact(newOrganization, c, security)));
      Optional.ofNullable(organization.getComments())
              .filter( c -> !c.isEmpty())
              .ifPresent( comments -> comments.forEach(c -> addComment(newOrganization, c, security)));
      organizationEndorsementService.onNewOrganization(organization);
    }
    return newOrganization;
  }

  /**
   * Confirm the endorsement of an organization.
   *
   * @param organizationKey
   * @param confirmationKeyParameter
   *
   * @return
   */
  @POST
  @Path("{key}/endorsement")
  @Validate
  @RolesAllowed(APP_ROLE)
  public Response confirmEndorsement(@PathParam("key") UUID organizationKey,
                                     @Valid @NotNull ConfirmationKeyParameter confirmationKeyParameter,
                                     @Context SecurityContext security) {
    return (confirmEndorsement(organizationKey, confirmationKeyParameter.getConfirmationKey()) ?
            Response.noContent() : Response.status(Response.Status.BAD_REQUEST)).build();
  }

  @Override
  public boolean confirmEndorsement(UUID organizationKey, UUID confirmationKey) {
    return organizationEndorsementService.confirmEndorsement(organizationKey, confirmationKey);
  }

  @PUT
  @Path("{key}")
  @Trim
  @Transactional
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public void update(@PathParam("key") UUID key, @NotNull @Trim Organization organization,
                     @Context SecurityContext securityContext) {
    checkArgument(key.equals(organization.getKey()), "Provided entity must have the same key as the resource URL");

    Organization previousOrg = super.get(organization.getKey());
    SecurityContextCheck.ensurePrecondition(
            OrganizationAuthorization.isUpdateAuthorized(previousOrg, organization, securityContext),
            Response.Status.FORBIDDEN);

    if (!previousOrg.isEndorsementApproved() && organization.isEndorsementApproved()) {
      // here we consider the user has the right to endorse an organization without a key.
      confirmEndorsement(organization.getKey(), null);
    }

    // let the parent class set the modifiedBy
    super.update(key, organization, securityContext);
  }

  public PagingResponse<Organization> search(String query, @Nullable Pageable page) {
    return list(null, null, null, null, null, null, null, query, page);
  }

  /**
   * All network entities support simple (!) search with "&q=".
   * This is to support the console user interface, and is in addition to any complex, faceted search that might
   * additionally be supported, such as dataset search.
   */
  @GET
  public PagingResponse<Organization> list(
    @Nullable @Context Country country,
    @Nullable @QueryParam("identifierType") IdentifierType identifierType,
    @Nullable @QueryParam("identifier") String identifier,
    @Nullable @QueryParam("isEndorsed") Boolean isEndorsed,
    @Nullable @QueryParam("machineTagNamespace") String namespace,
    @Nullable @QueryParam("machineTagName") String name,
    @Nullable @QueryParam("machineTagValue") String value,
    @Nullable @QueryParam("q") String query,
    @Nullable @Context Pageable page
  ) {

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
    long total = organizationMapper.count(query, country, isEndorsed);
    page = page == null ? new PagingRequest() : page;
    return new PagingResponse<>(page.getOffset(), page.getLimit(), total,
                                            organizationMapper.search(query, country, isEndorsed, page));
  }

  @GET
  @Path("{key}/hostedDataset")
  @Override
  public PagingResponse<Dataset> hostedDatasets(@PathParam("key") UUID organizationKey, @Context Pageable page) {
    return pagingResponse(page, datasetMapper.countDatasetsHostedBy(organizationKey),
      datasetMapper.listDatasetsHostedBy(organizationKey, page));
  }

  @GET
  @Path("{key}/publishedDataset")
  @Override
  public PagingResponse<Dataset> publishedDatasets(@PathParam("key") UUID organizationKey, @Context Pageable page) {
    return pagingResponse(page, datasetMapper.countDatasetsPublishedBy(organizationKey),
      datasetMapper.listDatasetsPublishedBy(organizationKey, page));
  }

  /**
   * This is an HTTP only method to provide the count for the homepage of the portal. The homepage count excludes
   * non publishing an non endorsed datasets.
   */
  @GET
  @Path("count")
  public int countOrganizations() {
    return organizationMapper.countPublishing();
  }

  @Override
  public PagingResponse<Organization> listByCountry(Country country, @Nullable Pageable page) {
    return pagingResponse(page, organizationMapper.countOrganizationsByCountry(country),
      organizationMapper.organizationsByCountry(country, page));
  }

  @GET
  @Path("{key}/installation")
  @Override
  public PagingResponse<Installation> installations(@PathParam("key") UUID organizationKey, @Context Pageable page) {
    return pagingResponse(page, installationMapper.countInstallationsByOrganization(organizationKey),
      installationMapper.listInstallationsByOrganization(organizationKey, page));
  }

  @GET
  @Path("deleted")
  @Override
  public PagingResponse<Organization> listDeleted(@Context Pageable page) {
    return pagingResponse(page, organizationMapper.countDeleted(), organizationMapper.deleted(page));
  }

  @GET
  @Path("pending")
  @Override
  public PagingResponse<Organization> listPendingEndorsement(@Context Pageable page) {
    return pagingResponse(page, organizationMapper.countPendingEndorsements(null),
      organizationMapper.pendingEndorsements(null, page));
  }

  @GET
  @Path("nonPublishing")
  @Override
  public PagingResponse<Organization> listNonPublishing(@Context Pageable page) {
    return pagingResponse(page, organizationMapper.countNonPublishing(), organizationMapper.nonPublishing(page));
  }


  @Path("suggest")
  @GET
  @Override
  public List<KeyTitleResult> suggest(@QueryParam("q") String label) {
    return organizationMapper.suggest(label);
  }

  /**
   * This is an HTTP only method to retrieve the password for an organization.
   *
   * @param organizationKey organization key
   *
   * @return password if set, warning message if not set, or null if organization doesn't exist
   */
  @Path("{key}/password")
  @GET
  @RolesAllowed(ADMIN_ROLE)
  @Produces(MediaType.TEXT_PLAIN)
  public String retrievePassword(@PathParam("key") UUID organizationKey) {
    Organization o = get(organizationKey);
    if (o == null) {
      return null;
    }
    // Organization.password is never null according to database schema. API doesn't mirror this though.
    return o.getPassword();
  }

  /**
   * Randomly generates a password for an organization.
   *
   * @return generated password
   */
  @VisibleForTesting
  protected static String generatePassword() {
    Random random = new Random();
    // randomly calculate the size of the password, between 0 and MAXIMUM_PASSWORD_SIZE
    int size = random.nextInt(MAXIMUM_PASSWORD_SIZE);
    // ensure the size is at least greater than or equal to MINIMUM_PASSWORD_SIZE
    size = (size < MINIMUM_PASSWORD_SIZE) ? MINIMUM_PASSWORD_SIZE : size;

    // generate the password
    StringBuilder password = new StringBuilder();
    int randomIndex;
    while (size-- > 0) {
      random = new Random();
      randomIndex = random.nextInt(PASSWORD_ALLOWED_CHARACTERS.length());
      password.append(PASSWORD_ALLOWED_CHARACTERS.charAt(randomIndex));
    }
    return password.toString();
  }

  @Override
  protected List<UUID> owningEntityKeys(@NotNull Organization entity) {
    return Lists.newArrayList(entity.getEndorsingNodeKey());
  }
}
