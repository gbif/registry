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
package org.gbif.registry.persistence.mapper.collections.dto;

import java.util.UUID;

public class DuplicateMetadataDto {

  private UUID key;
  private boolean active;
  private boolean isIh;
  private boolean isIdigbio;

  public UUID getKey() {
    return key;
  }

  public void setKey(UUID key) {
    this.key = key;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public boolean isIh() {
    return isIh;
  }

  public void setIh(boolean ih) {
    isIh = ih;
  }

  public boolean isIdigbio() {
    return isIdigbio;
  }

  public void setIdigbio(boolean idigbio) {
    isIdigbio = idigbio;
  }
}
