package org.gbif.registry.ws.resources;

import com.google.common.eventbus.EventBus;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.registry.NetworkEntityService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;
import org.gbif.registry.events.CreateEvent;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.BaseNetworkEntityMapper;
import org.gbif.registry.ws.Trim;
import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.registry.ws.security.SecurityContextCheck;
import org.gbif.ws.WebApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.APP_ROLE;
import static org.gbif.registry.ws.security.UserRoles.EDITOR_ROLE;

public class BaseNetworkEntityResource<T extends NetworkEntity> implements NetworkEntityService<T> {

  private final BaseNetworkEntityMapper<T> mapper;
  // TODO: 2019-08-20 mb wrap it
  private final EventBus eventBus;
  private final EditorAuthorizationService userAuthService;
  private final WithMyBatis withMyBatis;
  private final Class<T> objectClass;

  public BaseNetworkEntityResource(
      BaseNetworkEntityMapper<T> mapper,
      EventBus eventBus,
      EditorAuthorizationService userAuthService,
      WithMyBatis withMyBatis,
      Class<T> objectClass) {
    this.mapper = mapper;
    this.eventBus = eventBus;
    this.userAuthService = userAuthService;
    this.withMyBatis = withMyBatis;
    this.objectClass = objectClass;
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the server
   * controlled fields for createdBy and modifiedBy. It then creates the entity.
   *
   * @param entity entity that extends NetworkEntity
   * @return key of entity created
   */
  @Override
  @RequestMapping(method = RequestMethod.POST)
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  // TODO: 2019-08-20 bval Validate -> use something else instead?
//  @Validate(groups = {PrePersist.class, Default.class})
  public UUID create(@NotNull @Valid @Trim T entity) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final UserDetails principal = (UserDetails) authentication.getPrincipal();
    // if not admin or app, verify rights
    if (!SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE, APP_ROLE)) {
      boolean allowed = false;
      for (UUID entityKeyToBeAssessed : owningEntityKeys(entity)) {
        if (entityKeyToBeAssessed == null) {
          throw new WebApplicationException(HttpStatus.FORBIDDEN);
        }
        if (userAuthService.allowedToModifyEntity(principal, entityKeyToBeAssessed)) {
          allowed = true;
          break;
        }
      }
      if (!allowed) {
        throw new WebApplicationException(HttpStatus.FORBIDDEN);
      }
    }
    entity.setCreatedBy(principal.getUsername());
    entity.setModifiedBy(principal.getUsername());

    withMyBatis.create(mapper, entity);
    eventBus.post(CreateEvent.newInstance(entity, objectClass));
    return entity.getKey();
  }

  @Override
  public void delete(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public T get(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public Map<UUID, String> getTitles(Collection<UUID> collection) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public PagingResponse<T> list(@Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public PagingResponse<T> search(String s, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public PagingResponse<T> listByIdentifier(IdentifierType identifierType, String s, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public PagingResponse<T> listByIdentifier(String s, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public PagingResponse<T> listByMachineTag(String s, @Nullable String s1, @Nullable String s2, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void update(@NotNull T t) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int addComment(@NotNull UUID uuid, @NotNull Comment comment) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteComment(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public List<Comment> listComments(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int addContact(@NotNull UUID uuid, @NotNull Contact contact) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteContact(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public List<Contact> listContacts(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void updateContact(@NotNull UUID uuid, @NotNull Contact contact) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int addEndpoint(@NotNull UUID uuid, @NotNull Endpoint endpoint) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteEndpoint(@NotNull UUID uuid, int i) {

  }

  @Override
  public List<Endpoint> listEndpoints(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int addIdentifier(@NotNull UUID uuid, @NotNull Identifier identifier) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteIdentifier(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public List<Identifier> listIdentifiers(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull MachineTag machineTag) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull TagName tagName, @NotNull String s) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull String s, @NotNull String s1, @NotNull String s2) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteMachineTag(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull TagNamespace tagNamespace) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull String s) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull TagName tagName) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull String s, @NotNull String s1) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public List<MachineTag> listMachineTags(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull String s) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull Tag tag) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteTag(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public List<Tag> listTags(@NotNull UUID uuid, @Nullable String s) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  /**
   * Override this method to extract the entity key that governs security rights for creating.
   * If null is returned only admins are allowed to create new entities which is the default.
   */
  protected List<UUID> owningEntityKeys(@NotNull T entity) {
    return new ArrayList<>();
  }
}
