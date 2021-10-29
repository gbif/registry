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
package org.gbif.registry.ws.advice;

import org.gbif.registry.identity.model.PropertyConstants;
import org.gbif.registry.identity.model.UserModelMutationResult;
import org.gbif.registry.ws.UpdatePasswordException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class UserManagementControllerAdvice {

  @ExceptionHandler(UpdatePasswordException.class)
  public ResponseEntity<UserModelMutationResult> updatePasswordException(
      UpdatePasswordException e) {
    // determine if it's a challengeCode error
    boolean challengeCodeError =
        e.getUpdatePasswordMutationResult().getConstraintViolation().keySet().stream()
            .anyMatch(s -> s.equalsIgnoreCase(PropertyConstants.CHALLENGE_CODE_PROPERTY_NAME));

    return challengeCodeError
        ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        : ResponseEntity.unprocessableEntity().body(e.getUpdatePasswordMutationResult());
  }
}
