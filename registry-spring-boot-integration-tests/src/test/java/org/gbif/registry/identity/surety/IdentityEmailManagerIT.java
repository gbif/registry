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
package org.gbif.registry.identity.surety;

import org.gbif.api.model.ChallengeCode;
import org.gbif.api.model.common.GbifUser;
import org.gbif.registry.domain.mail.BaseEmailModel;
import org.gbif.registry.mail.identity.IdentityEmailManager;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

/**
 * Tests related to {@link IdentityEmailManager}. The main purpose of the following tests is to
 * ensure we can generate a {@link BaseEmailModel} using the Freemarker templates.
 */
@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class IdentityEmailManagerIT {

  @Autowired private IdentityEmailManager identityEmailManager;

  private GbifUser generateTestUser() {
    GbifUser newUser = new GbifUser();
    newUser.setUserName("User");
    newUser.setEmail("a@b.com");
    return newUser;
  }

  @Test
  public void testGenerateNewUserEmailModel() throws IOException {
    GbifUser newUser = generateTestUser();
    BaseEmailModel baseEmail =
        identityEmailManager.generateNewUserEmailModel(newUser, ChallengeCode.newRandom());
    assertNotNull("We can generate the model from the template", baseEmail);
  }

  @Test
  public void testGenerateResetPasswordEmailModel() throws IOException {
    GbifUser newUser = generateTestUser();
    BaseEmailModel baseEmail =
        identityEmailManager.generateResetPasswordEmailModel(newUser, ChallengeCode.newRandom());
    assertNotNull("We can generate the model from the template", baseEmail);
  }

  @Test
  public void testGenerateWelcomeEmailModel() throws IOException {
    GbifUser newUser = new GbifUser();
    newUser.setUserName("User");
    newUser.setEmail("a@b.com");
    BaseEmailModel baseEmail = identityEmailManager.generateWelcomeEmailModel(newUser);
    assertNotNull("We can generate the model from the template", baseEmail);
  }
}
