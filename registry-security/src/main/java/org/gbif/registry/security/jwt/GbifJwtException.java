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
package org.gbif.registry.security.jwt;

/** Exception to handle all the possible JWT error cases. */
public class GbifJwtException extends Exception {

  private final JwtErrorCode errorCode;

  public GbifJwtException(JwtErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  public JwtErrorCode getErrorCode() {
    return errorCode;
  }

  public enum JwtErrorCode {
    EXPIRED_TOKEN,
    INVALID_TOKEN,
    INVALID_USERNAME
  }
}
