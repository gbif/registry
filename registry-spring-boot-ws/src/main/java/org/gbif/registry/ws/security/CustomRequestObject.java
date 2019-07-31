package org.gbif.registry.ws.security;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

// TODO: 2019-07-31 rename and move
public class CustomRequestObject {

  /**
   * RequestMethod (e.g. GET, POST, PUT).
   */
  @NotNull
  private final RequestMethod method;

  /**
   * Request URI (e.g. /user/login).
   */
  @NotNull
  private final String requestUri;

  /**
   * Request content.
   */
  @Nullable
  private final String content;

  /**
   * Request HTTP headers.
   */
  @NotNull
  private final HttpHeaders headers;

  public CustomRequestObject(RequestMethod method, String requestUri, @Nullable String content, HttpHeaders headers) {
    this.method = method;
    this.requestUri = requestUri;
    this.content = content;
    this.headers = headers;
  }

  public RequestMethod getMethod() {
    return method;
  }

  public String getMethodValue() {
    return method.name();
  }

  public String getRequestUri() {
    return requestUri;
  }

  @Nullable
  public String getContent() {
    return content;
  }

  public HttpHeaders getHeaders() {
    return headers;
  }

  public void addHeader(String name, String value) {
    headers.add(name, value);
  }
}
