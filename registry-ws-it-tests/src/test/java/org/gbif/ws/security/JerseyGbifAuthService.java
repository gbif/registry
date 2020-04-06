/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.ws.security;

import java.net.URI;

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;
import com.sun.jersey.api.client.ClientRequest;

import static org.gbif.ws.util.SecurityConstants.GBIF_SCHEME_PREFIX;
import static org.gbif.ws.util.SecurityConstants.HEADER_CONTENT_MD5;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_USER;
import static org.gbif.ws.util.SecurityConstants.HEADER_ORIGINAL_REQUEST_URL;

@Component
public class JerseyGbifAuthService {

  private static final Logger LOG = LoggerFactory.getLogger(JerseyGbifAuthService.class);

  private SigningService signingService;
  private Md5EncodeService md5EncodeService;
  private AppKeyProvider appKeyProvider;

  public JerseyGbifAuthService(
      SigningService signingService,
      Md5EncodeService md5EncodeService,
      AppKeyProvider appKeyProvider) {
    this.signingService = signingService;
    this.md5EncodeService = md5EncodeService;
    this.appKeyProvider = appKeyProvider;
  }

  /**
   * @return an absolute uri of the resource path alone, excluding host, scheme and query parameters
   */
  private String getCanonicalizedPath(final String strUri) {
    return URI.create(strUri).normalize().getPath();
  }

  private RequestDataToSign buildRequestDataToSign(final ClientRequest request) {
    final MultivaluedMap<String, Object> headers = request.getHeaders();
    final RequestDataToSign dataToSign = new RequestDataToSign();

    dataToSign.setMethod(request.getMethod());
    // custom header set by varnish overrides real URI
    // see http://dev.gbif.org/issues/browse/GBIFCOM-137
    if (headers.containsKey(HEADER_ORIGINAL_REQUEST_URL)) {
      dataToSign.setUrl((String) headers.getFirst(HEADER_ORIGINAL_REQUEST_URL));
    } else {
      dataToSign.setUrl(getCanonicalizedPath(request.getURI().toString()));
    }
    dataToSign.setContentType((String) headers.getFirst(HttpHeaders.CONTENT_TYPE));
    dataToSign.setContentTypeMd5((String) headers.getFirst(HEADER_CONTENT_MD5));
    dataToSign.setUser((String) headers.getFirst(HEADER_GBIF_USER));

    return dataToSign;
  }

  public void signRequest(String username, ClientRequest request) {
    String appKey = appKeyProvider.get();
    Preconditions.checkNotNull(appKey, "To sign the request a single application key is required");
    request.getHeaders().putSingle(HEADER_GBIF_USER, username);
    request
        .getHeaders()
        .putSingle(HEADER_ORIGINAL_REQUEST_URL, getCanonicalizedPath(request.getURI().toString()));

    if (request.getEntity() != null) {
      request
          .getHeaders()
          .putSingle(HEADER_CONTENT_MD5, md5EncodeService.encode(request.getEntity()));
    }

    RequestDataToSign requestDataToSign = buildRequestDataToSign(request);

    String signature;
    try {
      signature = this.signingService.buildSignature(requestDataToSign, appKey);
    } catch (PrivateKeyNotFoundException ex) {
      LOG.warn("Skip signing request with unknown application key: {}", appKey);
      throw new RuntimeException(ex);
    }

    String header = buildAuthHeader(appKey, signature);
    LOG.debug(
        "Adding authentication header to request {} for proxied user {} : {}",
        request.getURI(),
        username,
        header);
    request.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, header);
  }

  private static String buildAuthHeader(String applicationKey, String signature) {
    return GBIF_SCHEME_PREFIX + applicationKey + ':' + signature;
  }
}
