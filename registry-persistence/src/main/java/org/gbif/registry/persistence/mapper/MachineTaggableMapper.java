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
package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

public interface MachineTaggableMapper<T extends MachineTaggable> {

  int addMachineTag(
      @Param("targetEntityKey") UUID entityKey, @Param("machineTagKey") int machineTagKey);

  int deleteMachineTag(
      @Param("targetEntityKey") UUID entityKey, @Param("machineTagKey") int machineTagKey);

  int deleteMachineTags(
      @Param("targetEntityKey") UUID entityKey,
      @Param("namespace") String namespace,
      @Nullable @Param("name") String name);

  List<MachineTag> listMachineTags(@Param("targetEntityKey") UUID targetEntityKey);

  long countByMachineTag(
      @Param("namespace") String namespace,
      @Nullable @Param("name") String name,
      @Nullable @Param("value") String value);

  List<T> listByMachineTag(
      @Param("namespace") String namespace,
      @Nullable @Param("name") String name,
      @Nullable @Param("value") String value,
      @Param("page") Pageable page);
}
