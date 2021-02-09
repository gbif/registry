package org.gbif.registry.ws.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.security.AuthenticationFacade;
import org.gbif.registry.security.EditorAuthorizationFilter;
import org.gbif.registry.security.EditorAuthorizationService;
import org.gbif.registry.security.UserRoles;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reflections.Reflections;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SecuredEditorAnnotatedMethodsTest {

  private static final UUID KEY = UUID.randomUUID();
  private static final String CONTENT = "{\"key\": \"" + KEY + "\"}";
  private static final Organization ORG = new Organization();
  private static final Dataset DATASET = new Dataset();
  private static final Installation INSTALLATION = new Installation();
  private static final String SUB_KEY = "123";
  private static final UUID SUB_DATASET_KEY = UUID.randomUUID();
  private static final String USERNAME = "user";
  private static final Pattern RESOURCE_WITHOUT_KEY = Pattern.compile("^/[A-Za-z-_]+$");
  private static final List<GrantedAuthority> ROLES_EDITOR_ONLY =
      Collections.singletonList(new SimpleGrantedAuthority(UserRoles.EDITOR_ROLE));
  private static final List<String> ALWAYS_FAILING_REQUESTS =
      Arrays.asList("POST /node", "POST /network", "PUT /node", "PUT /network");
  public static final String SPACE = " ";

  @Mock
  private GbifHttpServletRequestWrapper mockRequest;
  @Mock private HttpServletResponse mockResponse;
  @Mock private FilterChain mockFilterChain;
  @Mock private AuthenticationFacade mockAuthenticationFacade;
  @Mock private EditorAuthorizationService mockEditorAuthService;
  @Mock private Authentication mockAuthentication;
  @Spy
  private final ObjectMapper objectMapper = new ObjectMapper();
  @InjectMocks
  private EditorAuthorizationFilter filter;

  // Finds all methods annotated by Secured(EDITOR_ROLE) by reflection
  public static Stream<String> testData() {
    Set<String> data = new HashSet<>();
    // scan package
    Reflections reflections = new Reflections("org.gbif.registry");

    // get all rest controllers of the package
    Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(RestController.class);

    for (Class<?> clazz : typesAnnotatedWith) {
      Method[] methods = clazz.getMethods();
      for (Method method : methods) {
        Secured secured = method.getAnnotation(Secured.class);
        // only deal with methods with Secured annotation
        if (secured != null) {
          boolean containsEditor = Arrays.asList(secured.value()).contains(UserRoles.EDITOR_ROLE);
          // only deal with Secured(EDITOR_ROLE)
          if (containsEditor) {
            // extract class' RequestMapping to construct a full path
            RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);

            // compute all possible paths
            // note that class and method can have several paths
            // e.g. @RequestMapping(value = {"", "{key})
            Set<String> fullPaths = Arrays.stream(requestMapping.value())
                .map(SecuredEditorAnnotatedMethodsTest::prependWithSlash)
                .flatMap(item -> Arrays.stream(getMethodPaths(method)).map(SecuredEditorAnnotatedMethodsTest::prependWithSlash).map(item::concat))
                .collect(Collectors.toSet());

            // get method type (POST, PUT etc.)
            RequestMethod methodType = getMethodType(method);

            // concat method and full path
            // e.g PUT /dataset/{key}
            fullPaths.forEach(path -> data.add(methodType.toString() + " " + path));
          }
        }
      }
    }

    return data.stream();
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("testData")
  public void testEditorAuthFail(String requestTemplate) throws Exception {
    // GIVEN
    String path = replaceVariables(requestTemplate);
    String[] requestItems = path.split(SPACE);
    String methodType = requestItems[0];
    String requestPath = requestItems[1];
    boolean isResourceWithoutKey = RESOURCE_WITHOUT_KEY.matcher(requestPath).matches();
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn(replaceVariables(requestPath));
    when(mockRequest.getMethod()).thenReturn(methodType);
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    mockSpecific(requestPath, isResourceWithoutKey, false);

    // WHEN & THEN
    WebApplicationException exception = assertThrows(
        WebApplicationException.class,
        () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
    assertTrue(exception.getMessage().contains("not allowed"));
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest).getRequestURI();
    verify(mockRequest, atLeast(2)).getMethod();
    verify(mockAuthentication, atLeastOnce()).getName();
    verify(mockAuthentication, atLeast(2)).getAuthorities();
    verifySpecific(requestPath, isResourceWithoutKey);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("testData")
  public void testEditorAuthSuccess(String requestTemplate) throws Exception {
    // GIVEN
    boolean isFailingRequest = ALWAYS_FAILING_REQUESTS.contains(requestTemplate);
    String path = replaceVariables(requestTemplate);
    String[] requestItems = path.split(SPACE);
    String methodType = requestItems[0];
    String requestPath = requestItems[1];
    boolean isResourceWithoutKey = RESOURCE_WITHOUT_KEY.matcher(requestPath).matches();
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn(replaceVariables(requestPath));
    when(mockRequest.getMethod()).thenReturn(methodType);
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    mockSpecific(requestPath, isResourceWithoutKey, true);

    // WHEN & THEN
    if (isFailingRequest) {
      assertThrows(WebApplicationException.class, () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
    } else {
      filter.doFilter(mockRequest, mockResponse, mockFilterChain);
    }
    verify(mockAuthenticationFacade).getAuthentication();
    verify(mockRequest).getRequestURI();
    verify(mockRequest, atLeast(2)).getMethod();
    verify(mockAuthentication, atLeastOnce()).getName();
    verify(mockAuthentication, atLeast(2)).getAuthorities();
    verifySpecific(requestPath, isResourceWithoutKey);
  }

  // mock request specific things
  private void mockSpecific(String requestPath, boolean isResourceWithoutKey, boolean isAllowedToModify) throws Exception {
    String resourceName = requestPath.split("/")[1];
    switch (resourceName) {
      case "organization":
        if (isResourceWithoutKey) {
          when(mockEditorAuthService.allowedToModifyOrganization(USERNAME, ORG)).thenReturn(isAllowedToModify);
          when(objectMapper.readValue(CONTENT, Organization.class)).thenReturn(ORG);
          when(mockRequest.getContent()).thenReturn(CONTENT);
        } else {
          when(mockEditorAuthService.allowedToModifyOrganization(USERNAME, KEY)).thenReturn(isAllowedToModify);
        }
        break;
      case "dataset":
        if (isResourceWithoutKey) {
          when(mockEditorAuthService.allowedToModifyDataset(USERNAME, DATASET)).thenReturn(isAllowedToModify);
          when(objectMapper.readValue(CONTENT, Dataset.class)).thenReturn(DATASET);
          when(mockRequest.getContent()).thenReturn(CONTENT);
        } else {
          when(mockEditorAuthService.allowedToModifyDataset(USERNAME, KEY)).thenReturn(isAllowedToModify);
        }
        break;
      case "installation":
        if (isResourceWithoutKey) {
          when(mockEditorAuthService.allowedToModifyInstallation(USERNAME, INSTALLATION)).thenReturn(isAllowedToModify);
          when(objectMapper.readValue(CONTENT, Installation.class)).thenReturn(INSTALLATION);
          when(mockRequest.getContent()).thenReturn(CONTENT);
        } else {
          when(mockEditorAuthService.allowedToModifyInstallation(USERNAME, KEY)).thenReturn(isAllowedToModify);
        }
        break;
      case "network":
      case "node":
        if (!isResourceWithoutKey) {
          when(mockEditorAuthService.allowedToModifyEntity(USERNAME, KEY)).thenReturn(isAllowedToModify);
        }
        break;
      case "pipelines":
        when(mockEditorAuthService.allowedToModifyDataset(USERNAME, SUB_DATASET_KEY)).thenReturn(isAllowedToModify);
        break;
      default:
        throw new IllegalStateException("mock specific for " + resourceName + " not implemented");
    }
  }

  // verify request specific things
  private void verifySpecific(String requestPath, boolean isResourceWithoutKey) {
    String resourceName = requestPath.split("/")[1];
    switch (resourceName) {
      case "organization":
        if (isResourceWithoutKey) {
          verify(mockEditorAuthService).allowedToModifyOrganization(USERNAME, ORG);
          verify(mockRequest).getContent();
        } else {
          verify(mockEditorAuthService).allowedToModifyOrganization(USERNAME, KEY);
        }
        break;
      case "dataset":
        if (isResourceWithoutKey) {
          verify(mockEditorAuthService).allowedToModifyDataset(USERNAME, DATASET);
          verify(mockRequest).getContent();
        } else {
          verify(mockEditorAuthService).allowedToModifyDataset(USERNAME, KEY);
        }
        break;
      case "installation":
        if (isResourceWithoutKey) {
          verify(mockEditorAuthService).allowedToModifyInstallation(USERNAME, INSTALLATION);
          verify(mockRequest).getContent();
        } else {
          verify(mockEditorAuthService).allowedToModifyInstallation(USERNAME, KEY);
        }
        break;
      case "node":
      case "network":
        if (!isResourceWithoutKey) {
          verify(mockEditorAuthService).allowedToModifyEntity(USERNAME, KEY);
        }
        break;
      case "pipelines":
        verify(mockEditorAuthService).allowedToModifyDataset(USERNAME, SUB_DATASET_KEY);
        break;
      default:
        throw new IllegalStateException("verify specific for " + resourceName + " not implemented");
    }
  }

  // replace path variables in the template with actual values
  private String replaceVariables(String path) {
    return path.replace("{key}", KEY.toString())
        .replace("{identifierKey}", SUB_KEY)
        .replace("{endpointKey}", SUB_KEY)
        .replace("{commentKey}", SUB_KEY)
        .replace("{contactKey}", SUB_KEY)
        .replace("{tagKey}", SUB_KEY)
        .replace("{datasetKey}", SUB_DATASET_KEY.toString())
        .replace("{processKey}", SUB_KEY)
        .replace("{executionKey}", SUB_KEY)
        .replace("{attempt}", SUB_KEY);
  }

  private static String prependWithSlash(String path) {
    if (StringUtils.isNotEmpty(path)) {
      return path.startsWith("/") ? path : "/" + path;
    }
    return path;
  }

  private static RequestMethod getMethodType(Method method) {
    Object annotation = method.getAnnotation(GetMapping.class);
    if (annotation != null) {
      return RequestMethod.GET;
    }

    annotation = method.getAnnotation(PostMapping.class);
    if (annotation != null) {
      return RequestMethod.POST;
    }

    annotation = method.getAnnotation(PutMapping.class);
    if (annotation != null) {
      return RequestMethod.PUT;
    }

    annotation = method.getAnnotation(DeleteMapping.class);
    if (annotation != null) {
      return RequestMethod.DELETE;
    }

    throw new IllegalStateException("There is no Mapping annotation!");
  }

  private static String[] getMethodPaths(Method method) {
    String[] result;
    Object annotation = method.getAnnotation(GetMapping.class);
    if (annotation != null) {
      result = ((GetMapping) annotation).value();
      return result.length == 0 ? new String[]{""} : result;
    }

    annotation = method.getAnnotation(PostMapping.class);
    if (annotation != null) {
      result = ((PostMapping) annotation).value();
      return result.length == 0 ? new String[]{""} : result;
    }

    annotation = method.getAnnotation(PutMapping.class);
    if (annotation != null) {
      result = ((PutMapping) annotation).value();
      return result.length == 0 ? new String[]{""} : result;
    }

    annotation = method.getAnnotation(DeleteMapping.class);
    if (annotation != null) {
      result = ((DeleteMapping) annotation).value();
      return result.length == 0 ? new String[]{""} : result;
    }

    throw new IllegalStateException("There is no Mapping annotation!");
  }
}
