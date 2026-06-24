package org.gbif.registry.search.dataset.indexing.ws.taxon;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

public interface TaxonApiClient {

  @RequestMapping(
    value = "dataset/{datasetKey}/metrics",
    method = RequestMethod.GET,
    produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  JsonNode getMetrics(@PathVariable("datasetKey") UUID datasetKey);

  @RequestMapping(
    value = "taxon/search/{datasetKey}",
    method = RequestMethod.GET,
    produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  JsonNode search(@PathVariable("datasetKey") String datasetKey,@RequestBody JsonNode payload);

}
