package org.gbif.registry.ws.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: 2019-07-29 revise, move to another package?
public class HeaderMapRequestWrapper extends HttpServletRequestWrapper {

  /**
   * construct a wrapper for this request
   */
  public HeaderMapRequestWrapper(HttpServletRequest request) {
    super(request);
  }

  private Map<String, String> headerMap = new HashMap<>();

  /**
   * add a header with given name and value
   */
  public void addHeader(String name, String value) {
    headerMap.put(name, value);
  }

  @Override
  public String getHeader(String name) {
    String headerValue = super.getHeader(name);
    if (headerMap.containsKey(name)) {
      headerValue = headerMap.get(name);
    }
    return headerValue;
  }

  /**
   * get the Header names
   */
  @Override
  public Enumeration<String> getHeaderNames() {
    List<String> names = Collections.list(super.getHeaderNames());
    names.addAll(headerMap.keySet());
    return Collections.enumeration(names);
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    List<String> values = Collections.list(super.getHeaders(name));
    if (headerMap.containsKey(name)) {
      values.add(headerMap.get(name));
    }
    return Collections.enumeration(values);
  }
}