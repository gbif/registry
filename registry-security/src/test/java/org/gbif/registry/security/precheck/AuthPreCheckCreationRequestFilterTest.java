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
package org.gbif.registry.security.precheck;

import org.gbif.registry.security.EditorAuthorizationFilter;
import org.gbif.registry.security.grscicoll.GrSciCollEditorAuthorizationFilter;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;

import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/** Tests the {@link org.gbif.registry.security.precheck.AuthPreCheckCreationRequestFilter} */
@ExtendWith(MockitoExtension.class)
public class AuthPreCheckCreationRequestFilterTest {

  @Mock private GbifHttpServletRequestWrapper mockRequest;
  @Mock private HttpServletResponse mockResponse;
  @InjectMocks private AuthPreCheckCreationRequestFilter authPreCheckCreationRequestFilter;

  @Test
  public void getResourceToCreateTest() {
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution");
    assertEquals(
        GrSciCollEditorAuthorizationFilter.INSTITUTION,
        authPreCheckCreationRequestFilter.getResourceToCreate(mockRequest).get().name);

    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/collection");
    assertEquals(
        GrSciCollEditorAuthorizationFilter.COLLECTION,
        authPreCheckCreationRequestFilter.getResourceToCreate(mockRequest).get().name);

    when(mockRequest.getRequestURI()).thenReturn("/dataset");
    when(mockRequest.getMethod()).thenReturn("POST");
    assertEquals(
        EditorAuthorizationFilter.DATASET,
        authPreCheckCreationRequestFilter.getResourceToCreate(mockRequest).get().name);

    when(mockRequest.getRequestURI()).thenReturn("/organization");
    when(mockRequest.getMethod()).thenReturn("POST");
    assertEquals(
        EditorAuthorizationFilter.ORGANIZATION,
        authPreCheckCreationRequestFilter.getResourceToCreate(mockRequest).get().name);

    when(mockRequest.getRequestURI()).thenReturn("/installation");
    when(mockRequest.getMethod()).thenReturn("POST");
    assertEquals(
        EditorAuthorizationFilter.INSTALLATION,
        authPreCheckCreationRequestFilter.getResourceToCreate(mockRequest).get().name);

    when(mockRequest.getRequestURI())
        .thenReturn("/dataset/f822c473-fea5-467c-93ca-3f09befd9817/machineTag");
    assertEquals(
        EditorAuthorizationFilter.MACHINE_TAG,
        authPreCheckCreationRequestFilter.getResourceToCreate(mockRequest).get().name);

    when(mockRequest.getRequestURI())
        .thenReturn("/grscicoll/institution/f822c473-fea5-467c-93ca-3f09befd9817/merge");
    assertEquals(
        AuthPreCheckCreationRequestFilter.INSTITUTION_MERGE,
        authPreCheckCreationRequestFilter.getResourceToCreate(mockRequest).get().name);

    when(mockRequest.getRequestURI())
        .thenReturn("/grscicoll/collection/f822c473-fea5-467c-93ca-3f09befd9817/merge");
    assertEquals(
        AuthPreCheckCreationRequestFilter.COLLECTION_MERGE,
        authPreCheckCreationRequestFilter.getResourceToCreate(mockRequest).get().name);
  }
}
