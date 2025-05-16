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

import org.gbif.registry.domain.ws.IptDatasetValidationRequest;
import org.gbif.validator.api.Validation;
import org.gbif.validator.api.ValidationRequest;
import org.gbif.validator.ws.client.ValidationWsClient;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Hidden;
import feign.FeignException;

@Hidden
@RestController
@RequestMapping("registry/validation")
public class IptDatasetValidationResource {

  private static final Logger LOG = LoggerFactory.getLogger(IptDatasetValidationResource.class);

  private final ValidationWsClient validationClient;

  public IptDatasetValidationResource(ValidationWsClient validationClient) {
    this.validationClient = validationClient;
  }

  // TODO: remove and use the URL validation only?
  @PostMapping(
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Validation> validate(
      @RequestParam("file") MultipartFile fileToValidate,
      @ModelAttribute IptDatasetValidationRequest dvr) {

    if (fileToValidate == null || fileToValidate.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    File tempFile = null;
    Validation validation = null;

    try {
      tempFile = File.createTempFile("validate-file", UUID.randomUUID().toString());
      fileToValidate.transferTo(tempFile);

      ValidationRequest validationRequest = ValidationRequest.builder()
          .sourceId(dvr.getSourceId())
          .installationKey(dvr.getInstallationKey())
          .notificationEmail(dvr.getNotificationEmails())
          .build();

      validation = validationClient.validateFile(tempFile, validationRequest);
    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    } finally {
      if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
        LOG.error("Failed to delete temp validation file: {}", tempFile.getAbsolutePath());
      }
    }

    return ResponseEntity.ok(validation);
  }

  @PostMapping(
      value = "url",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Validation> validateUrl(
      @RequestParam("fileToValidate") String fileToValidate,
      @ModelAttribute IptDatasetValidationRequest dvr) {

    try {
      // TODO: Refactor
      URL url = new URL(fileToValidate);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("HEAD");
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);

      int responseCode = connection.getResponseCode();
      if (responseCode >= 400) {
        LOG.error("URL is not accessible: {}", url);
        return ResponseEntity.badRequest().build();
      }

      ValidationRequest validationRequest = ValidationRequest.builder()
          .sourceId(dvr.getSourceId())
          .installationKey(dvr.getInstallationKey())
          .notificationEmail(dvr.getNotificationEmails())
          .build();

      Validation validation = validationClient.validateFileFromUrl(fileToValidate, validationRequest);

      return ResponseEntity.ok(validation);
    } catch (MalformedURLException e) {
      LOG.error("Invalid URL format: {}", fileToValidate);
      return ResponseEntity.badRequest().build();
    } catch (IOException e) {
      LOG.error("Error connecting to URL: {}", fileToValidate);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping(
      value = "{validationKey}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Validation> getValidation(@PathVariable UUID validationKey) {
    try {
      Validation validation = validationClient.get(validationKey);
      return ResponseEntity.ok(validation);
    } catch (FeignException.NotFound e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
  }
}
