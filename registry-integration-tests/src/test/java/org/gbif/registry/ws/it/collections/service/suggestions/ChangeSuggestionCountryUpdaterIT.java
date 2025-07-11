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
package org.gbif.registry.ws.it.collections.service.suggestions;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.descriptors.DescriptorChangeSuggestion;
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.InstitutionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.persistence.mapper.collections.ChangeSuggestionMapper;
import org.gbif.registry.persistence.mapper.collections.DescriptorChangeSuggestionMapper;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeSuggestionDto;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.security.UserRoles;
import org.gbif.registry.ws.client.collections.CollectionClient;
import org.gbif.registry.ws.client.collections.InstitutionClient;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChangeSuggestionCountryUpdaterIT extends BaseItTest {

  private final ChangeSuggestionMapper changeSuggestionMapper;
  private final InstitutionClient institutionClient;
  private final CollectionClient collectionClient;
  private final DescriptorChangeSuggestionMapper descriptorChangeSuggestionMapper;

  @Autowired
  public ChangeSuggestionCountryUpdaterIT(
      ChangeSuggestionMapper changeSuggestionMapper,
      SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer,
      KeyStore keyStore,
      @LocalServerPort int localServerPort,
    DescriptorChangeSuggestionMapper descriptorChangeSuggestionMapper) {
    super(simplePrincipalProvider, esServer);
    this.changeSuggestionMapper = changeSuggestionMapper;
    this.descriptorChangeSuggestionMapper = descriptorChangeSuggestionMapper;
    this.institutionClient = prepareClient(localServerPort, keyStore, InstitutionClient.class);
    this.collectionClient = prepareClient(localServerPort, keyStore, CollectionClient.class);

    // Set up security context for tests
    SecurityContext ctx = SecurityContextHolder.createEmptyContext();
    SecurityContextHolder.setContext(ctx);
    ctx.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            "test",
            "",
            Collections.singletonList(
                new SimpleGrantedAuthority(UserRoles.GRSCICOLL_ADMIN_ROLE))));
  }

  @Test
  public void testInstitutionCountryUpdate() {
    // Create an institution with Denmark as country
    Institution institution = new Institution();
    institution.setCode("test");
    institution.setName("Test Institution");

    Address address = new Address();
    address.setCountry(Country.DENMARK);
    institution.setAddress(address);

    UUID institutionKey = institutionClient.create(institution);

    // Create a change suggestion for this institution
    InstitutionChangeSuggestion suggestion = new InstitutionChangeSuggestion();
    suggestion.setEntityKey(institutionKey);
    suggestion.setType(Type.UPDATE);
    suggestion.setProposerEmail("test@example.org");
    suggestion.setComments(Collections.singletonList("Test comment"));

    Institution suggestedInstitution = institutionClient.get(institutionKey);
    suggestedInstitution.setCode("test-updated");
    suggestedInstitution.setName("Test Institution Updated");

    suggestion.setSuggestedEntity(suggestedInstitution);

    institutionClient.createChangeSuggestion(suggestion);

    // Verify country in suggestion
    List<ChangeSuggestionDto> suggestions =
        changeSuggestionMapper.list(null, null, CollectionEntityType.INSTITUTION, null, institutionKey, null, null, null);

    assertEquals(1, suggestions.size());
    assertEquals(Country.DENMARK.getIso2LetterCode(), suggestions.get(0).getCountryIsoCode());

    // Update institution's country
    institution = institutionClient.get(institutionKey);
    address = institution.getAddress();
    address.setCountry(Country.TURKEY);
    institution.setAddress(address);

    institutionClient.update(institution);

    // Verify country in suggestion is updated
    suggestions =
        changeSuggestionMapper.list(null, null, CollectionEntityType.INSTITUTION, null, institutionKey, null, null, null);

    assertEquals(1, suggestions.size());
    assertEquals(Country.TURKEY.getIso2LetterCode(), suggestions.get(0).getCountryIsoCode());
  }

  @Test
  public void testCollectionCountryUpdate() throws IOException {
    // Create a collection with Denmark as country
    Collection collection = new Collection();
    collection.setCode("test");
    collection.setName("Test Collection");

    Address address = new Address();
    address.setCountry(Country.DENMARK);
    collection.setAddress(address);

    UUID collectionKey = collectionClient.create(collection);

    // Create a change suggestion for this collection
    CollectionChangeSuggestion suggestion = new CollectionChangeSuggestion();
    suggestion.setEntityKey(collectionKey);
    suggestion.setType(Type.UPDATE);
    suggestion.setProposerEmail("test@example.org");
    suggestion.setComments(Collections.singletonList("Test comment"));

    Resource descriptorsResource = new ClassPathResource("collections/descriptors.csv");
    MultipartFile descriptorsFile =
      new MockMultipartFile("file", "descriptors.csv", "text/csv", descriptorsResource.getInputStream());

    DescriptorChangeSuggestion descriptorChangeSuggestion = getDescriptorChangeSuggestion(
      collectionKey);

    Collection suggestedCollection = collectionClient.get(collectionKey);
    suggestedCollection.setCode("test-updated");
    suggestedCollection.setName("Test Collection Updated");

    suggestion.setSuggestedEntity(suggestedCollection);

    collectionClient.createChangeSuggestion(suggestion);
    collectionClient.createDescriptorSuggestion(
      collectionKey,
      descriptorsFile,
      descriptorChangeSuggestion.getType(),
      descriptorChangeSuggestion.getTitle(),
      descriptorChangeSuggestion.getDescription(),
      descriptorChangeSuggestion.getFormat(),
      descriptorChangeSuggestion.getComments(),
      descriptorChangeSuggestion.getProposerEmail(),
      descriptorChangeSuggestion.getTags()
    );

    // Verify country in suggestion
    List<ChangeSuggestionDto> suggestions =
        changeSuggestionMapper.list(null, null, CollectionEntityType.COLLECTION, null, collectionKey, null, null, null);

    assertEquals(1, suggestions.size());
    assertEquals(Country.DENMARK.getIso2LetterCode(), suggestions.get(0).getCountryIsoCode());

    List<DescriptorChangeSuggestion> descriptorChangeSuggestions = descriptorChangeSuggestionMapper.list(null, null, null, null, collectionKey, null);

    assertEquals(1, descriptorChangeSuggestions.size());
    assertEquals(Country.DENMARK, descriptorChangeSuggestions.get(0).getCountry());

    // Update collection's country
    collection = collectionClient.get(collectionKey);
    address = collection.getAddress();
    address.setCountry(Country.TURKEY);
    collection.setAddress(address);

    collectionClient.update(collection);

    // Verify country in suggestion is updated
    suggestions =
        changeSuggestionMapper.list(null, null, CollectionEntityType.COLLECTION, null, collectionKey, null, null, null);
    descriptorChangeSuggestions =
      descriptorChangeSuggestionMapper.list(null, null, null, null, collectionKey, null);

    assertEquals(1, suggestions.size());
    assertEquals(Country.TURKEY.getIso2LetterCode(), suggestions.get(0).getCountryIsoCode());

    assertEquals(1, descriptorChangeSuggestions.size());
    assertEquals(Country.TURKEY, descriptorChangeSuggestions.get(0).getCountry());
  }

  private DescriptorChangeSuggestion getDescriptorChangeSuggestion(UUID collectionKey) {
    DescriptorChangeSuggestion descriptorChangeSuggestion = new DescriptorChangeSuggestion();
    descriptorChangeSuggestion.setCollectionKey(collectionKey);
    descriptorChangeSuggestion.setTitle("test title");
    descriptorChangeSuggestion.setType(Type.UPDATE);
    descriptorChangeSuggestion.setFormat(ExportFormat.CSV);
    descriptorChangeSuggestion.setProposerEmail("test@example.org");
    descriptorChangeSuggestion.setComments(Collections.singletonList("Test comment"));
    descriptorChangeSuggestion.setSuggestedFile("file/path");
    return descriptorChangeSuggestion;
  }
}
