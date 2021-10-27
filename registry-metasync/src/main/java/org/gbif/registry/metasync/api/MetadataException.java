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
package org.gbif.registry.metasync.api;

/** Any exception happening during synchronisation will be converted to this exception. */
public class MetadataException extends Exception {

  private static final long serialVersionUID = -6555608958328542296L;
  private final ErrorCode errorCode;

  public MetadataException(String message, ErrorCode errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public MetadataException(Throwable cause, ErrorCode errorCode) {
    super(cause);
    this.errorCode = errorCode;
  }

  public MetadataException(String message, Throwable cause, ErrorCode errorCode) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public MetadataException(ErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
