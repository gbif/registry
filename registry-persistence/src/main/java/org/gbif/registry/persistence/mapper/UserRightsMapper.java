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

import org.gbif.api.vocabulary.Country;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRightsMapper {

  boolean keyExistsForUser(@Param("username") String username, @Param("key") UUID key);

  List<UUID> getKeysByUser(@Param("username") String username);

  boolean namespaceExistsForUser(@Param("username") String username, @Param("ns") String namespace);

  boolean allowedToDeleteMachineTag(
      @Param("username") String username, @Param("key") int machineTagKey);

  List<String> getNamespacesByUser(@Param("username") String username);

  boolean countryExistsForUser(
      @Param("username") String username, @Param("country") String country);

  List<Country> getCountriesByUser(@Param("username") String username);
}
