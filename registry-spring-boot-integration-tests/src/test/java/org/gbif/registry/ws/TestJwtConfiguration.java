package org.gbif.registry.ws;

import org.gbif.registry.ws.security.jwt.JwtIssuanceService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.spy;

@TestConfiguration
public class TestJwtConfiguration {

  @Bean
  @Qualifier("jwtIssuanceServiceSpy")
  public JwtIssuanceService jwtServiceSpy(JwtIssuanceService jwtIssuanceService) {
    return spy(jwtIssuanceService);
  }
}
