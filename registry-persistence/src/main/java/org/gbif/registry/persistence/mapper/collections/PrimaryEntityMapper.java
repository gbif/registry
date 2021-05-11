package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Taggable;
import org.gbif.registry.persistence.ContactableMapper;

public interface PrimaryEntityMapper<
        T extends Taggable & Identifiable & MachineTaggable & Commentable>
    extends BaseMapper<T>, ContactableMapper, OccurrenceMappeableMapper, ReplaceableMapper {}
