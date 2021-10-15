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
package org.gbif.registry.test;

import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.io.IOException;

import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.beanutils.WrapDynaBean;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.Resources;

/** Base class providing basic Jackson based factories for new object instances. */
@SuppressWarnings("UnstableApiUsage")
abstract class JsonBackedData<T> {

  private final ObjectMapper mapper;
  private final TypeReference<T> type;
  private final String json; // for reuse to save file IO
  private final SimplePrincipalProvider simplePrincipalProvider;

  // only for instantiation by subclasses, type references are required to keep typing at runtime
  protected JsonBackedData(
      String file,
      TypeReference<T> type,
      ObjectMapper objectMapper,
      SimplePrincipalProvider simplePrincipalProvider) {
    json = getJson(file);
    this.type = type;
    this.mapper = objectMapper;
    this.simplePrincipalProvider = simplePrincipalProvider;
  }

  public T newInstance() {
    try {
      return addRequiredFields(mapper.readValue(json, type));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  // utility method to read the file, and throw RTE if there is a problem
  private String getJson(String file) {
    try {
      return Resources.toString(Resources.getResource(file), Charsets.UTF_8);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Add required fields createdBy and modifiedBy (where possible) to target object which either
   * extends NetworkEntity or is a Contact, Endpoint, MachineTag, Tag, Identifier, or Comment.
   *
   * @param target object
   */
  private T addRequiredFields(T target) {
    if (simplePrincipalProvider != null) {
      WrapDynaBean wrapped = new WrapDynaBean(target);
      DynaClass dynaClass = wrapped.getDynaClass();
      // update createdBy field
      if (dynaClass.getDynaProperty("createdBy") != null) {
        wrapped.set("createdBy", "WS TEST");
      }
      // update modifiedBy field
      if (dynaClass.getDynaProperty("modifiedBy") != null) {
        wrapped.set("modifiedBy", "WS TEST");
      }
    }
    return target;
  }
}
