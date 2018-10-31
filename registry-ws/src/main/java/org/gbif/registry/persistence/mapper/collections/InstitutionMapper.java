package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.collections.Institution;
import org.gbif.registry.persistence.mapper.IdentifiableMapper;
import org.gbif.registry.persistence.mapper.TaggableMapper;

public interface InstitutionMapper extends CrudMapper<Institution>, ContactableMapper, TaggableMapper, IdentifiableMapper {

}
