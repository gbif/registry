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
package org.gbif.registry.persistence;

import org.gbif.api.model.collections.Contact;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Param;

/**
 * Generic mapper to work with collections-related contacts. It works with {@link Contact} entities.
 */
// TODO: 29/08/2019 conflicts with existing one
public interface ContactableMapper {

  List<Contact> listContactPersons(@Param("key") UUID key);

  void addContactPerson(@Param("entityKey") UUID entityKey, @Param("contactKey") int contactKey);

  void removeContactPerson(@Param("entityKey") UUID entityKey, @Param("contactKey") int contactKey);

  void removeAllContactPersons(@Param("entityKey") UUID entityKey);
}
