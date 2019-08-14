package org.gbif.ws.security;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextProvider {

  public SecurityContext getContext() {
    return SecurityContextHolder.getContext();
  }
}
