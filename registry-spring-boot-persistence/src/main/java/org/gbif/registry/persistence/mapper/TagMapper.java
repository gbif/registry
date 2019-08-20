package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.registry.Tag;
import org.springframework.stereotype.Repository;

@Repository
public interface TagMapper {

  int createTag(Tag tag);

}
