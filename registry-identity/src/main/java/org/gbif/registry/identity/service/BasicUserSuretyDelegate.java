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
package org.gbif.registry.identity.service;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.registry.surety.ChallengeCodeManager;

import java.util.List;
import java.util.UUID;

/**
 * A UserSuretyDelegate that only implements the ChallengeCode related functions.
 * To instantiate it use:
 * <pre>
 *   @Bean
 *   public UserSuretyDelegate userSuretyDelegate(ChallengeCodeManager<Integer> challengeCodeManager) {
 *     return new BasicUserSuretyDelegate(challengeCodeManager);
 *   }
 * </pre>
 */
public class BasicUserSuretyDelegate implements UserSuretyDelegate {

  private final ChallengeCodeManager<Integer> challengeCodeManager;

  public BasicUserSuretyDelegate(ChallengeCodeManager<Integer> challengeCodeManager) {
    this.challengeCodeManager = challengeCodeManager;
  }

  @Override
  public boolean hasChallengeCode(Integer userKey) {
    return this.challengeCodeManager.hasChallengeCode(userKey);
  }

  @Override
  public boolean isValidChallengeCode(Integer userKey, String email, UUID challengeCode) {
    return this.challengeCodeManager.isValidChallengeCode(userKey, challengeCode, email);
  }

  @Override
  public void onNewUser(GbifUser gbifUser) {
    throw new UnsupportedOperationException(
        "OccurrenceUserSuretyDelegate does not support this operation");
  }

  @Override
  public boolean confirmUser(GbifUser gbifUser, UUID uuid) {
    throw new UnsupportedOperationException(
        "OccurrenceUserSuretyDelegate does not support this operation");
  }

  @Override
  public boolean confirmAndNotifyUser(GbifUser gbifUser, UUID uuid) {
    throw new UnsupportedOperationException(
        "OccurrenceUserSuretyDelegate does not support this operation");
  }

  @Override
  public boolean confirmUserAndEmail(GbifUser gbifUser, String s, UUID uuid) {
    throw new UnsupportedOperationException(
        "OccurrenceUserSuretyDelegate does not support this operation");
  }

  @Override
  public void onDeleteUser(GbifUser gbifUser, List<Download> list) {
    throw new UnsupportedOperationException(
        "OccurrenceUserSuretyDelegate does not support this operation");
  }

  @Override
  public void onPasswordReset(GbifUser gbifUser) {
    throw new UnsupportedOperationException(
        "OccurrenceUserSuretyDelegate does not support this operation");
  }

  @Override
  public void onPasswordChanged(GbifUser gbifUser) {
    throw new UnsupportedOperationException(
        "OccurrenceUserSuretyDelegate does not support this operation");
  }

  @Override
  public void onChangeEmail(GbifUser gbifUser, String newEmail) {
    throw new UnsupportedOperationException(
        "OccurrenceUserSuretyDelegate does not support this operation");
  }

  @Override
  public void onEmailChanged(GbifUser gbifUser, String oldEmail) {
    throw new UnsupportedOperationException(
        "OccurrenceUserSuretyDelegate does not support this operation");
  }
}
