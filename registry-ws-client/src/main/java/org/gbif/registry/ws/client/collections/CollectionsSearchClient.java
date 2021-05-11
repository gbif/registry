package org.gbif.registry.ws.client.collections;

import org.gbif.api.model.collections.search.CollectionsSearchResponse;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("grscicoll/search")
public interface CollectionsSearchClient {

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  List<CollectionsSearchResponse> searchCollections(
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "highlight", defaultValue = "false") boolean highlight,
      @RequestParam(value = "limit", defaultValue = "20") int limit);
}
