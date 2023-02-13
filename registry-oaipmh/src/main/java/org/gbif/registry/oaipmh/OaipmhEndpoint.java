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

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/** An OAI-PMH endpoint, using the XOAI library. */
@io.swagger.v3.oas.annotations.tags.Tag(
  name = "OAI-PMH",
  description = "In addition to the RESTful JSON API, Datasets are exposed using " +
    "[OAI-PMH](https://www.openarchives.org/pmh/). " +
    "Two metadata formats can be retrieved: Ecological Metadata Language (EML) and OAI Dublin Core. " +
    "Datasets are grouped into sets according to type, country and installation.\n" +
    "\n" +
    "### Example queries\n" +
    "Example queries:\n" +
    "* Retrieve information about the OAI-PMH service: " +
    "  [Identify](https://api.gbif.org/v1/oai-pmh/registry?verb=Identify).\n" +
    "* Retrieve a list of available sets (dataset types, countries and serving installations): " +
    "  [ListSets](https://api.gbif.org/v1/oai-pmh/registry?verb=ListSets).\n" +
    "  Sets have names like `dataset_type:CHECKLIST` and `country:NL`.\n" +
    "* Retrieve the identifiers for all datasets from a particular installation: " +
    "  [ListIdentifiers](https://api.gbif.org/v1/oai-pmh/registry?verb=ListIdentifiers&metadataPrefix=oai_dc).\n" +
    "  According to the OAI-PMH protocol, metadataPrefix must be set to either oai_dc or eml, even though both formats " +
    "  are supported for all datasets.\n" +
    "* Retrieve the metadata for all datasets served by installations in a country: " +
    "  [ListRecords](https://api.gbif.org/v1/oai-pmh/registry?verb=ListRecords&metadataPrefix=oai_dc&set=Country:TG).\n" +
    "  Country codes are based on the ISO 3166-1 standard.\n" +
    "* Some queries will return more than one page of results. In this case, the XML will end with a resumption token " +
    "  element, for example: `<resumptionToken cursor=\"1\">MToxMDB8Mjpjb3VudHJ5Ok5MfDM6fDQ6fDU6b2FpX2Rj</resumptionToken>` " +
    "  The second page of results can be retrieved like this: " +
    "  [Resume](https://api.gbif.org/v1/oai-pmh/registry?verb=ListRecords&resumptionToken=MToxMDB8MjpDb3VudHJ5Ok5MfDM6fDQ6fDU6b2FpX2Rj).\n",
  externalDocs = @ExternalDocumentation(
    description = " The Open Archives Initiative Protocol for Metadata Harvesting",
    url = "https://www.openarchives.org/OAI/openarchivesprotocol.html"
  ),
  extensions = @io.swagger.v3.oas.annotations.extensions.Extension(
    name = "Order", properties = @ExtensionProperty(name = "Order", value = "2000")))
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
  @Operation(
    operationId = "oaipmh",
    summary = "Make an OAI-PMH request",
    description = "Deletes an existing institution. The institution entry gets a deleted timestamp but remains registered.",
    externalDocs = @ExternalDocumentation(
      description = "The Open Archives Initiative Protocol for Metadata Harvesting § Protocol Requests and, Responses.",
      url = "https://www.openarchives.org/OAI/openarchivesprotocol.html#ProtocolMessages"
    ))
  @ApiResponse(
    responseCode = "200",
    description = "Successful query")
  @ApiResponse(
    responseCode = "400",
    description = "Invalid query")
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
