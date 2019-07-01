package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.registry.Comment;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentMapper {

  int createComment(Comment comment);
}
