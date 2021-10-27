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
package org.gbif.registry.metadata.parse.converter;

import org.gbif.api.vocabulary.MaintenanceUpdateFrequency;
import org.gbif.common.parsers.MaintenanceUpdateFrequencyParser;

/**
 * {@link org.apache.commons.beanutils.Converter} implementation that handles conversion to and from
 * <b>MaintenanceUpdateFrequency</b> ENUM objects.
 */
public class MaintenanceUpdateFrequencyConverter
    extends AbstractGbifParserConvert<MaintenanceUpdateFrequency> {

  /** Construct a <b>MaintenanceUpdateFrequencyConverter</b> <i>Converter</i>. */
  public MaintenanceUpdateFrequencyConverter() {
    super(MaintenanceUpdateFrequency.class, MaintenanceUpdateFrequencyParser.getInstance());
  }
}
