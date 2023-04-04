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
