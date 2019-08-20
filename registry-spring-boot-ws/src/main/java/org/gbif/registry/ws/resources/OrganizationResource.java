package org.gbif.registry.ws.resources;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.ws.Trim;
import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.registry.ws.security.SecurityContextCheck;
import org.gbif.registry.ws.surety.OrganizationEndorsementService;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.APP_ROLE;
import static org.gbif.registry.ws.security.UserRoles.EDITOR_ROLE;

@RestController
@RequestMapping("organization")
public class OrganizationResource extends BaseNetworkEntityResource<Organization> implements OrganizationService {

  protected static final int MINIMUM_PASSWORD_SIZE = 12;
  protected static final int MAXIMUM_PASSWORD_SIZE = 15;
  private static final String PASSWORD_ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

  private final OrganizationMapper organizationMapper;
  private final OrganizationEndorsementService<UUID> organizationEndorsementService;

  public OrganizationResource(
      OrganizationMapper organizationMapper,
      EventBus eventBus,
      EditorAuthorizationService editorAuthorizationService,
      OrganizationEndorsementService<UUID> organizationEndorsementService,
      WithMyBatis withMyBatis) {
    super(organizationMapper, eventBus, editorAuthorizationService, withMyBatis, Organization.class);
    this.organizationEndorsementService = organizationEndorsementService;
    this.organizationMapper = organizationMapper;
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
  public UUID create(@NotNull @Trim Organization organization) {
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
  public PagingResponse<Dataset> hostedDatasets(@NotNull UUID uuid, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public PagingResponse<Dataset> publishedDatasets(@NotNull UUID uuid, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public PagingResponse<Installation> installations(@NotNull UUID uuid, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public PagingResponse<Organization> listByCountry(Country country, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public PagingResponse<Organization> listDeleted(@Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public PagingResponse<Organization> listPendingEndorsement(@Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public PagingResponse<Organization> listNonPublishing(@Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public List<KeyTitleResult> suggest(@Nullable String s) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public boolean confirmEndorsement(@NotNull UUID uuid, @NotNull UUID uuid1) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  protected List<UUID> owningEntityKeys(@NotNull Organization entity) {
    return Lists.newArrayList(entity.getEndorsingNodeKey());
  }
}
