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
package org.gbif.registry.service.collections.batch.model;

import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.common.export.ExportFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import lombok.Builder;
import lombok.Getter;

@Builder
public class ContactsParserResult implements ParserResult<Contact> {

  @Getter private Map<String, List<ParsedData<Contact>>> contactsByEntity;
  private Map<String, ParsedData<Contact>> contactsByKey;
  private List<String> duplicates;
  private List<String> fileErrors = new ArrayList<>();
  private Map<String, Integer> fileHeadersIndex;
  private ExportFormat format;

  @Override
  public Map<String, ParsedData<Contact>> getParsedDataMap() {
    return contactsByKey;
  }

  @Override
  public List<String> getDuplicates() {
    return duplicates;
  }

  @Override
  public List<String> getFileErrors() {
    return fileErrors;
  }

  @Override
  public Map<String, Integer> getFileHeadersIndex() {
    return fileHeadersIndex;
  }

  @Override
  public Function<Contact, String> getEntityKeyExtractor() {
    return c -> c.getKey() != null ? c.getKey().toString() : null;
  }

  @Override
  public ExportFormat getFormat() {
    return format;
  }
}
