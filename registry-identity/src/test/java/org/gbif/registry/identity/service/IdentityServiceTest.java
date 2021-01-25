/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.identity.service;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.identity.model.ModelMutationError;
import org.gbif.registry.identity.model.UserModelMutationResult;
import org.gbif.registry.persistence.mapper.UserMapper;

import java.util.concurrent.atomic.AtomicInteger;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IdentityServiceTest {

  private static final String TEST_PASSWORD = "[password]";
  private static final String TEST_PASSWORD2 = "]password[";
  private static final AtomicInteger index = new AtomicInteger(0);
  private ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();

  @Mock private UserMapper userMapperMock;
  @Mock private UserSuretyDelegate userSuretyDelegateMock;
  @Spy private Validator validator = validatorFactory.getValidator();

  @InjectMocks private IdentityServiceImpl identityService;

  @Test
  public void testCreate() {
    // GIVEN
    GbifUser gbifUser = generateUser();

    // WHEN
    UserModelMutationResult result = identityService.create(gbifUser, TEST_PASSWORD);

    // THEN
    assertNotNull(result.getUsername());
    assertFalse(result.containsError());
    verify(userMapperMock).get(gbifUser.getUserName());
    verify(userMapperMock).getByEmail(gbifUser.getEmail());
    verify(userMapperMock).create(any(GbifUser.class));
    verify(userSuretyDelegateMock).onNewUser(any(GbifUser.class));
  }

  @Test
  public void testGet() {
    // GIVEN
    GbifUser gbifUser = generateUser();
    when(userMapperMock.get(gbifUser.getUserName())).thenReturn(gbifUser);

    // WHEN
    GbifUser result = identityService.get(gbifUser.getUserName());

    // THEN
    assertEquals(gbifUser, result);
    verify(userMapperMock).get(gbifUser.getUserName());
  }

  @Test
  public void testGetByEmail() {
    // GIVEN
    GbifUser gbifUser = generateUser();
    when(userMapperMock.getByEmail(gbifUser.getEmail())).thenReturn(gbifUser);

    // WHEN
    GbifUser result = identityService.get(gbifUser.getEmail());

    // THEN
    assertEquals(gbifUser, result);
    verify(userMapperMock).getByEmail(gbifUser.getEmail());
  }

  @Test
  public void testUpdate() {
    // GIVEN
    GbifUser gbifUser = generateUser();
    gbifUser.setKey(1);
    gbifUser.setPasswordHash("hash");
    when(userMapperMock.getByKey(gbifUser.getKey())).thenReturn(gbifUser);

    // WHEN
    UserModelMutationResult result = identityService.update(gbifUser);

    // THEN
    assertNotNull(result.getUsername());
    assertFalse(result.containsError());
    verify(userMapperMock).getByKey(gbifUser.getKey());
    verify(userMapperMock).update(gbifUser);
  }

  @Test
  public void testCreateExistingCauseValidationError() {
    // GIVEN
    GbifUser gbifUser = generateUser();
    when(userMapperMock.get(gbifUser.getUserName())).thenReturn(gbifUser);

    // WHEN
    UserModelMutationResult result = identityService.create(gbifUser, TEST_PASSWORD);

    // THEN
    assertEquals(ModelMutationError.USER_ALREADY_EXIST, result.getError());
    verify(userMapperMock).get(gbifUser.getUserName());
  }

  @Test
  public void testCreateEmptyUsernameCauseValidationError() {
    // GIVEN
    GbifUser gbifUser = generateUser();
    gbifUser.setUserName("");

    // WHEN
    UserModelMutationResult result = identityService.create(gbifUser, TEST_PASSWORD);

    // THEN
    assertEquals(ModelMutationError.CONSTRAINT_VIOLATION, result.getError());
    verify(userMapperMock).get(gbifUser.getUserName());
  }

  @Test
  public void testCreateTooShortPasswordCauseValidationError() {
    // GIVEN
    GbifUser gbifUser = generateUser();

    // WHEN
    UserModelMutationResult result = identityService.create(gbifUser, "p");

    // THEN
    assertEquals(ModelMutationError.PASSWORD_LENGTH_VIOLATION, result.getError());
    verify(userMapperMock).get(gbifUser.getUserName());
  }

  @Test
  public void testGetBySystemSettings() {
    // GIVEN
    GbifUser gbifUser = generateUser();
    when(userMapperMock.getBySystemSetting("internal.settings", "18")).thenReturn(gbifUser);

    // WHEN
    GbifUser presentResult = identityService.getBySystemSetting("internal.settings", "18");
    GbifUser absentResult = identityService.getBySystemSetting("random.settings", "1");

    // THEN
    assertNotNull(presentResult);
    assertNull(absentResult);
    verify(userMapperMock).getBySystemSetting("internal.settings", "18");
    verify(userMapperMock).getBySystemSetting("random.settings", "1");
  }

  /** Generates a different user on each call. Thread-Safe */
  public static GbifUser generateUser() {
    int idx = index.incrementAndGet();
    GbifUser user = new GbifUser();
    user.setUserName("user_" + idx);
    user.setFirstName("Tim");
    user.setLastName("Robertson");
    user.getRoles().add(UserRole.USER);
    user.getSettings().put("user.settings.language", "en");
    user.getSettings().put("user.country", "dk");
    user.getSystemSettings().put("internal.settings", "18");
    user.setEmail("user_" + idx + "@gbif.org");
    return user;
  }
}
