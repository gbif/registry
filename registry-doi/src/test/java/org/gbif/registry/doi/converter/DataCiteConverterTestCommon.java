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
package org.gbif.registry.doi.converter;

import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.utils.file.FileUtils;

public class DataCiteConverterTestCommon {

  static String getXmlMetadataFromFile(String fileName) throws Exception {
    DataCiteMetadata metadata = DataCiteValidator.fromXml(FileUtils.classpathStream(fileName));
    return DataCiteValidator.toXml(metadata, true);
  }
}
