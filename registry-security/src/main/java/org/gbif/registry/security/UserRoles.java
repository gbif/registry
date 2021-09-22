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

/**
 * Simple utils class to expose the API user role enumeration also as static strings required by the
 * JSR 250 annotations. Unit tests makes sure these are indeed the same.
 */
public class UserRoles {

  private UserRoles() {}

  // UserRole.REGISTRY_ADMIN.name();
  public static final String ADMIN_ROLE = "REGISTRY_ADMIN";
  // UserRole.REGISTRY_EDITOR.name();
  public static final String EDITOR_ROLE = "REGISTRY_EDITOR";

  public static final String USER_ROLE = "USER";

  public static final String APP_ROLE = "APP";

  public static final String IPT_ROLE = "IPT";

  public static final String GRSCICOLL_ADMIN_ROLE = "GRSCICOLL_ADMIN";

  public static final String GRSCICOLL_EDITOR_ROLE = "GRSCICOLL_EDITOR";

  public static final String GRSCICOLL_MEDIATOR_ROLE = "GRSCICOLL_MEDIATOR";

  @Deprecated public static final String IDIGBIO_GRSCICOLL_EDITOR_ROLE = "IDIGBIO_GRSCICOLL_EDITOR";
}
