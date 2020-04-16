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
package org.gbif.registry.ws.it.fixtures;

import org.gbif.ws.security.Md5EncodeService;
import org.gbif.ws.security.RequestDataToSign;
import org.gbif.ws.security.SigningService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;
import javax.xml.transform.stream.StreamSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.gbif.registry.ws.it.fixtures.TestConstants.IT_APP_KEY;
import static org.gbif.ws.util.SecurityConstants.GBIF_SCHEME;
import static org.gbif.ws.util.SecurityConstants.HEADER_CONTENT_MD5;
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
  private Md5EncodeService md5EncodeService;
  private ObjectMapper objectMapper;
  private Jaxb2Marshaller marshaller;

  @Autowired
  public RequestTestFixture(
      MockMvc mvc,
      SigningService signingService,
      Md5EncodeService md5EncodeService,
      @Qualifier("registryObjectMapper") ObjectMapper objectMapper,
      Jaxb2Marshaller marshaller) {
    this.mvc = mvc;
    this.signingService = signingService;
    this.md5EncodeService = md5EncodeService;
    this.objectMapper = objectMapper;
    this.marshaller = marshaller;
  }

  public ResultActions getRequest(String path) throws Exception {
    return mvc.perform(get(path));
  }

  public ResultActions getRequest(String username, String password, String path) throws Exception {
    return mvc.perform(get(path).with(httpBasic(username, password)));
  }

  public ResultActions postRequest(String path) throws Exception {
    return mvc.perform(post(path));
  }

  public ResultActions postRequest(String username, String password, String path) throws Exception {
    return mvc.perform(post(path).with(httpBasic(username, password)));
  }

  public ResultActions postRequest(String username, String password, Object entity, String path)
      throws Exception {
    return mvc.perform(
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
    return mvc.perform(
        post(path)
            .params(params)
            .contentType(APPLICATION_FORM_URLENCODED)
            .accept(responseType)
            .with(httpBasic(username.toString(), password)));
  }

  public ResultActions deleteRequestUrlEncoded(Object username, String password, String path)
      throws Exception {
    return mvc.perform(
        delete(path)
            .contentType(APPLICATION_FORM_URLENCODED)
            .with(httpBasic(username.toString(), password)));
  }

  public ResultActions putRequest(String username, String password, Object entity, String path)
      throws Exception {
    return mvc.perform(
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

    return mvc.perform(
        put(path).content(content).contentType(APPLICATION_JSON).headers(authHeaders));
  }

  public ResultActions postSignedRequest(String username, String path) throws Exception {
    HttpHeaders authHeaders =
        prepareGbifAuthorizationHeadersNoContent(POST, path, username, IT_APP_KEY);

    return mvc.perform(post(path).headers(authHeaders));
  }

  public ResultActions postSignedRequest(String username, Object entity, String path)
      throws Exception {
    String content = objectMapper.writeValueAsString(entity);

    HttpHeaders authHeaders =
        prepareGbifAuthorizationHeadersWithContent(
            POST, path, APPLICATION_JSON_UTF8, content, username, IT_APP_KEY);

    return mvc.perform(
        post(path).content(content).contentType(APPLICATION_JSON).headers(authHeaders));
  }

  public ResultActions postSignedRequestPlainText(String username, Object entity, String path)
      throws Exception {
    HttpHeaders authHeaders =
        prepareGbifAuthorizationHeadersWithContentAsPlainText(
            POST, path, "text/plain;charset=UTF-8", entity, username, IT_APP_KEY);

    return mvc.perform(
        post(path).content(entity.toString()).contentType(TEXT_PLAIN).headers(authHeaders));
  }

  public ResultActions postSignedRequest(String username, String appKey, Object entity, String path)
      throws Exception {
    String content = objectMapper.writeValueAsString(entity);

    HttpHeaders authHeaders =
        prepareGbifAuthorizationHeadersWithContent(
            POST, path, APPLICATION_JSON_UTF8, content, username, appKey);

    return mvc.perform(
        post(path).content(content).contentType(APPLICATION_JSON).headers(authHeaders));
  }

  public ResultActions getSignedRequest(String username, String path) throws Exception {
    HttpHeaders authHeaders =
        prepareGbifAuthorizationHeadersNoContent(GET, path, username, IT_APP_KEY);

    return mvc.perform(get(path).headers(authHeaders));
  }

  public ResultActions getSignedRequest(String username, String path, Map<String, String> params)
      throws Exception {
    HttpHeaders authHeaders =
        prepareGbifAuthorizationHeadersNoContent(GET, path, username, IT_APP_KEY);

    Map<String, List<String>> queryParams =
        params.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Entry::getKey, entry -> Collections.singletonList(entry.getValue())));

    return mvc.perform(
        get(path).queryParams(new LinkedMultiValueMap<>(queryParams)).headers(authHeaders));
  }

  public ResultActions deleteSignedRequest(String username, String path) throws Exception {
    HttpHeaders authHeaders =
        prepareGbifAuthorizationHeadersNoContent(DELETE, path, username, IT_APP_KEY);

    return mvc.perform(delete(path).headers(authHeaders));
  }

  public String extractResponse(ResultActions actions) throws Exception {
    return actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
  }

  public <T> T extractJsonResponse(ResultActions actions, Class<T> entityClass) throws Exception {
    String content = actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    return objectMapper.readValue(content, entityClass);
  }

  public <T> T extractXmlResponse(ResultActions actions, Class<T> entityClass) throws Exception {
    byte[] content = actions.andReturn().getResponse().getContentAsByteArray();
    JAXBElement<T> jaxbElement =
        marshaller
            .createUnmarshaller()
            .unmarshal(new StreamSource(new ByteArrayInputStream(content)), entityClass);

    return jaxbElement.getValue();
  }

  public HttpHeaders prepareGbifAuthorizationHeadersWithContent(
      RequestMethod method,
      String requestUrl,
      MediaType contentType,
      Object data,
      String user,
      String appKey)
      throws Exception {
    HttpHeaders result = new HttpHeaders();

    String contentAsString = objectMapper.writeValueAsString(data);
    String contentMd5 = md5EncodeService.encode(contentAsString);
    RequestDataToSign requestDataToSign =
        buildStringToSign(method.name(), requestUrl, contentType.toString(), contentMd5, user);
    String sign = signingService.buildSignature(requestDataToSign, appKey);

    result.add(AUTHORIZATION, GBIF_SCHEME + " " + appKey + ":" + sign);
    result.add(HEADER_CONTENT_MD5, contentMd5);
    result.add(HEADER_GBIF_USER, user);
    result.add(CONTENT_TYPE, APPLICATION_JSON_VALUE);

    return result;
  }

  public HttpHeaders prepareGbifAuthorizationHeadersWithContentAsPlainText(
      RequestMethod method,
      String requestUrl,
      String contentType,
      Object data,
      String user,
      String appKey) {
    HttpHeaders result = new HttpHeaders();

    String contentAsString = data.toString();
    String contentMd5 = md5EncodeService.encode(contentAsString);
    RequestDataToSign requestDataToSign =
        buildStringToSign(method.name(), requestUrl, contentType.toString(), contentMd5, user);
    String sign = signingService.buildSignature(requestDataToSign, appKey);

    result.add(AUTHORIZATION, GBIF_SCHEME + " " + appKey + ":" + sign);
    result.add(HEADER_CONTENT_MD5, contentMd5);
    result.add(HEADER_GBIF_USER, user);
    result.add(CONTENT_TYPE, TEXT_PLAIN_VALUE);

    return result;
  }

  public HttpHeaders prepareGbifAuthorizationHeadersNoContent(
      RequestMethod method, String requestUrl, String user, String appKey) {
    HttpHeaders result = new HttpHeaders();

    RequestDataToSign requestDataToSign =
        buildStringToSign(method.name(), requestUrl, null, null, user);
    String sign = signingService.buildSignature(requestDataToSign, appKey);

    result.add(AUTHORIZATION, GBIF_SCHEME + " " + appKey + ":" + sign);
    result.add(HEADER_GBIF_USER, user);

    return result;
  }

  public RequestDataToSign buildStringToSign(
      String method, String requestUrl, String contentType, String contentMd5, String user) {
    RequestDataToSign requestDataToSign = new RequestDataToSign();

    requestDataToSign.setMethod(method);
    requestDataToSign.setUrl(requestUrl);
    requestDataToSign.setUser(user);
    requestDataToSign.setContentType(contentType);
    requestDataToSign.setContentTypeMd5(contentMd5);

    return requestDataToSign;
  }
}
