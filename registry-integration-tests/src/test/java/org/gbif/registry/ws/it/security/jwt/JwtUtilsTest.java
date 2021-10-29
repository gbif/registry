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
package org.gbif.registry.ws.it.security.jwt;

import org.gbif.registry.security.jwt.JwtConfiguration;
import org.gbif.registry.ws.jwt.JwtUtils;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequestWrapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import com.google.common.hash.Hashing;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtUtilsTest {

  @Test
  public void generateTokenTest() {
    String key = generateTestSigningKey("test");
    JwtConfiguration config = new JwtConfiguration();
    config.setSigningKey(key);
    config.setExpiryTimeInMs(60 * 1000L);
    config.setIssuer("issuer");

    String token = JwtUtils.generateJwt("user", config);

    Claims claims =
        Jwts.parser()
            .requireIssuer(config.getIssuer())
            .setSigningKey(config.getSigningKey())
            .parseClaimsJws(token)
            .getBody();

    assertEquals("user", claims.get("userName"));
    assertEquals(config.getIssuer(), claims.getIssuer());
  }

  @Test
  public void generateUnsignedTokenTest() {
    JwtConfiguration config = new JwtConfiguration();
    config.setExpiryTimeInMs(60 * 1000L);
    config.setIssuer("issuer");

    assertThrows(IllegalArgumentException.class, () -> JwtUtils.generateJwt("user", config));
  }

  @Test
  public void findTokenInRequestTest() {
    final String token = "abctoken";
    // mock request
    HttpServletRequestWrapper request = mock(HttpServletRequestWrapper.class);

    // no token present in request
    assertFalse(JwtUtils.findTokenInRequest(request).isPresent());

    // token in header
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
    assertEquals(token, JwtUtils.findTokenInRequest(request).get());

    // empty bearer
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer");
    assertEquals("", JwtUtils.findTokenInRequest(request).get());

    // empty header
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("");
    assertFalse(JwtUtils.findTokenInRequest(request).isPresent());

    // null header
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
    assertFalse(JwtUtils.findTokenInRequest(request).isPresent());
  }

  @SuppressWarnings("UnstableApiUsage")
  private String generateTestSigningKey(String string) {
    return Hashing.sha256().hashString(string, StandardCharsets.UTF_8).toString();
  }

  @Test
  public void generateTokenTestRegistry() {

    JwtConfiguration config = new JwtConfiguration();
    config.setSigningKey("ec9f0b6949e2d229828179ceddf5257429c2ca05e3426836f30fef836788067a");
    config.setExpiryTimeInMs(60000 * 1000L);
    config.setIssuer("GBIF-REGISTRY");

    String token = JwtUtils.generateJwt("fmendez", config);

    System.out.println(token);
  }
}
