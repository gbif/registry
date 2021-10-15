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

import org.gbif.api.model.common.AbstractGbifUser;
import org.gbif.api.model.common.GbifUser;

import javax.validation.constraints.NotNull;

/**
 * {@link AbstractGbifUser} concrete class that represents a user to be created. We are not using
 * {@link GbifUser} directly to avoid confusion with the password/passwordHash field.
 */
public class UserCreation extends AbstractGbifUser {

  private String password;

  @NotNull
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
