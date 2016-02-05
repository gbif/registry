package org.gbif.registry.ws.security;

import java.security.Principal;
import java.util.UUID;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.SecurityContext;

import com.sun.jersey.spi.container.ContainerRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EditorAuthorizationFilterTest {

  private final String userWithRights = "with";

  @Mock
  SecurityContext secContext;
  @Mock
  EditorAuthorizationService authService;
  @Mock
  ContainerRequest mockRequest;
  private EditorAuthorizationFilter filter;

  @Before
  public void setupMocks() throws Exception {
    // setup filter with mocks
    filter = new EditorAuthorizationFilter(authService);
    filter.setSecContext(secContext);
    when(secContext.isUserInRole(not(eq(UserRoles.EDITOR_ROLE)))).thenReturn(false);
    when(secContext.isUserInRole(eq(UserRoles.EDITOR_ROLE))).thenReturn(true);

    // setup mocks to authorize only based on user
    when(authService.allowedToModifyEntity(Matchers.<Principal>any(), Matchers.<UUID>any())).thenAnswer(
      new Answer<Boolean>() {
        @Override
        public Boolean answer(InvocationOnMock invocation) throws Throwable {
          Object[] args = invocation.getArguments();
          return ((Principal) args[0]).getName().equals(userWithRights);
        }
      });
    when(authService.allowedToModifyInstallation(Matchers.<Principal>any(), Matchers.<UUID>any())).thenAnswer(new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return ((Principal) args[0]).getName().equals(userWithRights);
      }
    });
    when(authService.allowedToModifyOrganization(Matchers.<Principal>any(), Matchers.<UUID>any())).thenAnswer(new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return ((Principal) args[0]).getName().equals(userWithRights);
      }
    });
    when(authService.allowedToModifyDataset(Matchers.<Principal>any(), Matchers.<UUID>any())).thenAnswer(new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return ((Principal) args[0]).getName().equals(userWithRights);
      }
    });
  }

  @Test
  public void testFilterGET() throws Exception {
    setRequestUser(userWithRights);
    // GETs dont need auth
    mockRequest("GET", "dataset");
    filter.filter(mockRequest);
  }

  @Test
  public void testFilterPOSTgood() throws Exception {
    setRequestUser(userWithRights);
    mockRequest("POST", "dataset");
    filter.filter(mockRequest);
  }

  @Test
  public void testFilterPOSTgoodCreate() throws Exception {
    // this is allowed as the filter does not handle paths without a UUID such as the create ones
    setRequestUser("erfunden");
    mockRequest("POST", "dataset");
    filter.filter(mockRequest);
  }

  @Test(expected = WebApplicationException.class)
  public void testFilterPOSTbad() throws Exception {
    setRequestUser("erfunden");
    mockRequest("POST", "dataset/"+ UUID.randomUUID().toString()+"/endpoint");
    filter.filter(mockRequest);
  }

  @Test
  public void testFilterKeyInPathGood() throws Exception {
    setRequestUser(userWithRights);
    mockRequest("DELETE", "dataset/"+ UUID.randomUUID().toString());
    filter.filter(mockRequest);
  }

  @Test(expected = WebApplicationException.class)
  public void testFilterKeyInPathBad() throws Exception {
    setRequestUser("erfunden");
    mockRequest("DELETE", "dataset/"+ UUID.randomUUID().toString());
    filter.filter(mockRequest);
  }


  private void mockRequest(String method, String path) {
    when(mockRequest.getPath()).thenReturn(path);
    when(mockRequest.getMethod()).thenReturn(method.toUpperCase().trim());
  }

  private void setRequestUser(String user) {
    when(secContext.getUserPrincipal()).thenReturn(principal(user));
  }
  private Principal principal(final String user){
    return new Principal() {
      @Override
      public String getName() {
        return user;
      }
    };
  }


}
