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
        authPreCheckCreationRequestFilter.getResourceToCreate(mockRequest).get());

    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/collection");
    assertEquals(
        GrSciCollEditorAuthorizationFilter.COLLECTION,
        authPreCheckCreationRequestFilter.getResourceToCreate(mockRequest).get());

    when(mockRequest.getRequestURI()).thenReturn("/dataset");
    when(mockRequest.getMethod()).thenReturn("POST");
    assertEquals(
        EditorAuthorizationFilter.DATASET,
        authPreCheckCreationRequestFilter.getResourceToCreate(mockRequest).get());

    when(mockRequest.getRequestURI()).thenReturn("/organization");
    when(mockRequest.getMethod()).thenReturn("POST");
    assertEquals(
        EditorAuthorizationFilter.ORGANIZATION,
        authPreCheckCreationRequestFilter.getResourceToCreate(mockRequest).get());

    when(mockRequest.getRequestURI()).thenReturn("/installation");
    when(mockRequest.getMethod()).thenReturn("POST");
    assertEquals(
        EditorAuthorizationFilter.INSTALLATION,
        authPreCheckCreationRequestFilter.getResourceToCreate(mockRequest).get());
  }

  @Test
  public void createEntityTest() {

  }

}
