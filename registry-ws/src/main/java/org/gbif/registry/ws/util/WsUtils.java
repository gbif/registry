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
