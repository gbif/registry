package org.gbif.registry.ws.security;

import org.gbif.ws.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * For requests authenticated with a REGISTRY_EDITOR role two levels of authorization need to be passed.
 * First of all any resource method is required to have the role included in the RolesAllowed annotation.
 * Secondly this request filter needs to be passed for POST/PUT/DELETE requests that act on existing and UUID identified
 * main registry entities such as dataset, organization, node, installation and network.
 * <p>
 * In order to do authorization the key of these entities is extracted from the requested path.
 * An exception to this is the create method for those main entities themselves.
 * This is covered by the BaseNetworkEntityResource.create() method directly.
 */
@Component
public class EditorAuthorizationFilter extends GenericFilterBean {

  private static final Logger LOG = LoggerFactory.getLogger(EditorAuthorizationFilter.class);

  private static final String ENTITY_KEY = "^/?%s/([a-f0-9-]+)";
  private static final Pattern NODE_NETWORK_PATTERN = Pattern.compile(String.format(ENTITY_KEY, "(?:network|node)"));
  private static final Pattern ORGANIZATION_PATTERN = Pattern.compile(String.format(ENTITY_KEY, "organization"));
  private static final Pattern DATASET_PATTERN = Pattern.compile(String.format(ENTITY_KEY, "dataset"));
  private static final Pattern INSTALLATION_PATTERN = Pattern.compile(String.format(ENTITY_KEY, "installation"));

  private final EditorAuthorizationService userAuthService;

  public EditorAuthorizationFilter(EditorAuthorizationService userAuthService) {
    this.userAuthService = userAuthService;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    // only verify non GET methods with an authenticated REGISTRY_EDITOR
    // all other roles are taken care by simple 'Secured' or JSR250 annotations on the resource methods
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    final String name = authentication.getName();
    final HttpServletRequest httpRequest = (HttpServletRequest) request;

    String path = httpRequest.getRequestURI().toLowerCase();

    // user must NOT be null if the resource requires editor rights restrictions
    if (name == null
      && (ORGANIZATION_PATTERN.matcher(path).matches()
      || DATASET_PATTERN.matcher(path).matches()
      || INSTALLATION_PATTERN.matcher(path).matches()
      || NODE_NETWORK_PATTERN.matcher(path).matches())) {
      throw new WebApplicationException(HttpStatus.FORBIDDEN);
    }

    if (name != null
      && (!SecurityContextCheck.checkUserInRole(authentication, UserRoles.ADMIN_ROLE)
      && SecurityContextCheck.checkUserInRole(authentication, UserRoles.EDITOR_ROLE))
      && !httpRequest.getMethod().equals("GET") && !httpRequest.getMethod().equals("OPTIONS")) {

      try {
        Matcher m = ORGANIZATION_PATTERN.matcher(path);
        if (m.find()) {
          final String organization = m.group(1);
          if (!userAuthService.allowedToModifyOrganization(name, UUID.fromString(organization))) {
            LOG.warn("User {} is not allowed to modify organization {}", name, organization);
            throw new WebApplicationException(HttpStatus.FORBIDDEN);
          } else {
            LOG.debug("User {} is allowed to modify organization {}", name, organization);
          }
        }

        m = DATASET_PATTERN.matcher(path);
        if (m.find()) {
          final String dataset = m.group(1);
          if (!userAuthService.allowedToModifyDataset(name, UUID.fromString(dataset))) {
            LOG.warn("User {} is not allowed to modify dataset {}", name, dataset);
            throw new WebApplicationException(HttpStatus.FORBIDDEN);
          } else {
            LOG.debug("User {} is allowed to modify dataset {}", name, dataset);
          }
        }

        m = INSTALLATION_PATTERN.matcher(path);
        if (m.find()) {
          final String installation = m.group(1);
          if (!userAuthService.allowedToModifyInstallation(name, UUID.fromString(installation))) {
            LOG.warn("User {} is not allowed to modify installation {}", name, installation);
            throw new WebApplicationException(HttpStatus.FORBIDDEN);
          } else {
            LOG.debug("User {} is allowed to modify installation {}", name, installation);
          }
        }

        m = NODE_NETWORK_PATTERN.matcher(path);
        if (m.find()) {
          final String nodeOrNetwork = m.group(1);
          if (!userAuthService.allowedToModifyEntity(name, UUID.fromString(nodeOrNetwork))) {
            LOG.warn("User {} is not allowed to modify node/network {}", name, nodeOrNetwork);
            throw new WebApplicationException(HttpStatus.FORBIDDEN);
          } else {
            LOG.debug("User {} is allowed to modify node/network {}", name, nodeOrNetwork);
          }
        }
      } catch (IllegalArgumentException e) {
        // no valid UUID, do nothing as it should not be a valid request anyway
      }
    }
    chain.doFilter(request, response);
  }
}
