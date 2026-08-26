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

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.MetadataMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.UserMapper;
import org.gbif.registry.persistence.mapper.UserRightsMapper;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditorAuthorizationServiceImplTest {

  private static final String USERNAME = "contact_user";
  private static final String USER_EMAIL = "contact@example.org";
  private static final UUID DATASET_KEY = UUID.randomUUID();

  @Mock private OrganizationMapper organizationMapper;
  @Mock private DatasetMapper datasetMapper;
  @Mock private InstallationMapper installationMapper;
  @Mock private UserRightsMapper userRightsMapper;
  @Mock private MachineTagMapper machineTagMapper;
  @Mock private MetadataMapper metadataMapper;
  @Mock private UserMapper userMapper;

  private EditorAuthorizationServiceImpl service;

  @BeforeEach
  public void setUp() {
    service =
        new EditorAuthorizationServiceImpl(
            organizationMapper,
            datasetMapper,
            installationMapper,
            userRightsMapper,
            machineTagMapper,
            metadataMapper,
            userMapper);
  }

  @Test
  void allowedToCrawlDatasetWithEditorRights() {
    when(userRightsMapper.keyExistsForUser(USERNAME, DATASET_KEY)).thenReturn(true);

    assertTrue(service.allowedToCrawlDataset(USERNAME, DATASET_KEY));
  }

  @Test
  void allowedToCrawlDatasetWithMatchingContactEmail() {
    when(userRightsMapper.keyExistsForUser(USERNAME, DATASET_KEY)).thenReturn(false);
    when(datasetMapper.get(DATASET_KEY)).thenReturn(null);

    GbifUser user = new GbifUser();
    user.setUserName(USERNAME);
    user.setEmail(USER_EMAIL);
    when(userMapper.get(USERNAME)).thenReturn(user);

    Contact contact = new Contact();
    contact.setEmail(List.of("other@example.org", USER_EMAIL));
    when(datasetMapper.listContacts(DATASET_KEY)).thenReturn(List.of(contact));

    assertTrue(service.allowedToCrawlDataset(USERNAME, DATASET_KEY));
  }

  @Test
  void allowedToCrawlDatasetWithCaseInsensitiveEmailMatch() {
    when(userRightsMapper.keyExistsForUser(USERNAME, DATASET_KEY)).thenReturn(false);
    when(datasetMapper.get(DATASET_KEY)).thenReturn(null);

    GbifUser user = new GbifUser();
    user.setUserName(USERNAME);
    user.setEmail("Contact@Example.ORG");
    when(userMapper.get(USERNAME)).thenReturn(user);

    Contact contact = new Contact();
    contact.setEmail(List.of("contact@example.org"));
    when(datasetMapper.listContacts(DATASET_KEY)).thenReturn(List.of(contact));

    assertTrue(service.allowedToCrawlDataset(USERNAME, DATASET_KEY));
  }

  @Test
  void notAllowedToCrawlDatasetWithWrongEmail() {
    when(userRightsMapper.keyExistsForUser(USERNAME, DATASET_KEY)).thenReturn(false);
    when(datasetMapper.get(DATASET_KEY)).thenReturn(new Dataset());

    GbifUser user = new GbifUser();
    user.setUserName(USERNAME);
    user.setEmail(USER_EMAIL);
    when(userMapper.get(USERNAME)).thenReturn(user);

    Contact contact = new Contact();
    contact.setEmail(List.of("someone.else@example.org"));
    when(datasetMapper.listContacts(DATASET_KEY)).thenReturn(List.of(contact));

    assertFalse(service.allowedToCrawlDataset(USERNAME, DATASET_KEY));
  }

  @Test
  void notAllowedToCrawlDatasetWithNoContacts() {
    when(userRightsMapper.keyExistsForUser(USERNAME, DATASET_KEY)).thenReturn(false);
    when(datasetMapper.get(DATASET_KEY)).thenReturn(null);

    GbifUser user = new GbifUser();
    user.setUserName(USERNAME);
    user.setEmail(USER_EMAIL);
    when(userMapper.get(USERNAME)).thenReturn(user);
    when(datasetMapper.listContacts(DATASET_KEY)).thenReturn(Collections.emptyList());

    assertFalse(service.allowedToCrawlDataset(USERNAME, DATASET_KEY));
  }

  @Test
  void notAllowedToCrawlDatasetWithNullNameOrKey() {
    assertFalse(service.allowedToCrawlDataset(null, DATASET_KEY));
    assertFalse(service.allowedToCrawlDataset(USERNAME, null));
  }
}
