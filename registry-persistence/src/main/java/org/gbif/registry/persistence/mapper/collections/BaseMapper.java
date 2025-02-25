/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.collections.MasterSourceMetadata;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.persistence.ContactableMapper;
import org.gbif.registry.persistence.mapper.CommentableMapper;
import org.gbif.registry.persistence.mapper.IdentifiableMapper;
import org.gbif.registry.persistence.mapper.MachineTaggableMapper;
import org.gbif.registry.persistence.mapper.PrimaryIdentifiableMapper;
import org.gbif.registry.persistence.mapper.TaggableMapper;
import org.gbif.registry.persistence.mapper.params.Count;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;

/** Generic mapper for CRUD operations. Initially implemented for collections. */
public interface BaseMapper<T extends Taggable & Identifiable & MachineTaggable & Commentable>
    extends TaggableMapper,
        IdentifiableMapper,
        MachineTaggableMapper<T>,
        CommentableMapper,
        ContactableMapper,
        OccurrenceMappeableMapper,
        ReplaceableMapper,
  PrimaryIdentifiableMapper {

  T get(@Param("key") UUID key);

  boolean exists(@Param("key") UUID key);

  void create(T entity);

  void delete(@Param("key") UUID key);

  void update(T entity);

  void addMasterSourceMetadata(
      @Param("targetEntityKey") UUID targetEntityKey,
      @Param("metadataKey") int metadataKey,
      @Param("masterSourceType") MasterSourceType masterSourceType);

  void removeMasterSourceMetadata(@Param("targetEntityKey") UUID targetEntityKey);

  MasterSourceMetadata getEntityMasterSourceMetadata(
      @Param("targetEntityKey") UUID targetEntityKey);

  List<T> findByMasterSource(@Param("source") Source source, @Param("sourceId") String sourceId);

  void updateCounts(@Param("counts") Collection<Count> counts);
}
