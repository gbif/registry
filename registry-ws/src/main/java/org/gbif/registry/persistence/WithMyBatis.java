package org.gbif.registry.persistence;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.CommentableMapper;
import org.gbif.registry.persistence.mapper.ContactMapper;
import org.gbif.registry.persistence.mapper.ContactableMapper;
import org.gbif.registry.persistence.mapper.EndpointMapper;
import org.gbif.registry.persistence.mapper.EndpointableMapper;
import org.gbif.registry.persistence.mapper.IdentifiableMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.MachineTaggableMapper;
import org.gbif.registry.persistence.mapper.NetworkEntityMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.TaggableMapper;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import org.mybatis.guice.transactional.Transactional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class WithMyBatis {

  @Transactional
  public static <T extends NetworkEntity> UUID create(NetworkEntityMapper<T> mapper, T entity) {
    checkArgument(entity.getKey() == null, "Unable to create an entity which already has a key");
    // REVIEW: If this call fails the entity will have been modified anyway! We could make a copy and return that
// instead
    entity.setKey(UUID.randomUUID());
    mapper.create(entity);
    return entity.getKey();
  }

  public static <T extends NetworkEntity> T get(NetworkEntityMapper<T> mapper, UUID key) {
    return mapper.get(key);
  }

  @Transactional
  public static <T extends NetworkEntity> void update(NetworkEntityMapper<T> mapper, T entity) {
    checkNotNull(entity, "Unable to update an entity when it is not provided");
    T existing = mapper.get(entity.getKey());
    checkNotNull(existing, "Unable to update a non existing entity");

    if (existing.getDeleted() != null) {
      // allow updates ONLY if they are undeleting too
      checkArgument(entity.getDeleted() == null,
        "Unable to update a previously deleted entity unless you clear the deletion timestamp");
    } else {
      // do not allow deletion here (for safety) - we have an explicity deletion service
      checkArgument(entity.getDeleted() == null, "Cannot delete using the update service.  Use the deletion service");
    }

    mapper.update(entity);
  }

  @Transactional
  public static <T extends NetworkEntity> void delete(NetworkEntityMapper<T> mapper, UUID key) {
    mapper.delete(key);
  }

  public static <T extends NetworkEntity> PagingResponse<T> list(NetworkEntityMapper<T> mapper, Pageable page) {
    long total = mapper.count();
    return new PagingResponse<T>(page.getOffset(), page.getLimit(), total, mapper.list(page));
  }

  /**
   * The simple search option of the list.
   *
   * @param mapper To use for the search
   * @param query A simple query string such as "Pontaurus"
   * @param page To support paging
   * @return A paging response
   */
  public static <T extends NetworkEntity> PagingResponse<T> search(NetworkEntityMapper<T> mapper, String query,
    Pageable page) {
    Preconditions.checkNotNull(page, "To search you must supply a page");
    long total = mapper.count(query);
    return new PagingResponse<T>(page.getOffset(), page.getLimit(), total, mapper.search(query, page));
  }

  public static <T extends NetworkEntity> PagingResponse<T> listByIdentifier(NetworkEntityMapper<T> mapper,
    @Nullable IdentifierType type, String identifier, @Nullable Pageable page) {
    Preconditions.checkNotNull(page, "To list by identifier you must supply a page");
    Preconditions.checkNotNull(identifier, "To list by identifier you must supply an identifier");
    long total = mapper.countByIdentifier(type, identifier);
    return new PagingResponse<T>(page.getOffset(), page.getLimit(), total, mapper.listByIdentifier(type, identifier,
      page));
  }


  @Transactional
  public static int addContact(
    ContactMapper contactMapper, ContactableMapper contactableMapper, UUID targetEntityKey, Contact contact
    ) {
    checkArgument(contact.getKey() == null, "Unable to create an entity which already has a key");
    contactMapper.createContact(contact);
    // is this a primary contact? We need to make sure it only exists once per type
    if (contact.isPrimary()) {
      contactableMapper.updatePrimaryContacts(targetEntityKey, contact.getType());
    }
    contactableMapper.addContact(targetEntityKey, contact.getKey(), contact.getType(), contact.isPrimary());
    return contact.getKey();
  }

  @Transactional
  public static int updateContact(ContactMapper contactMapper, ContactableMapper contactableMapper,
    UUID targetEntityKey, Contact contact) {
    checkArgument(contact.getKey() != null, "Unable to update an entity with no key");
    // null safe checking follows
    checkArgument(Boolean.TRUE.equals(contactableMapper.areRelated(targetEntityKey, contact.getKey())),
      "The provided contact is not connected to the given entity");
    // is this a primary contact? We need to make sure it only exists once per type
    if (contact.isPrimary()) {
      contactableMapper.updatePrimaryContacts(targetEntityKey, contact.getType());
    }
    contactMapper.updateContact(contact);
    // update the type and is_primary (is_primary will have been set to false by updatePrimaryContacts above)
    contactableMapper.updateContact(targetEntityKey, contact.getKey(), contact.getType(), contact.isPrimary());
    return contact.getKey();
  }

  public static void deleteContact(ContactableMapper contactableMapper, UUID targetEntityKey, int contactKey) {
    contactableMapper.deleteContact(targetEntityKey, contactKey);
  }

  public static List<Contact> listContacts(ContactableMapper contactableMapper, UUID targetEntityKey) {
    return contactableMapper.listContacts(targetEntityKey);
  }

  @Transactional
  public static int addEndpoint(
    EndpointMapper endpointMapper, EndpointableMapper endpointableMapper, UUID targetEntityKey, Endpoint endpoint,
    MachineTagMapper machineTagMapper
    ) {
    checkArgument(endpoint.getKey() == null, "Unable to create an entity which already has a key");
    endpointMapper.createEndpoint(endpoint);
    endpointableMapper.addEndpoint(targetEntityKey, endpoint.getKey());

    for (MachineTag machineTag : endpoint.getMachineTags()) {
      machineTag.setCreatedBy(endpoint.getCreatedBy());
      WithMyBatis.addMachineTag(machineTagMapper, endpointMapper, endpoint.getKey(), machineTag);
    }
    return endpoint.getKey();
  }

  /**
   * Private scoped to avoid misuse. Endpoints break the general principle and DO persist nested entities, and can
   * safely do so because they are immutable.
   */
  private static void addMachineTag(MachineTagMapper machineTagMapper, EndpointMapper endpointMapper,
    Integer endpointKey,
    MachineTag machineTag) {
    machineTagMapper.createMachineTag(machineTag);
    endpointMapper.addMachineTag(endpointKey, machineTag.getKey());
  }

  public static void deleteEndpoint(EndpointableMapper endpointableMapper, UUID targetEntityKey, int endpointKey) {
    endpointableMapper.deleteEndpoint(targetEntityKey, endpointKey);
  }

  public static List<Endpoint> listEndpoints(EndpointableMapper endpointableMapper, UUID targetEntityKey) {
    return endpointableMapper.listEndpoints(targetEntityKey);
  }

  public static int addMachineTag(
    MachineTagMapper machineTagMapper,
    MachineTaggableMapper machineTaggableMapper,
    UUID targetEntityKey,
    MachineTag machineTag
    ) {
    checkArgument(machineTag.getKey() == null, "Unable to create an entity which already has a key");
    machineTagMapper.createMachineTag(machineTag);
    machineTaggableMapper.addMachineTag(targetEntityKey, machineTag.getKey());
    return machineTag.getKey();
  }

  public static void deleteMachineTag(
    MachineTaggableMapper machineTaggableMapper, UUID targetEntityKey, int machineTagKey
    ) {
    machineTaggableMapper.deleteMachineTag(targetEntityKey, machineTagKey);
  }

  public static void deleteMachineTags(
    MachineTaggableMapper machineTaggableMapper, UUID targetEntityKey, String namespace, @Nullable String name
  ) {
    machineTaggableMapper.deleteMachineTags(targetEntityKey, namespace, name);
  }

  public static List<MachineTag> listMachineTags(MachineTaggableMapper machineTaggableMapper, UUID targetEntityKey) {
    return machineTaggableMapper.listMachineTags(targetEntityKey);
  }

  public static <T> PagingResponse<T> listByMachineTag(
    MachineTaggableMapper mapper, String namespace, @Nullable String name, @Nullable String value, @Nullable Pageable page
  ) {
    Preconditions.checkNotNull(page, "To list by machine tag you must supply a page");
    Preconditions.checkNotNull(namespace, "To list by machine tag you must supply a namespace");
    long total = mapper.countByMachineTag(namespace, name, value);
    return new PagingResponse<T>(page.getOffset(), page.getLimit(), total, mapper.listByMachineTag(namespace, name, value, page));
  }

  @Transactional
  public static int addTag(TagMapper tagMapper, TaggableMapper taggableMapper, UUID targetEntityKey, Tag tag) {
    // Mybatis needs an object to set the key on
    tagMapper.createTag(tag);
    taggableMapper.addTag(targetEntityKey, tag.getKey());
    return tag.getKey();
  }

  public static void deleteTag(TaggableMapper taggableMapper, UUID targetEntityKey, int tagKey) {
    taggableMapper.deleteTag(targetEntityKey, tagKey);
  }

  public static List<Tag> listTags(TaggableMapper taggableMapper, UUID targetEntityKey, String owner) {
    // TODO: support the owner
    return taggableMapper.listTags(targetEntityKey);
  }

  @Transactional
  public static int addIdentifier(
    IdentifierMapper identifierMapper,
    IdentifiableMapper identifiableMapper,
    UUID targetEntityKey,
    Identifier identifier
    ) {
    checkArgument(identifier.getKey() == null, "Unable to create an entity which already has a key");
    identifierMapper.createIdentifier(identifier);
    identifiableMapper.addIdentifier(targetEntityKey, identifier.getKey());
    return identifier.getKey();
  }

  public static void deleteIdentifier(IdentifiableMapper identifiableMapper, UUID targetEntityKey, int identifierKey) {
    identifiableMapper.deleteIdentifier(targetEntityKey, identifierKey);
  }

  public static List<Identifier> listIdentifiers(IdentifiableMapper identifiableMapper, UUID targetEntityKey) {
    return identifiableMapper.listIdentifiers(targetEntityKey);
  }

  @Transactional
  public static int addComment(
    CommentMapper commentMapper, CommentableMapper commentableMapper, UUID targetEntityKey, Comment comment
    ) {
    checkArgument(comment.getKey() == null, "Unable to create an entity which already has a key");
    commentMapper.createComment(comment);
    commentableMapper.addComment(targetEntityKey, comment.getKey());
    return comment.getKey();
  }

  public static void deleteComment(CommentableMapper commentableMapper, UUID targetEntityKey, int commentKey) {
    commentableMapper.deleteComment(targetEntityKey, commentKey);
  }

  public static List<Comment> listComments(CommentableMapper commentableMapper, UUID targetEntityKey) {
    return commentableMapper.listComments(targetEntityKey);
  }

  private WithMyBatis() {
    throw new UnsupportedOperationException("Can't initialize class");
  }

}
