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
package org.gbif.registry.directory.stubs;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.directory.Node;
import org.gbif.api.service.directory.NodeService;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Service;

@Service
public class NodeServiceStub implements NodeService {
  @Override
  public Node create(@NotNull Node node) {
    throw new UnsupportedOperationException(
        "NodeService#create not implemented yet (directory-ws needed)");
  }

  @Override
  public Node get(@NotNull Integer integer) {
    throw new UnsupportedOperationException(
        "NodeService#get not implemented yet (directory-ws needed)");
  }

  @Override
  public void update(@NotNull Node node) {
    throw new UnsupportedOperationException(
        "NodeService#update not implemented yet (directory-ws needed)");
  }

  @Override
  public void delete(@NotNull Integer integer) {
    throw new UnsupportedOperationException(
        "NodeService#delete not implemented yet (directory-ws needed)");
  }

  @Override
  public PagingResponse<Node> list(@Nullable String s, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException(
        "NodeService#list not implemented yet (directory-ws needed)");
  }
}
