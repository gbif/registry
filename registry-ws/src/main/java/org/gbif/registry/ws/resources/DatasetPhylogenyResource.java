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
package org.gbif.registry.ws.resources;

import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.ws.phylogeny.DatasetPhylogenyService;
import org.gbif.registry.ws.phylogeny.PhylogenyFile;
import org.gbif.registry.ws.phylogeny.PhylogenyFormat;
import org.gbif.ws.NotFoundException;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.forester.phylogeny.Phylogeny;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/** Phylogenetic trees published in dataset archives. */
@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Datasets",
    extensions =
        @Extension(
            name = "Order",
            properties = @ExtensionProperty(name = "Order", value = "0100")))
@Validated
@RestController
@RequestMapping(value = "dataset", produces = MediaType.APPLICATION_JSON_VALUE)
public class DatasetPhylogenyResource {

  private final DatasetPhylogenyService phylogenyService;
  private final DatasetMapper datasetMapper;

  public DatasetPhylogenyResource(
      DatasetPhylogenyService phylogenyService, DatasetMapper datasetMapper) {
    this.phylogenyService = phylogenyService;
    this.datasetMapper = datasetMapper;
  }

  @Operation(
      operationId = "listDatasetPhylogenies",
      summary = "List the phylogenetic trees of a dataset",
      description =
          "Lists the phylogeny files (Nexus, Newick or phyloXML) published in the dataset archive, "
              + "with a summary of the trees each file contains.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0600")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "List of phylogeny files")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/phylogeny")
  public List<PhylogenyFile> listPhylogenies(@PathVariable("key") UUID datasetKey) {
    checkDatasetExists(datasetKey);
    return phylogenyService.list(datasetKey);
  }

  @Operation(
      operationId = "getDatasetPhylogeny",
      summary = "Retrieve a phylogenetic tree file of a dataset",
      description = "Streams the content of a phylogeny file published in the dataset archive.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0601")))
  @Docs.DefaultEntityKeyParameter
  @Parameter(
      name = "fileName",
      description = "Name of the phylogeny file, as returned by the list of phylogenies",
      in = ParameterIn.PATH)
  @ApiResponse(
      responseCode = "200",
      description = "Phylogeny file",
      content = {
        @Content(mediaType = MediaType.TEXT_PLAIN_VALUE),
        @Content(mediaType = MediaType.APPLICATION_XML_VALUE)
      })
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping(value = "{key}/phylogeny/{fileName}", produces = MediaType.ALL_VALUE)
  public ResponseEntity<Resource> getPhylogeny(
      @PathVariable("key") UUID datasetKey, @PathVariable("fileName") String fileName) {
    checkDatasetExists(datasetKey);
    Path file =
        phylogenyService
            .getFile(datasetKey, fileName)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Phylogeny " + fileName + " not found for dataset " + datasetKey,
                        URI.create("/dataset/" + datasetKey + "/phylogeny")));

    MediaType mediaType =
        DatasetPhylogenyService.format(file)
            .map(PhylogenyFormat::getMediaType)
            .orElse(MediaType.TEXT_PLAIN);

    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline().filename(file.getFileName().toString()).build().toString())
        .body(new FileSystemResource(file));
  }

  @Operation(
      operationId = "getDatasetPhylogenyTree",
      summary = "Retrieve a single tree of a phylogeny file of a dataset",
      description =
          "Returns one tree of a phylogeny file, by its index in the file as returned by the list "
              + "of phylogenies. Useful for files containing many trees, e.g. Nexus files with "
              + "posterior samples.",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0602")))
  @Docs.DefaultEntityKeyParameter
  @Parameter(
      name = "fileName",
      description = "Name of the phylogeny file, as returned by the list of phylogenies",
      in = ParameterIn.PATH)
  @Parameter(
      name = "index",
      description = "Position of the tree in the file, starting at 0",
      in = ParameterIn.PATH)
  @Parameter(
      name = "format",
      description = "Format of the returned tree. Defaults to the format of the file.",
      schema = @Schema(implementation = PhylogenyFormat.class),
      in = ParameterIn.QUERY)
  @ApiResponse(
      responseCode = "200",
      description = "Phylogenetic tree",
      content = {
        @Content(mediaType = MediaType.TEXT_PLAIN_VALUE),
        @Content(mediaType = MediaType.APPLICATION_XML_VALUE)
      })
  @ApiResponse(
      responseCode = "422",
      description = "The phylogeny file can't be parsed",
      content = @Content)
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping(value = "{key}/phylogeny/{fileName}/{index}", produces = MediaType.ALL_VALUE)
  public ResponseEntity<String> getPhylogenyTree(
      @PathVariable("key") UUID datasetKey,
      @PathVariable("fileName") String fileName,
      @PathVariable("index") int index,
      @RequestParam(value = "format", required = false) PhylogenyFormat format) {
    checkDatasetExists(datasetKey);
    Phylogeny tree;
    try {
      tree =
          phylogenyService
              .getTree(datasetKey, fileName, index)
              .orElseThrow(
                  () ->
                      new NotFoundException(
                          "Tree " + index + " of phylogeny " + fileName + " not found for dataset "
                              + datasetKey,
                          URI.create("/dataset/" + datasetKey + "/phylogeny/" + fileName)));
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Invalid phylogeny file " + fileName, e);
    }

    if (format == null) {
      format =
          phylogenyService
              .getFile(datasetKey, fileName)
              .flatMap(DatasetPhylogenyService::format)
              .orElse(PhylogenyFormat.NEWICK);
    }

    try {
      return ResponseEntity.ok()
          .contentType(new MediaType(format.getMediaType(), StandardCharsets.UTF_8))
          .body(format.write(tree));
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error writing tree " + index + " of " + fileName, e);
    }
  }

  private void checkDatasetExists(UUID datasetKey) {
    if (datasetMapper.get(datasetKey) == null) {
      throw new NotFoundException(
          "Dataset " + datasetKey + " not found", URI.create("/dataset/" + datasetKey));
    }
  }
}
