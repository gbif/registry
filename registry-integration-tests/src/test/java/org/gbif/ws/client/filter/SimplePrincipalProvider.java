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
package org.gbif.ws.client.filter;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.UserPrincipal;

import java.security.Principal;
import java.util.function.Supplier;

import com.google.common.base.Strings;

/**
 * A Principal provider providing the same principal every time. The principal to be provided can be
 * changed at any time.
 *
 * <p>Useful for testing ws-resources.
 */
public class SimplePrincipalProvider implements Supplier<Principal> {

  private UserPrincipal current;

  public void setPrincipal(String username) {
    if (Strings.isNullOrEmpty(username)) {
      current = null;
    } else {
      User user = new User();
      user.setUserName(username);
      current = new UserPrincipal(user);
    }
  }

  @Override
  public Principal get() {
    return current;
  }
}
