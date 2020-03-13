/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.cli.directoryupdate.mapper;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.persistence.mapper.IdentifierMapper;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

import com.beust.jcommander.internal.Lists;

/** */
public class RegistryIdentifierMockMapper implements IdentifierMapper {

  private AtomicInteger idGenerator = new AtomicInteger(0);
  private List<Identifier> created = Lists.newArrayList();

  @Override
  public int createIdentifier(Identifier identifier) {
    identifier.setKey(idGenerator.incrementAndGet());
    created.add(identifier);
    return identifier.getKey();
  }

  @Override
  public List<Identifier> list(
      @Nullable @Param("type") IdentifierType type,
      @Nullable @Param("identifier") String identifier,
      @Nullable @Param("page") Pageable page) {
    return null;
  }

  public List<Identifier> getCreated() {
    return created;
  }
}
