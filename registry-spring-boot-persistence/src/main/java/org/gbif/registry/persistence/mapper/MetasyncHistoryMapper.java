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
package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.metasync.MetasyncHistory;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

// TODO: 26/08/2019 xml mapper has the different name
/** Mapper that perform operations on {@link MetasyncHistory} instances. */
@Repository
public interface MetasyncHistoryMapper {

  int count();

  int countByInstallation(@Param("installationKey") UUID installationKey);

  void create(MetasyncHistory metasyncHistory);

  List<MetasyncHistory> list(@Nullable @Param("page") Pageable page);

  List<MetasyncHistory> listByInstallation(
      @Param("installationKey") UUID installationKey, @Nullable @Param("page") Pageable page);
}
