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
package org.gbif.registry.metadata;

/** Enum of all GBIF Metadata Profile versions. */
public enum EMLProfileVersion {
  GBIF_1_0("1.0", "http://rs.gbif.org/schema/eml-gbif-profile/1.0/eml.xsd"),
  GBIF_1_0_1("1.0.1", "http://rs.gbif.org/schema/eml-gbif-profile/1.0.1/eml.xsd"),
  GBIF_1_0_2("1.0.2", "http://rs.gbif.org/schema/eml-gbif-profile/1.0.2/eml.xsd"),
  GBIF_1_1("1.1", "http://rs.gbif.org/schema/eml-gbif-profile/1.1/eml.xsd");

  private final String version;
  private final String schemaLocation;

  EMLProfileVersion(String version, String schemaLocation) {
    this.version = version;
    this.schemaLocation = schemaLocation;
  }

  /** @return textual representation of the version */
  public String getVersion() {
    return version;
  }

  /** @return textual representation of the schema location */
  public String getSchemaLocation() {
    return schemaLocation;
  }
}
