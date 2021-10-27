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
package org.gbif.registry.domain.collections;

import org.gbif.api.model.collections.CollectionEntityType;

import java.util.Date;
import java.util.UUID;

public class AuditLog {

  private long key;
  private long traceId;
  private CollectionEntityType collectionEntityType;
  private String subEntityType;
  private String operation;
  private UUID collectionEntityKey;
  private String subEntityKey;
  private UUID replacementKey;
  private Date created;
  private String createdBy;
  private String preState;
  private String postState;

  public long getKey() {
    return key;
  }

  public void setKey(long key) {
    this.key = key;
  }

  public long getTraceId() {
    return traceId;
  }

  public void setTraceId(long traceId) {
    this.traceId = traceId;
  }

  public CollectionEntityType getCollectionEntityType() {
    return collectionEntityType;
  }

  public void setCollectionEntityType(CollectionEntityType collectionEntityType) {
    this.collectionEntityType = collectionEntityType;
  }

  public String getSubEntityType() {
    return subEntityType;
  }

  public void setSubEntityType(String subEntityType) {
    this.subEntityType = subEntityType;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public UUID getCollectionEntityKey() {
    return collectionEntityKey;
  }

  public void setCollectionEntityKey(UUID collectionEntityKey) {
    this.collectionEntityKey = collectionEntityKey;
  }

  public String getSubEntityKey() {
    return subEntityKey;
  }

  public void setSubEntityKey(String subEntityKey) {
    this.subEntityKey = subEntityKey;
  }

  public UUID getReplacementKey() {
    return replacementKey;
  }

  public void setReplacementKey(UUID replacementKey) {
    this.replacementKey = replacementKey;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getPreState() {
    return preState;
  }

  public void setPreState(String preState) {
    this.preState = preState;
  }

  public String getPostState() {
    return postState;
  }

  public void setPostState(String postState) {
    this.postState = postState;
  }
}
