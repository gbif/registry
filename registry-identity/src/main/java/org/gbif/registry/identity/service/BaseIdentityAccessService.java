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
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.registry.persistence.mapper.UserMapper;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

import static org.gbif.registry.identity.util.IdentityUtils.NORMALIZE_EMAIL_FCT;
import static org.gbif.registry.identity.util.IdentityUtils.NORMALIZE_USERNAME_FCT;

/**
 * This service only provides access to user by loging/email. For authentication and other
 * functionality use {@link IdentityServiceImpl}.
 */
@Service
public class BaseIdentityAccessService implements IdentityAccessService {

  private static final Logger LOG = LoggerFactory.getLogger(BaseIdentityAccessService.class);

  private final UserMapper userMapper;

  public BaseIdentityAccessService(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  /**
   * Get a {@link GbifUser} using its identifier (username or email). The username is case
   * insensitive.
   *
   * @param identifier user's username or email
   * @return {@link GbifUser} or null
   */
  @Override
  @Nullable
  public GbifUser get(String identifier) {
    if (Strings.isNullOrEmpty(identifier)) {
      return null;
    }
    // this assumes username name can not contains @ (which is the case, see AbstractGbifUser's
    // getUserName())
    return StringUtils.contains(identifier, "@")
        ? getByEmail(identifier)
        : userMapper.get(NORMALIZE_USERNAME_FCT.apply(identifier));
  }

  /**
   * Get a {@link GbifUser} using its email. The email is case insensitive.
   *
   * @param email user's email
   * @return {@link GbifUser} or null
   */
  @Nullable
  private GbifUser getByEmail(String email) {
    // emails are stored in lowercase
    // the mybatis mapper will run the query with a lower()
    return userMapper.getByEmail(NORMALIZE_EMAIL_FCT.apply(email));
  }

  @Nullable
  @Override
  public GbifUser authenticate(String s, String s1) {
    LOG.warn("Method is not implemented, use IdentityServiceImpl#authenticate instead!");
    return null;
  }
}
