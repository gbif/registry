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
package org.gbif.registry.ws.it.fixtures;

import org.gbif.ws.security.GbifAuthUtils;
import org.gbif.ws.security.RequestDataToSign;
import org.gbif.ws.security.SigningService;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.xml.bind.JAXBElement;

import static org.gbif.registry.ws.it.fixtures.TestConstants.IT_APP_KEY;
import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_PASSWORD;
import static org.gbif.ws.util.SecurityConstants.GBIF_SCHEME;
import static org.gbif.ws.util.SecurityConstants.HEADER_CONTENT_MD5;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_DATE;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_USER;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@SuppressWarnings("unused")
@Component
public class RequestTestFixture {

  private MockMvc mvc;
  private SigningService signingService;
  @Qualifier("registryObjectMapper") private ObjectMapper objectMapper;
  private Jaxb2Marshaller marshaller;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired(required = false)
  public void setMvc(MockMvc mvc) {
    this.mvc = mvc;
  }

  private MockMvc getMvc() {
    if (mvc == null && applicationContext != null) {
      mvc = applicationContext.getBean(MockMvc.class);
    }
    return mvc;
  }

  @Autowired
  public void setSigningService(SigningService signingService) {
    this.signingService = signingService;
  }


  @Autowired
  public void setObjectMapper(@Qualifier("registryObjectMapper") ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Autowired
  public void setMarshaller(Jaxb2Marshaller marshaller) {
    this.marshaller = marshaller;
  }

  public ResultActions getRequest(String path) throws Exception {
    return getMvc().perform(get(path));
  }

  public ResultActions getRequest(URI path) throws Exception {
    return getMvc().perform(get(path));
  }

  public ResultActions getRequest(String username, String password, String path) throws Exception {
    return getMvc().perform(get(path).with(httpBasic(username, password)));
  }

  public ResultActions postRequest(String path) throws Exception {
    return getMvc().perform(post(path));
  }

  public ResultActions deleteRequest(String path) throws Exception {
    return getMvc().perform(delete(path));
  }

  public ResultActions deleteRequest(String username, String path) throws Exception {
    return getMvc().perform(delete(path).with(httpBasic(username, TEST_PASSWORD)));
  }

  public ResultActions deleteRequest(String username, String password, String path)
      throws Exception {
    return getMvc().perform(delete(path).with(httpBasic(username, password)));
  }

  public ResultActions postRequest(String username, String path) throws Exception {
    return getMvc().perform(post(path).with(httpBasic(username, TEST_PASSWORD)));
  }

  public ResultActions postRequest(String username, String password, String path) throws Exception {
    return getMvc().perform(post(path).with(httpBasic(username, password)));
  }

  public ResultActions postRequest(Object entity, String path) throws Exception {
    return getMvc().perform(
        post(path).content(objectMapper.writeValueAsString(entity)).contentType(APPLICATION_JSON));
  }

  public ResultActions postRequest(String bearer, Object entity, String path) throws Exception {
    return getMvc().perform(
        post(path)
            .content(objectMapper.writeValueAsString(entity))
            .contentType(APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer " + bearer));
  }

  public ResultActions postRequest(HttpHeaders headers, Object entity, String path)
      throws Exception {
    return getMvc().perform(
        post(path)
            .content(objectMapper.writeValueAsString(entity))
            .contentType(APPLICATION_JSON)
            .headers(headers));
  }

  public ResultActions postRequest(String username, String password, Object entity, String path)
      throws Exception {
    return getMvc().perform(
        post(path)
            .content(objectMapper.writeValueAsString(entity))
            .contentType(APPLICATION_JSON)
            .with(httpBasic(username, password)));
  }

  public ResultActions postRequestUrlEncoded(
      MultiValueMap<String, String> params, Object username, String password, String path)
      throws Exception {
    return postRequestUrlEncoded(params, APPLICATION_XML, username, password, path);
  }

  public ResultActions postRequestUrlEncoded(
      MultiValueMap<String, String> params,
      MediaType responseType,
      Object username,
      String password,
      String path)
      throws Exception {
    return getMvc().perform(
        post(path)
            .params(params)
            .contentType(APPLICATION_FORM_URLENCODED)
            .accept(responseType)
            .with(httpBasic(username.toString(), password)));
  }

  public ResultActions deleteRequestUrlEncoded(String path) throws Exception {
    return getMvc().perform(delete(path).contentType(APPLICATION_FORM_URLENCODED));
  }

  public ResultActions deleteRequestUrlEncoded(Object username, String password, String path)
      throws Exception {
    return getMvc().perform(
        delete(path)
            .contentType(APPLICATION_FORM_URLENCODED)
            .with(httpBasic(username.toString(), password)));
  }

  public ResultActions putRequest(String username, String password, Object entity, String path)
      throws Exception {
    return getMvc().perform(
        put(path)
            .content(objectMapper.writeValueAsString(entity))
            .contentType(APPLICATION_JSON)
            .with(httpBasic(username, password)));
  }

  public ResultActions putSignedRequest(String username, Object entity, String path)
      throws Exception {
    String content = objectMapper.writeValueAsString(entity);

    HttpHeaders authHeaders =
        prepareGbifAuthorizationHeadersWithContent(
            PUT, path, APPLICATION_JSON_UTF8, content, username, IT_APP_KEY);

    return getMvc().perform(
        put(path).content(content).contentType(APPLICATION_JSON).headers(authHeaders));
  }

  public ResultActions postSignedRequest(String username, String path) throws Exception {
    HttpHeaders authHeaders =
        prepareGbifAuthorizationHeadersNoContent(POST, path, username, IT_APP_KEY);

    return getMvc().perform(post(path).headers(authHeaders));
  }

  public ResultActions postSignedRequest(String username, Object entity, String path)
      throws Exception {
    String content = objectMapper.writeValueAsString(entity);

    HttpHeaders authHeaders =
        prepareGbifAuthorizationHeadersWithContent(
            POST, path, APPLICATION_JSON_UTF8, content, username, IT_APP_KEY);

    return getMvc().perform(
        post(path).content(content).contentType(APPLICATION_JSON).headers(authHeaders));
  }

  public ResultActions postSignedRequestPlainText(String username, Object entity, String path)
      throws Exception {
    HttpHeaders authHeaders =
        prepareGbifAuthorizationHeadersWithContentAsPlainText(
            POST, path, "text/plain;charset=UTF-8", entity, username, IT_APP_KEY);

    return getMvc().perform(
        post(path).content(entity.toString()).contentType(TEXT_PLAIN).headers(authHeaders));
  }

  public ResultActions postSignedRequest(String username, String appKey, Object entity, String path)
      throws Exception {
    String content = objectMapper.writeValueAsString(entity);

    HttpHeaders authHeaders =
        prepareGbifAuthorizationHeadersWithContent(
            POST, path, APPLICATION_JSON_UTF8, content, username, appKey);

    return getMvc().perform(
        post(path).content(content).contentType(APPLICATION_JSON).headers(authHeaders));
  }

  public ResultActions getSignedRequest(String username, String path) throws Exception {
    HttpHeaders authHeaders =
        prepareGbifAuthorizationHeadersNoContent(GET, path, username, IT_APP_KEY);

    return getMvc().perform(get(path).headers(authHeaders));
  }

  public ResultActions getSignedRequest(String username, String path, Map<String, String> params)
      throws Exception {
    String query =
        params.entrySet().stream()
            .map(
                e ->
                    URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
    HttpHeaders authHeaders =
        prepareGbifAuthorizationHeadersNoContent(
            GET, query.isEmpty() ? path : path + "?" + query, username, IT_APP_KEY);

    Map<String, List<String>> queryParams =
        params.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Entry::getKey, entry -> Collections.singletonList(entry.getValue())));

    return getMvc().perform(
        get(path).queryParams(new LinkedMultiValueMap<>(queryParams)).headers(authHeaders));
  }

  public ResultActions deleteSignedRequest(String username, String path) throws Exception {
    HttpHeaders authHeaders =
        prepareGbifAuthorizationHeadersNoContent(DELETE, path, username, IT_APP_KEY);

    return getMvc().perform(delete(path).headers(authHeaders));
  }

  public String extractResponse(ResultActions actions) throws Exception {
    return actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
  }

  public <T> T extractJsonResponse(ResultActions actions, Class<T> entityClass) throws Exception {
    String content = actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    return objectMapper.readValue(content, entityClass);
  }

  public <T> T extractJsonResponse(ResultActions actions, TypeReference<T> typeReference)
      throws Exception {
    String content = actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    return objectMapper.readValue(content, typeReference);
  }

  public <T> T extractXmlResponse(ResultActions actions, Class<T> entityClass) throws Exception {
    byte[] content = actions.andReturn().getResponse().getContentAsByteArray();
    JAXBElement<T> jaxbElement =
        marshaller
            .createUnmarshaller()
            .unmarshal(new StreamSource(new ByteArrayInputStream(content)), entityClass);

    return jaxbElement.getValue();
  }

  public String getHeader(ResultActions actions, String header) {
    return actions.andReturn().getResponse().getHeader(header);
  }

  /** Headers of a request signed with an app key, with a JSON body. */
  public HttpHeaders prepareGbifAuthorizationHeadersWithContent(
      RequestMethod method,
      String requestUrl,
      MediaType contentType,
      Object data,
      String user,
      String appKey)
      throws Exception {
    // data is the body as sent, serialize it only if it is not already a string
    String content = data instanceof String ? (String) data : objectMapper.writeValueAsString(data);
    return prepareGbifAuthorizationHeaders(
        method,
        requestUrl,
        // MockMvc adds the charset of a String body, the signature covers the header as sent
        APPLICATION_JSON_VALUE + ";charset=UTF-8",
        content.getBytes(StandardCharsets.UTF_8),
        user,
        appKey);
  }

  /** Headers of a request signed with an app key, with a plain text body. */
  public HttpHeaders prepareGbifAuthorizationHeadersWithContentAsPlainText(
      RequestMethod method,
      String requestUrl,
      String contentType,
      Object data,
      String user,
      String appKey) {
    return prepareGbifAuthorizationHeaders(
        method,
        requestUrl,
        TEXT_PLAIN_VALUE + ";charset=UTF-8",
        data.toString().getBytes(StandardCharsets.UTF_8),
        user,
        appKey);
  }

  /** Headers of a request signed with an app key, without body. */
  public HttpHeaders prepareGbifAuthorizationHeadersNoContent(
      RequestMethod method, String requestUrl, String user, String appKey) {
    return prepareGbifAuthorizationHeaders(method, requestUrl, null, null, user, appKey);
  }

  /**
   * Signs a request as described in gbif-common-ws' README (GBIF request signing): the canonical
   * URL, content type, Content-MD5 of the exact body, user and date are signed.
   *
   * @param requestUrl path of the request, with its query string if any
   */
  private HttpHeaders prepareGbifAuthorizationHeaders(
      RequestMethod method,
      String requestUrl,
      String contentType,
      byte[] body,
      String user,
      String appKey) {
    String path = requestUrl;
    String query = null;
    int queryStart = requestUrl.indexOf('?');
    if (queryStart >= 0) {
      path = requestUrl.substring(0, queryStart);
      query = requestUrl.substring(queryStart + 1);
    }

    String date = GbifAuthUtils.formatDate(Clock.systemUTC());
    String contentMd5 = body != null ? Base64.getEncoder().encodeToString(DigestUtils.md5(body)) : null;

    RequestDataToSign requestDataToSign = new RequestDataToSign();
    requestDataToSign.setMethod(method.name());
    requestDataToSign.setUrl(GbifAuthUtils.getCanonicalizedUrl(path, query));
    requestDataToSign.setContentType(contentType != null ? contentType.toLowerCase() : null);
    requestDataToSign.setContentTypeMd5(contentMd5);
    requestDataToSign.setUser(user);
    requestDataToSign.setDate(date);
    String sign = signingService.buildSignature(requestDataToSign, appKey);

    HttpHeaders result = new HttpHeaders();
    result.add(AUTHORIZATION, GBIF_SCHEME + " " + appKey + ":" + sign);
    result.add(HEADER_GBIF_USER, user);
    result.add(HEADER_GBIF_DATE, date);
    if (contentMd5 != null) {
      result.add(HEADER_CONTENT_MD5, contentMd5);
    }
    if (contentType != null) {
      result.add(CONTENT_TYPE, contentType);
    }
    return result;
  }
}
