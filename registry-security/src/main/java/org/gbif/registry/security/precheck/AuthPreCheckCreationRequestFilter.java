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

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.merge.MergeParams;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.NetworkMapper;
import org.gbif.registry.persistence.mapper.NodeMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.UserRightsMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.security.AuthenticationFacade;
import org.gbif.registry.security.grscicoll.GrSciCollEditorAuthorizationFilter;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;

import static org.gbif.registry.security.EditorAuthorizationFilter.DATASET;
import static org.gbif.registry.security.EditorAuthorizationFilter.INSTALLATION;
import static org.gbif.registry.security.EditorAuthorizationFilter.MACHINE_TAG;
import static org.gbif.registry.security.EditorAuthorizationFilter.NETWORK;
import static org.gbif.registry.security.EditorAuthorizationFilter.NODE;
import static org.gbif.registry.security.EditorAuthorizationFilter.ORGANIZATION;
import static org.gbif.registry.security.EditorAuthorizationFilter.POST_RESOURCES_TO_FILTER;
import static org.gbif.registry.security.grscicoll.GrSciCollEditorAuthorizationFilter.COLLECTION;
import static org.gbif.registry.security.grscicoll.GrSciCollEditorAuthorizationFilter.INSTITUTION;
import static org.gbif.registry.security.grscicoll.GrSciCollEditorAuthorizationFilter.INST_COLL_CREATE_PATTERN;
import static org.gbif.registry.security.grscicoll.GrSciCollEditorAuthorizationFilter.MERGE_PATTERN;
import static org.gbif.registry.security.precheck.AuthPreCheckInterceptor.containsCheckPermissionsOnlyParam;

/**
 * When there is a request to create a resource that contains the query param {{@link
 * AuthPreCheckInterceptor#CHECK_PERMISSIONS_ONLY_PARAM}} as <strong>true</strong> and the body is
 * empty, this filter creates a body with the keys that the user is scoped for. This tries to mock
 * the scenario where the user would have the most amount of permissions in order to determine if an
 * specific action can be performed. In a real request, if the user doesn't use their scoped keys
 * its requests will still be rejected.
 *
 * <p>This filter must be triggered before {@link
 * org.gbif.registry.security.EditorAuthorizationFilter} and {@link
 * GrSciCollEditorAuthorizationFilter}.
 */
@Component
public class AuthPreCheckCreationRequestFilter extends OncePerRequestFilter {

  private static final Logger LOG =
      LoggerFactory.getLogger(AuthPreCheckCreationRequestFilter.class);

  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[\\h\\s+]");
  static final String INSTITUTION_MERGE = "institutionMerge";
  static final String COLLECTION_MERGE = "collectionMerge";

  private final UserRightsMapper userRightsMapper;
  private final OrganizationMapper organizationMapper;
  private final DatasetMapper datasetMapper;
  private final InstallationMapper installationMapper;
  private final NodeMapper nodeMapper;
  private final NetworkMapper networkMapper;
  private final InstitutionMapper institutionMapper;
  private final CollectionMapper collectionMapper;
  private final ObjectMapper objectMapper;
  private final AuthenticationFacade authenticationFacade;

