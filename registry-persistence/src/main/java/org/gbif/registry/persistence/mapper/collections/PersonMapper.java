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

import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.search.collections.PersonSuggestResult;
import org.gbif.registry.persistence.mapper.collections.params.PersonSearchParams;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/** Mapper for {@link Person} entities. */
@Repository
public interface PersonMapper extends BaseMapper<Person> {

  List<Person> list(
      @Nullable @Param("params") PersonSearchParams searchParams,
      @Nullable @Param("page") Pageable page);

  long count(@Nullable @Param("params") PersonSearchParams searchParams);

  /** A simple suggest by title service. */
  List<PersonSuggestResult> suggest(@Nullable @Param("q") String q);

  /** @return the persons marked as deleted */
  List<Person> deleted(@Param("page") Pageable page);

  /** @return the count of the persons marked as deleted. */
  long countDeleted();
}
