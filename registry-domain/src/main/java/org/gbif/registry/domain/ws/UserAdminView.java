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
package org.gbif.registry.domain.ws;

import org.gbif.api.model.common.GbifUser;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** Administrative view of {@link org.gbif.api.model.common.User} . */
public class UserAdminView {

  private GbifUser user;
  private boolean challengeCodePresent;

  public UserAdminView() {}

  public UserAdminView(GbifUser user, boolean challengeCodePresent) {
    this.user = user;
    this.challengeCodePresent = challengeCodePresent;
  }

  @JsonUnwrapped
  public GbifUser getUser() {
    return user;
  }

  public boolean isChallengeCodePresent() {
    return challengeCodePresent;
  }
}
