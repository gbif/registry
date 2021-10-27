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
package org.gbif.registry.ws.util;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;

import java.util.List;

public final class WsUtils {

  private WsUtils() {}

  /**
   * Null safe builder to construct a paging response.
   *
   * @param page page to create response for, can be null
   */
  public static <D> PagingResponse<D> pagingResponse(Pageable page, Long count, List<D> result) {
    if (page == null) {
      // use default request
      page = new PagingRequest();
    }
    return new PagingResponse<>(page, count, result);
  }
}
