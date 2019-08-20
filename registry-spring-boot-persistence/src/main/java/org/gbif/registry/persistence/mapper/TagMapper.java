package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.registry.Tag;

public interface TagMapper {

  int createTag(Tag tag);

}
