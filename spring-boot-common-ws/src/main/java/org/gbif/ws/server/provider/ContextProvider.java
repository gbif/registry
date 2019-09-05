package org.gbif.ws.server.provider;

import org.springframework.web.context.request.WebRequest;

public interface ContextProvider<T> {

  T getValue(WebRequest context);
}
