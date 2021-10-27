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
package org.gbif.registry.identity.model;

/** Enumeration of possible errors in user model mutations (creates and updates). */
public enum ModelMutationError {
  USER_ALREADY_EXIST,

  PASSWORD_LENGTH_VIOLATION,

  /** a user can update its own email but it shall not be use by another user */
  EMAIL_ALREADY_IN_USE,

  CONSTRAINT_VIOLATION
}
