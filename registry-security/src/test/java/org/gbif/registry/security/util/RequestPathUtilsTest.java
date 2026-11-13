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
package org.gbif.registry.security.util;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestPathUtilsTest {

  private static final String KEY = "8d453ca7-553f-40d2-8054-82708dc354f6";

  @Test
  public void normalizeTest() {
    assertEquals("/dataset/" + KEY, RequestPathUtils.normalize("/dataset/" + KEY));
    assertEquals("/dataset/" + KEY, RequestPathUtils.normalize("/dataset/" + KEY + "/"));
    assertEquals("/dataset/" + KEY, RequestPathUtils.normalize("/dataset/" + KEY + "//"));
    assertEquals(
        "/dataset/" + KEY, RequestPathUtils.normalize("/dataset/" + KEY.replace("-", "%2D")));
    assertEquals("/dataset/" + KEY, RequestPathUtils.normalize("/dataset;a=b/" + KEY + ";c=d"));
    assertEquals("/dataset/" + KEY + "/crawl", RequestPathUtils.normalize("//dataset/" + KEY + "/crawl/"));
    assertEquals("/", RequestPathUtils.normalize("/"));
    assertEquals("/", RequestPathUtils.normalize(""));
    assertEquals("/", RequestPathUtils.normalize(null));
  }

  @Test
  public void normalizedPathRemovesContextPathTest() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/v1/dataset/" + KEY + "/");
    when(request.getContextPath()).thenReturn("/v1");
    assertEquals("/dataset/" + KEY, RequestPathUtils.normalizedPath(request));
  }
}
