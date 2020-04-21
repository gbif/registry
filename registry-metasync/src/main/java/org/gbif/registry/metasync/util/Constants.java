/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.metasync.util;

/** Constants used by the Metadata synchroniser. */
public final class Constants {

  public static final String ABCD_12_SCHEMA = "http://www.tdwg.org/schemas/abcd/1.2";
  public static final String ABCD_206_SCHEMA = "http://www.tdwg.org/schemas/abcd/2.06";

  // "Names" used in Machine Tags, but not yet stored in TagName in gbif-api.
  public static final String INSTALLATION_VERSION = "version";
  // END: "Names" used in Machine Tags

  private Constants() {
    throw new UnsupportedOperationException("Can't initialize class");
  }
}
