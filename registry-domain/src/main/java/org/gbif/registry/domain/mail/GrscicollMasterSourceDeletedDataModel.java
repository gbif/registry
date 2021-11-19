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
package org.gbif.registry.domain.mail;

import java.net.URL;

/**
 * Model to generate an email to notify the deletion of the master source of a GRSciColl entity.
 *
 * <p>This class is required to be public for Freemarker.
 */
public class GrscicollMasterSourceDeletedDataModel {

  private URL collectionEntityLink;

  private String collectionEntityName;

  private URL masterSourceLink;

  private String masterSourceName;

  public URL getCollectionEntityLink() {
    return collectionEntityLink;
  }

  public void setCollectionEntityLink(URL collectionEntityLink) {
    this.collectionEntityLink = collectionEntityLink;
  }

  public String getCollectionEntityName() {
    return collectionEntityName;
  }

  public void setCollectionEntityName(String collectionEntityName) {
    this.collectionEntityName = collectionEntityName;
  }

  public URL getMasterSourceLink() {
    return masterSourceLink;
  }

  public void setMasterSourceLink(URL masterSourceLink) {
    this.masterSourceLink = masterSourceLink;
  }

  public String getMasterSourceName() {
    return masterSourceName;
  }

  public void setMasterSourceName(String masterSourceName) {
    this.masterSourceName = masterSourceName;
  }
}
