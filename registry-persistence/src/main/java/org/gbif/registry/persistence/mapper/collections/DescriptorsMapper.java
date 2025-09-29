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

import org.gbif.api.model.collections.descriptors.DescriptorGroup;
import org.gbif.registry.persistence.mapper.collections.dto.DescriptorDto;
import org.gbif.registry.persistence.mapper.collections.dto.VerbatimDto;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorGroupParams;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorParams;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DescriptorsMapper {

  DescriptorGroup getDescriptorGroup(@Param("key") long key);

  void createDescriptorGroup(DescriptorGroup entity);

  void deleteDescriptorGroup(@Param("key") long key);

  void updateDescriptorGroup(DescriptorGroup entity);

  List<DescriptorGroup> listDescriptorGroups(@Param("params") DescriptorGroupParams searchParams);

  long countDescriptorGroups(@Param("params") DescriptorGroupParams searchParams);

  DescriptorDto getDescriptor(@Param("key") long key);

  void updateDescriptor(DescriptorDto descriptorDto);

  void createDescriptor(DescriptorDto entity);

  void deleteDescriptor(@Param("key") long key);

  List<DescriptorDto> listDescriptors(@Param("params") DescriptorParams searchParams);

  long countDescriptors(@Param("params") DescriptorParams searchParams);

  List<DescriptorDto> listDescriptorsWithVocabularyField(@Param("fieldName") String fieldName);

  void deleteDescriptors(@Param("descriptorGroupKey") long descriptorGroupKey);

  void createVerbatim(
      @Param("descriptorKey") long descriptorKey,
      @Param("fieldName") String fieldName,
      @Param("fieldValue") String fieldValue);

  // TODO: list deleted

  List<VerbatimDto> getVerbatimNames(long descriptorGroupKey);

  List<VerbatimDto> getVerbatimValues(@Param("descriptorKey") long descriptorKey);

  void moveDescriptorGroupForCollectionMerge(DescriptorGroup entity);
}
