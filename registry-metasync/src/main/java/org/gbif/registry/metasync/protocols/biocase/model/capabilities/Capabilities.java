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
package org.gbif.registry.metasync.protocols.biocase.model.capabilities;

import org.gbif.registry.metasync.util.Constants;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.digester3.annotations.rules.CallMethod;
import org.apache.commons.digester3.annotations.rules.CallParam;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetNext;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@ObjectCreate(pattern = "response")
public class Capabilities {

  private final Map<String, String> versions = Maps.newHashMap();

  private final List<SupportedSchema> supportedSchemas = Lists.newArrayList();

  public Map<String, String> getVersions() {
    return Collections.unmodifiableMap(versions);
  }

  public List<SupportedSchema> getSupportedSchemas() {
    return Collections.unmodifiableList(supportedSchemas);
  }

  @CallMethod(pattern = "response/header/version")
  public void addVersion(
      @CallParam(pattern = "response/header/version", attributeName = "software") String name,
      @CallParam(pattern = "response/header/version") String version) {
    versions.put(name, version);
  }

  @SetNext
  public void addSupportedSchema(SupportedSchema supportedSchema) {
    supportedSchemas.add(supportedSchema);
  }

  public String getPreferredSchema() {
    boolean abcd12 = false;
    for (SupportedSchema schema : supportedSchemas) {
      if (schema.getNamespace().toASCIIString().equals(Constants.ABCD_206_SCHEMA)) {
        return Constants.ABCD_206_SCHEMA;
      } else if (schema.getNamespace().toASCIIString().equals(Constants.ABCD_12_SCHEMA)) {
        abcd12 = true;
      }
    }

    return abcd12 ? Constants.ABCD_12_SCHEMA : null;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("versions", versions)
        .add("supportedSchemas", supportedSchemas)
        .toString();
  }
}
