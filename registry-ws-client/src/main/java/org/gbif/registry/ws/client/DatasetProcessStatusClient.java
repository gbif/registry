package org.gbif.registry.ws.client;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.gbif.api.service.registry.DatasetProcessStatusService;

import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

public interface DatasetProcessStatusClient extends DatasetProcessStatusService {

  @Override
  default void createDatasetProcessStatus(
    @NotNull DatasetProcessStatus datasetProcessStatus) {
    throw new IllegalStateException("Dataset process status create not supported");
  }

  @Override
  default void updateDatasetProcessStatus(
    @NotNull DatasetProcessStatus datasetProcessStatus) {
    throw new IllegalStateException("Dataset process status update not supported");
  }

  @RequestMapping(
    method = RequestMethod.GET,
    value = "dataset/{datasetKey}/process/{attempt}",
    produces = MediaType.APPLICATION_JSON_VALUE)
  @Nullable
  @ResponseBody
  @Override
  DatasetProcessStatus getDatasetProcessStatus(
    @PathVariable("datasetKey") UUID datasetKey, @PathVariable("attempt") int attempt);

  @RequestMapping(
    method = RequestMethod.GET,
    value = "dataset/process",
    produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<DatasetProcessStatus> listDatasetProcessStatus(@SpringQueryMap Pageable page);

  @RequestMapping(
    method = RequestMethod.GET,
    value = "dataset/process/aborted",
    produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<DatasetProcessStatus> listAbortedDatasetProcesses(@SpringQueryMap Pageable page);

  @RequestMapping(
    method = RequestMethod.GET,
    value = "dataset/{datasetKey}/process",
    produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<DatasetProcessStatus> listDatasetProcessStatus(
    @PathVariable("datasetKey") UUID datasetKey, @SpringQueryMap Pageable page);
}
