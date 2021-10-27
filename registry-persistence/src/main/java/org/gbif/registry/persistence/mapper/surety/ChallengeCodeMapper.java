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
package org.gbif.registry.persistence.mapper.surety;

import org.gbif.api.model.ChallengeCode;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/** Mapper for ChallengeCode table */
@Repository
public interface ChallengeCodeMapper {

  void createChallengeCode(ChallengeCode challengeCode);

  UUID getChallengeCode(@Param("key") Integer key);

  ChallengeCode getChallengeCodeObject(@Param("key") Integer key);

  void deleteChallengeCode(int key);
}
