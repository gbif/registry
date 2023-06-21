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
package org.gbif.registry.ws.resources.legacy;

import org.gbif.api.model.registry.Network;
import org.gbif.registry.domain.ws.IptNetworkBriefResponse;
import org.gbif.registry.persistence.mapper.NetworkMapper;

import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import org.gbif.registry.persistence.mapper.params.NetworkListParams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

/**
 * Handle all web service Network requests from IPT.
 */
@Hidden
@RestController
@RequestMapping("registry")
public class IptNetworkResource {

  private static final Logger LOG = LoggerFactory.getLogger(IptNetworkResource.class);

  private final NetworkMapper networkMapper;

  public IptNetworkResource(NetworkMapper networkMapper) {
    this.networkMapper = networkMapper;
  }

  /**
   * Get a list of all Networks, handling incoming request with path
   * /registry/network.json. For each Network, only the key and title(name) fields are
   * required. No authorization is required for this request.
   *
   * @return list of all Networks
   */
  @GetMapping(
      value = {"network", "network{extension:\\.[a-z]+}"},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<IptNetworkBriefResponse>> getNetworks(
      @PathVariable(required = false, value = "extension") String extension,
      HttpServletResponse response) {
    LOG.debug("List all Networks for IPT");

    String responseType;
    if (".json".equals(extension) || StringUtils.isEmpty(extension)) {
      responseType = "application/json";
      response.setContentType(responseType);
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .cacheControl(CacheControl.noCache())
          .build();
    }

    List<IptNetworkBriefResponse> networks = networkMapper.listNetworksBrief();

    return ResponseEntity.status(HttpStatus.OK)
        .cacheControl(CacheControl.noCache())
        .contentType(MediaType.parseMediaType(responseType))
        .body(networks);
  }

  @GetMapping(value = "resource/{key}/networks", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<Network> listResourceNetworks(@PathVariable("key") UUID datasetKey) {
    return networkMapper.list(NetworkListParams.builder().datasetKey(datasetKey).build());
  }
}
