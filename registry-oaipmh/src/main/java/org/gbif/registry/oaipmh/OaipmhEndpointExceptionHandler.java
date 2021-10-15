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

import javax.servlet.http.HttpServletRequest;

import org.dspace.xoai.dataprovider.builder.OAIRequestParametersBuilder;
import org.dspace.xoai.dataprovider.exceptions.BadArgumentException;
import org.dspace.xoai.dataprovider.handlers.ErrorHandler;
import org.dspace.xoai.dataprovider.parameters.OAIRequest;
import org.dspace.xoai.dataprovider.repository.Repository;
import org.dspace.xoai.model.oaipmh.OAIPMH;
import org.dspace.xoai.model.oaipmh.Request;
import org.dspace.xoai.services.api.DateProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static org.dspace.xoai.dataprovider.parameters.OAIRequest.Parameter.From;
import static org.dspace.xoai.dataprovider.parameters.OAIRequest.Parameter.Identifier;
import static org.dspace.xoai.dataprovider.parameters.OAIRequest.Parameter.MetadataPrefix;
import static org.dspace.xoai.dataprovider.parameters.OAIRequest.Parameter.ResumptionToken;
import static org.dspace.xoai.dataprovider.parameters.OAIRequest.Parameter.Until;
import static org.dspace.xoai.dataprovider.parameters.OAIRequest.Parameter.Verb;

/** OAI-PMH Exception handler. */
@ControllerAdvice("org.gbif.registry.oaipmh")
public class OaipmhEndpointExceptionHandler {

  private static final String EXCEPTION_MESSAGE = "Unparseable date: ";
  private final DateProvider dateProvider;
  private final ErrorHandler errorsHandler;
  private final Repository repository;

  public OaipmhEndpointExceptionHandler(
      DateProvider dateProvider, ErrorHandler errorsHandler, Repository repository) {
    this.dateProvider = dateProvider;
    this.errorsHandler = errorsHandler;
    this.repository = repository;
  }

  /**
   * Handle {@link ParseException} from {@link OaipmhEndpoint}. Triggered when 'from' or 'until'
   * parameters are invalid. For more details see {@link
   * OaipmhEndpoint#oaipmh(OaipmhRequestParameters)}
   *
   * @param request http request, for getting request params
   * @param e source exception
   * @return xml response data with error message as bytes
   */
  @ExceptionHandler(ParseException.class)
  public ResponseEntity<byte[]> toResponse(HttpServletRequest request, ParseException e) {
    OAIRequestParametersBuilder reqBuilder =
        new OAIRequestParametersBuilder()
            .withVerb(request.getParameter(Verb.toString()))
            .withMetadataPrefix(request.getParameter(MetadataPrefix.toString()));

    // get wrong unparseable value from exception message
    String exceptionParamValue =
        e.getMessage()
            .replace(EXCEPTION_MESSAGE, "") // replace text message
            .replace("\"", ""); // replace double quotes (if present)

    String fromParamValue = request.getParameter(From.toString());

    // check if it's 'from' otherwise it's 'until'
    String paramName =
        fromParamValue != null && fromParamValue.equals(exceptionParamValue)
            ? From.toString()
            : Until.toString();

    byte[] data =
        handleOAIRequestBadArgument(reqBuilder.build(), paramName + "=" + exceptionParamValue);

    return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_XML).body(data);
  }

  /**
   * Build and generate the response for a BadArgument error code.
   *
   * @param requestParameters origin of the BadArgument error
   * @param errorMessage textual message to report
   * @return response data as bytes
   */
  private byte[] handleOAIRequestBadArgument(OAIRequest requestParameters, String errorMessage) {
    Request request =
        new Request(repository.getConfiguration().getBaseUrl())
            .withVerbType(requestParameters.get(Verb))
            .withResumptionToken(requestParameters.get(ResumptionToken))
            .withIdentifier(requestParameters.get(Identifier))
            .withMetadataPrefix(requestParameters.get(MetadataPrefix));

    try {
      OAIPMH errorResponse =
          new OAIPMH()
              .withRequest(request)
              .withResponseDate(dateProvider.now())
              .withError(errorsHandler.handle(new BadArgumentException(errorMessage)));
      return OaipmhUtils.write(errorResponse).getBytes(StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new ServiceUnavailableException("OAI Failed to serialize dataset", e);
    }
  }
}