  public AuthPreCheckCreationRequestFilter(
      UserRightsMapper userRightsMapper,
      OrganizationMapper organizationMapper,
      DatasetMapper datasetMapper,
      InstallationMapper installationMapper,
      NodeMapper nodeMapper,
      NetworkMapper networkMapper,
      InstitutionMapper institutionMapper,
      CollectionMapper collectionMapper,
      AuthenticationFacade authenticationFacade,
      @Qualifier("registryObjectMapper") ObjectMapper objectMapper) {
    this.userRightsMapper = userRightsMapper;
    this.organizationMapper = organizationMapper;
    this.datasetMapper = datasetMapper;
    this.installationMapper = installationMapper;
    this.nodeMapper = nodeMapper;
    this.networkMapper = networkMapper;
    this.institutionMapper = institutionMapper;
    this.collectionMapper = collectionMapper;
    this.objectMapper = objectMapper;
    this.authenticationFacade = authenticationFacade;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {

    if (containsCheckPermissionsOnlyParam(request)
        && "POST".equals(request.getMethod())
        && isEmptyBody(((GbifHttpServletRequestWrapper) request).getContent())) {

      // if the body is empty we create it
      Optional<Resource> resource = getResourceToCreate(request);
      if (resource.isPresent()) {
        Authentication authentication = authenticationFacade.getAuthentication();
        Optional<Object> entity = createEntity(authentication.getName(), resource.get());
        try {
          request =
              new GbifHttpServletRequestWrapper(
                  request,
                  entity.isPresent() ? objectMapper.writeValueAsString(entity.get()) : "{}",
                  true);
        } catch (JsonProcessingException e) {
          LOG.warn("Error creating the auth pre check body");
        }
      }
    }

    filterChain.doFilter(request, response);
  }

  private Optional<Object> createEntity(String username, Resource resource) {
    Map<String, Set<UUID>> scopeEntities = identifyUserRights(username);
    List<String> namespaces = userRightsMapper.getNamespacesByUser(username);
    List<Country> countryScopes = userRightsMapper.getCountriesByUser(username);

    Function<String, UUID> getScope =
        k -> {
          if (scopeEntities.containsKey(k)) {
            return scopeEntities.get(k).iterator().next();
          }
          return null;
        };

    Supplier<UUID> endorsingNodeOrganizationSupplier =
        () -> {
          // get a random organization that is endorsed by this node
          List<Organization> endorsedOrgs =
              organizationMapper.organizationsEndorsedBy(getScope.apply(NODE), null);
          if (endorsedOrgs != null && !endorsedOrgs.isEmpty()) {
            return endorsedOrgs.get(0).getKey();
          }
          return null;
        };

    if (resource.name.equals(MACHINE_TAG)) {
      return namespaces.isEmpty()
          ? Optional.empty()
          : Optional.of(new MachineTag(namespaces.get(0), "authPreCheck", "authPreCheck"));
    }

    if (resource.name.equals(DATASET)) {
      Dataset dataset = new Dataset();
      dataset.setPublishingOrganizationKey(getScope.apply(ORGANIZATION));
      dataset.setInstallationKey(getScope.apply(INSTALLATION));

      if (scopeEntities.containsKey(NODE)) {
        dataset.setPublishingOrganizationKey(endorsingNodeOrganizationSupplier.get());
      }

      return Optional.of(dataset);
    }

    if (resource.name.equals(ORGANIZATION)) {
      Organization organization = new Organization();
      organization.setEndorsingNodeKey(getScope.apply(NODE));
      return Optional.of(organization);
    }

    if (resource.name.equals(INSTALLATION)) {
      Installation installation = new Installation();
      installation.setOrganizationKey(getScope.apply(ORGANIZATION));
      if (scopeEntities.containsKey(NODE)) {
        installation.setOrganizationKey(endorsingNodeOrganizationSupplier.get());
      }
      return Optional.of(installation);
    }

    // grscicoll
    if (resource.name.equals(INSTITUTION) && !countryScopes.isEmpty()) {
      Institution institution = new Institution();
      Address address = new Address();
      address.setCountry(countryScopes.get(0));
      institution.setAddress(address);
      return Optional.of(institution);
    }

    if (resource.name.equals(COLLECTION)) {
      Collection collection = new Collection();
      collection.setInstitutionKey(getScope.apply(INSTITUTION));

      if (!countryScopes.isEmpty()) {
        Address address = new Address();
        address.setCountry(countryScopes.get(0));
        collection.setAddress(address);
      }

      return Optional.of(collection);
    }

    // grscicoll merge
    BiFunction<String, Predicate<UUID>, UUID> getScopeWithFilter =
        (k, p) -> {
          if (scopeEntities.containsKey(k)) {
            return scopeEntities.get(k).stream().filter(p).findFirst().orElse(null);
          }
          return null;
        };

    if (resource.name.equals(INSTITUTION_MERGE)) {
      MergeParams mergeParams = new MergeParams();
      mergeParams.setReplacementEntityKey(
          getScopeWithFilter.apply(INSTITUTION, k -> !k.equals(resource.key)));
      return Optional.of(mergeParams);
    }

    if (resource.name.equals(COLLECTION_MERGE)) {
      MergeParams mergeParams = new MergeParams();
      mergeParams.setReplacementEntityKey(
          getScopeWithFilter.apply(COLLECTION, k -> !k.equals(resource.key)));
      return Optional.of(mergeParams);
    }

    return Optional.empty();
  }

  private Map<String, Set<UUID>> identifyUserRights(String username) {
    Map<String, Set<UUID>> entitiesFound = new HashMap<>();

    List<UUID> keys = userRightsMapper.getKeysByUser(username);
    for (UUID k : keys) {
      resolveUserRight(k)
          .ifPresent(t -> entitiesFound.computeIfAbsent(t, v -> new HashSet<>()).add(k));
    }
    return entitiesFound;
  }

  private Optional<String> resolveUserRight(UUID key) {
    if (datasetMapper.get(key) != null) {
      return Optional.of(DATASET);
    }
    if (organizationMapper.get(key) != null) {
      return Optional.of(ORGANIZATION);
    }
    if (installationMapper.get(key) != null) {
      return Optional.of(INSTALLATION);
    }
    if (nodeMapper.get(key) != null) {
      return Optional.of(NODE);
    }
    if (networkMapper.get(key) != null) {
      return Optional.of(NETWORK);
    }
    if (institutionMapper.get(key) != null) {
      return Optional.of(INSTITUTION);
    }
    if (collectionMapper.get(key) != null) {
      return Optional.of(COLLECTION);
    }
    return Optional.empty();
  }

  @VisibleForTesting
  Optional<Resource> getResourceToCreate(HttpServletRequest request) {
    final String path = request.getRequestURI();

    if (path.contains("/machineTag")) {
      return Optional.of(new Resource(MACHINE_TAG));
    }

    if (path.contains(GrSciCollEditorAuthorizationFilter.GRSCICOLL_PATH)) {
      Matcher mergeMatch = MERGE_PATTERN.matcher(path);
      if (mergeMatch.matches()) {
        return mergeMatch.group(1).equals(INSTITUTION)
            ? Optional.of(new Resource(INSTITUTION_MERGE, UUID.fromString(mergeMatch.group(2))))
            : Optional.of(new Resource(COLLECTION_MERGE, UUID.fromString(mergeMatch.group(2))));
      }

      Matcher createEntityMatch = INST_COLL_CREATE_PATTERN.matcher(path);
      if (createEntityMatch.matches()) {
        return Optional.of(new Resource(createEntityMatch.group(1)));
      }
    } else {
      final String requestWithMethod = request.getMethod() + " " + path;

      Optional<Matcher> networkEntityMatcher =
          POST_RESOURCES_TO_FILTER.stream()
              .map(p -> p.matcher(requestWithMethod))
              .filter(Matcher::matches)
              .findFirst();

      if (networkEntityMatcher.isPresent()) {
        return Optional.of(new Resource(networkEntityMatcher.get().group(1)));
      }
    }

    return Optional.empty();
  }

  private boolean isEmptyBody(String content) {
    if (content == null) {
      return true;
    }

    String contentNormalized = WHITESPACE_PATTERN.matcher(content).replaceAll("");
    return contentNormalized.isEmpty() || contentNormalized.equals("{}");
  }

  @VisibleForTesting
  static class Resource {
    String name;
    UUID key;

    Resource(String name) {
      this.name = name;
    }

    Resource(String name, UUID key) {
      this.name = name;
      this.key = key;
    }
  }
}
