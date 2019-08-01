package org.gbif.registry.ws.security;

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

  // TODO: 2019-07-10 and what should be if OrganizationMapper is needed instead
  private UserMapper userMapper;

  public RegistryUserDetailsService(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  @Override
  public UserDetails loadUserByUsername(final String username) {
    final GbifUser user = username.contains("@") ?
        userMapper.getByEmail(username) :
        userMapper.get(username);

    if (user == null) {
      throw new UsernameNotFoundException(username);
    }

    return new GbifUserPrincipal(user);
  }
}
