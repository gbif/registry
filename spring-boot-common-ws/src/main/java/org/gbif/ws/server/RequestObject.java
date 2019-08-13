package org.gbif.ws.server;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;

public class RequestObject extends HttpServletRequestWrapper {

  private String content;

  private HttpHeaders httpHeaders;

  public RequestObject(HttpServletRequest request) {
    super(request);

    try {
      if (request.getInputStream() != null) {
        content = IOUtils.toString(request.getInputStream(), request.getCharacterEncoding());
      } else {
        content = null;
      }
    } catch (IOException e) {
      throw new RuntimeException("Stream can't be read", e);
    }

    httpHeaders = getHttpHeaders(request);
  }

  @Override
  public ServletInputStream getInputStream() {
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content.getBytes());
    return new DelegatingServletInputStream(byteArrayInputStream);
  }

  @Override
  public BufferedReader getReader() {
    return new BufferedReader(new InputStreamReader(this.getInputStream()));
  }

  private HttpHeaders getHttpHeaders(HttpServletRequest request) {
    final HttpHeaders requestHeaders = new HttpHeaders();
    Enumeration<String> headerNames = request.getHeaderNames();

    if (headerNames != null) {
      while (headerNames.hasMoreElements()) {
        String currentHeaderName = headerNames.nextElement();
        requestHeaders.add(currentHeaderName, request.getHeader(currentHeaderName));
      }
    }

    return requestHeaders;
  }

  public String getContent() {
    return content;
  }

  public HttpHeaders getHttpHeaders() {
    return httpHeaders;
  }
}
