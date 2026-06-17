package org.gbif.registry.search.dataset.indexing.ws.taxon;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

import org.gbif.checklistbank.ws.client.DatasetMetricsClient;

import org.gbif.metrics.ws.client.CubeWsClient;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
  ResponseEntity<JsonNode> getMetrics(@PathVariable("datasetKey") UUID datasetKey);

  @RequestMapping(
    value = "taxon/{datasetKey}/search",
    method = RequestMethod.GET,
    produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  ResponseEntity<JsonNode> search(@RequestBody JsonNode payload);

}
