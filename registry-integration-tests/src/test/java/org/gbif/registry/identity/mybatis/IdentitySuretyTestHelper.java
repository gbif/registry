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
package org.gbif.registry.identity.mybatis;

import org.gbif.registry.persistence.ChallengeCodeSupportMapper;
import org.gbif.registry.persistence.mapper.surety.ChallengeCodeMapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

/** Helper class that offers direct access to challenge codes for testing purpose (only). */
@Component
public class IdentitySuretyTestHelper {

  private ChallengeCodeMapper challengeCodeMapper;
  private ChallengeCodeSupportMapper<Integer> challengeCodeSupportMapper;

  public IdentitySuretyTestHelper(
      ChallengeCodeMapper challengeCodeMapper,
      ChallengeCodeSupportMapper<Integer> challengeCodeSupportMapper) {
    this.challengeCodeMapper = challengeCodeMapper;
    this.challengeCodeSupportMapper = challengeCodeSupportMapper;
  }

  public UUID getChallengeCode(Integer entityKey) {
    return challengeCodeMapper.getChallengeCode(
        challengeCodeSupportMapper.getChallengeCodeKey(entityKey));
  }
}
