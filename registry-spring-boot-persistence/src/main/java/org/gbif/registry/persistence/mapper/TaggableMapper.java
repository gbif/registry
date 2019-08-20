package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.registry.Tag;

import java.util.List;
import java.util.UUID;

public interface TaggableMapper {

  int addTag(@Param("targetEntityKey") UUID entityKey, @Param("tagKey") int tagKey);

  int deleteTag(@Param("targetEntityKey") UUID entityKey, @Param("tagKey") int tagKey);

  List<Tag> listTags(@Param("targetEntityKey") UUID targetEntityKey);

}
