package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.registry.Comment;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentableMapper {

  int addComment(@Param("targetEntityKey") UUID entityKey, @Param("commentKey") int commentKey);

  int deleteComment(@Param("targetEntityKey") UUID entityKey, @Param("commentKey") int commentKey);

  List<Comment> listComments(@Param("targetEntityKey") UUID identifierKey);

}
