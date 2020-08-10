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
package org.gbif.registry.security;

import org.gbif.api.model.common.GbifUser;
import org.gbif.registry.persistence.mapper.UserMapper;
import org.gbif.ws.security.GbifUserPrincipal;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Qualifier("registryUserDetailsService")
public class RegistryUserDetailsService implements UserDetailsService {

  private final UserMapper userMapper;

  public RegistryUserDetailsService(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  @Override
  public UserDetails loadUserByUsername(final String username) {
    final GbifUser user =
        username.contains("@") ? userMapper.getByEmail(username) : userMapper.get(username);

    if (user == null) {
      throw new UsernameNotFoundException(username);
    }

    return new GbifUserPrincipal(user);
  }
}
