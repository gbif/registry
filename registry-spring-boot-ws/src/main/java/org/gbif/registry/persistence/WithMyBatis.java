package org.gbif.registry.persistence;

import com.google.common.base.Preconditions;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.CommentableMapper;
import org.gbif.registry.persistence.mapper.NetworkEntityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

// TODO: 2019-08-20 it used to be a class with static util methods
// TODO: 2019-08-20 should be in the persistence module?
@Service
public class WithMyBatis {

  @Transactional
  public <T extends NetworkEntity> UUID create(NetworkEntityMapper<T> mapper, T entity) {
    checkArgument(entity.getKey() == null, "Unable to create an entity which already has a key");
    // REVIEW: If this call fails the entity will have been modified anyway! We could make a copy and return that instead
    entity.setKey(UUID.randomUUID());
    mapper.create(entity);
    return entity.getKey();
  }

  @Transactional
  public <T extends NetworkEntity> void update(NetworkEntityMapper<T> mapper, T entity) {
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
  public <T extends NetworkEntity> void delete(NetworkEntityMapper<T> mapper, UUID key) {
    mapper.delete(key);
  }

  public <T extends NetworkEntity> T get(NetworkEntityMapper<T> mapper, UUID key) {
    return mapper.get(key);
  }

  /**
   * The simple search option of the list.
   *
   * @param mapper To use for the search
   * @param query  A simple query string such as "Pontaurus"
   * @param page   To support paging
   * @return A paging response
   */
  public <T extends NetworkEntity> PagingResponse<T> search(NetworkEntityMapper<T> mapper, String query,
                                                            Pageable page) {
    Preconditions.checkNotNull(page, "To search you must supply a page");
    long total = mapper.count(query);
    return new PagingResponse<>(page.getOffset(), page.getLimit(), total, mapper.search(query, page));
  }

  public <T extends NetworkEntity> PagingResponse<T> list(NetworkEntityMapper<T> mapper, Pageable page) {
    long total = mapper.count();
    return new PagingResponse<>(page.getOffset(), page.getLimit(), total, mapper.list(page));
  }

  public <T extends NetworkEntity> PagingResponse<T> listByIdentifier(
      NetworkEntityMapper<T> mapper,
      @Nullable IdentifierType type,
      String identifier,
      @Nullable Pageable page) {
    Preconditions.checkNotNull(page, "To list by identifier you must supply a page");
    Preconditions.checkNotNull(identifier, "To list by identifier you must supply an identifier");
    long total = mapper.countByIdentifier(type, identifier);
    return new PagingResponse<T>(page.getOffset(), page.getLimit(), total, mapper.listByIdentifier(type, identifier,
        page));
  }

  @Transactional
  public int addComment(
      CommentMapper commentMapper,
      CommentableMapper commentableMapper,
      UUID targetEntityKey,
      Comment comment) {
    checkArgument(comment.getKey() == null, "Unable to create an entity which already has a key");
    commentMapper.createComment(comment);
    commentableMapper.addComment(targetEntityKey, comment.getKey());
    return comment.getKey();
  }
}
