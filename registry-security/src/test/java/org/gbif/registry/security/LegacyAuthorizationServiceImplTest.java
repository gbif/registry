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
package org.gbif.registry.security;

import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.security.LegacyRequestAuthorization;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LegacyAuthorizationServiceImplTest {

  private static final UUID ORG_KEY = UUID.randomUUID();
  private static final UUID INSTALLATION_KEY = UUID.randomUUID();
  private static final String ORG_PASSWORD = "orgSecret";
  private static final String INSTALLATION_PASSWORD = "installationSecret";

  @Mock private OrganizationMapper organizationMapper;
  @Mock private DatasetMapper datasetMapper;
  @Mock private InstallationMapper installationMapper;
  @Mock private HttpServletRequest request;

  private LegacyAuthorizationServiceImpl service;

  @BeforeEach
  public void setUp() {
    service =
        new LegacyAuthorizationServiceImpl(organizationMapper, datasetMapper, installationMapper);

    Organization organization = new Organization();
    organization.setKey(ORG_KEY);
    organization.setPassword(ORG_PASSWORD);
    when(organizationMapper.get(ORG_KEY)).thenReturn(organization);

    Installation installation = new Installation();
    installation.setKey(INSTALLATION_KEY);
    installation.setPassword(INSTALLATION_PASSWORD);
    when(installationMapper.get(INSTALLATION_KEY)).thenReturn(installation);

    when(request.getParameterMap()).thenReturn(Collections.emptyMap());
  }

  @Test
  public void validCredentialsTest() {
    basicAuth(ORG_KEY + ":" + ORG_PASSWORD);
    LegacyRequestAuthorization authorization = service.authenticate(request);
    assertNotNull(authorization);
    assertEquals(ORG_KEY, authorization.getUserKey());

    basicAuth("IPT__" + INSTALLATION_KEY + ":" + INSTALLATION_PASSWORD);
    authorization = service.authenticate(request);
    assertNotNull(authorization);
    assertEquals(INSTALLATION_KEY, authorization.getUserKey());
  }

  /** Wrong password, unknown key and non-UUID user give the same answer, without credentials. */
  @Test
  public void invalidCredentialsAreIndistinguishableTest() {
    String nonUuidPassword = "myPersonalPassword";
    String[] credentials = {
      ORG_KEY + ":wrong",
      INSTALLATION_KEY + ":wrong",
      UUID.randomUUID() + ":" + ORG_PASSWORD,
      "jane:" + nonUuidPassword
    };

    for (String credential : credentials) {
      basicAuth(credential);
      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> service.authenticate(request));
      assertEquals(HttpStatus.UNAUTHORIZED.value(), ex.getStatus());
      assertEquals("Invalid credentials", ex.getMessage());
      assertFalse(String.valueOf(ex.getMessage()).contains(nonUuidPassword));
    }
  }

  @Test
  public void invalidBase64Test() {
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic %%%not-base64");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.authenticate(request));
    assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatus());
  }

  @Test
  public void missingPasswordTest() {
    basicAuth(ORG_KEY.toString());
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> service.authenticate(request));
    assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatus());
  }

  private void basicAuth(String credentials) {
    when(request.getHeader(HttpHeaders.AUTHORIZATION))
        .thenReturn(
            "Basic "
                + Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
  }
}
