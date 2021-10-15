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
package org.gbif.registry.oaipmh;

import org.gbif.api.exception.ServiceUnavailableException;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;

import javax.validation.Valid;

import org.dspace.xoai.dataprovider.DataProvider;
import org.dspace.xoai.dataprovider.builder.OAIRequestParametersBuilder;
import org.dspace.xoai.dataprovider.parameters.OAIRequest;
import org.dspace.xoai.model.oaipmh.OAIPMH;
import org.dspace.xoai.services.api.DateProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** An OAI-PMH endpoint, using the XOAI library. */
@Controller
@RequestMapping("oai-pmh/registry")
public class OaipmhEndpoint {

  private final DateProvider dateProvider;
  private final DataProvider dataProvider;

  public OaipmhEndpoint(DateProvider dateProvider, DataProvider dataProvider) {
    this.dateProvider = dateProvider;
    this.dataProvider = dataProvider;
  }

  /**
   * Main OAI-PMH endpoint.
   *
   * @param params request parameters
   * @return xml response data as bytes
   * @throws ParseException in case of wrong data format parameters 'from' or 'until'
   */
  @GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity<byte[]> oaipmh(@Valid OaipmhRequestParameters params)
      throws ParseException {
    OAIRequestParametersBuilder reqBuilder =
        new OAIRequestParametersBuilder()
            .withVerb(params.getVerb())
            .withMetadataPrefix(params.getMetadataPrefix());

    Date fromDate = params.getFrom() != null ? dateProvider.parse(params.getFrom()) : null;
    Date untilDate = params.getUntil() != null ? dateProvider.parse(params.getUntil()) : null;

    reqBuilder
        .withIdentifier(params.getIdentifier())
        .withFrom(fromDate)
        .withUntil(untilDate)
        .withSet(params.getSet())
        .withResumptionToken(params.getResumptionToken());

    byte[] data = handleOAIRequest(reqBuilder.build());

    return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(data);
  }

  private byte[] handleOAIRequest(OAIRequest request) {
    try {
      OAIPMH oaipmh = dataProvider.handle(request);
      return OaipmhUtils.write(oaipmh).getBytes(StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new ServiceUnavailableException("OAI Failed to serialize dataset", e);
    }
  }
}
