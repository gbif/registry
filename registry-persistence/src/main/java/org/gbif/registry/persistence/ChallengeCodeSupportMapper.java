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
package org.gbif.registry.persistence;

import org.apache.ibatis.annotations.Param;

/**
 * Support for ChallengeCode based surety inside a Mapper. The implementation of this interface
 * allows an entity to be used with the ChallengeCodeManager.
 */
// TODO: 2019-07-01 Invalid bound statement (not found), moved to parent package for now
public interface ChallengeCodeSupportMapper<K> {

  Integer getChallengeCodeKey(@Param("key") K key);

  boolean updateChallengeCodeKey(
      @Param("key") K key, @Param("challengeCodeKey") Integer challengeCodeKey);
}
