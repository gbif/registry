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
package org.gbif.registry.ws.it.collections.service.batch;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.service.collections.batch.BaseBatchHandler;
import org.gbif.registry.service.collections.batch.FileFields;
import org.gbif.registry.service.collections.batch.model.ContactsParserResult;
import org.gbif.registry.service.collections.batch.model.EntitiesParserResult;
import org.gbif.registry.service.collections.batch.model.ParsedData;
import org.gbif.registry.ws.it.collections.service.BaseServiceIT;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import static org.gbif.registry.service.collections.batch.FileParsingUtils.normalizeValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class BaseBatchHandlerIT<T extends CollectionEntity> extends BaseServiceIT {

  protected final BaseBatchHandler<T> batchHandler;
  protected final CollectionEntityService<T> entityService;
  protected final CollectionEntityType entityType;

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  public BaseBatchHandlerIT(
      SimplePrincipalProvider simplePrincipalProvider,
      BaseBatchHandler<T> batchHandler,
      CollectionEntityService<T> entityService,
      CollectionEntityType entityType) {
    super(simplePrincipalProvider);
    this.batchHandler = batchHandler;
    this.entityService = entityService;
    this.entityType = entityType;
  }

  @Test
  public void mergeEntitiesTest()
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    Set<String> headers = new HashSet<>();
    headers.add(FileFields.CommonFields.CODE);
    headers.add(FileFields.CommonFields.NAME);
    headers.add(FileFields.CommonFields.ACTIVE);

    T existing = newEntity();
    existing.setCode("c1");
    existing.setName("n1");
    existing.setActive(false);
    existing.setDescription("desc");

    T parsed = newEntity();
    parsed.setCode("c11");
    parsed.setName("n11");
    parsed.setActive(true);
    parsed.setDescription("desc2");

    T merged = batchHandler.mergeEntities(existing, parsed, headers);

    assertEquals("c11", merged.getCode());
    assertEquals("n11", merged.getName());
    assertTrue(merged.isActive());
    assertEquals("desc", merged.getDescription());
  }

  @Test
  public void createResultFileTest() throws IOException {
    Resource institutionsFile =
        new ClassPathResource("collections/" + entityType.name().toLowerCase() + "s.csv");
    Resource contactsFile =
        new ClassPathResource("collections/" + entityType.name().toLowerCase() + "s_contacts.csv");
    ExportFormat format = ExportFormat.CSV;

    T parsedEntity = newEntity();
    parsedEntity.setKey(UUID.randomUUID());
    parsedEntity.setCode("c1");
    parsedEntity.setDescription("desc1");

    T parsedEntity2 = newEntity();
    parsedEntity2.setKey(UUID.randomUUID());
    parsedEntity2.setCode("c2");
    parsedEntity2.setDescription("desc2");

    Map<String, Integer> fileHeadersIndex = new HashMap<>();
    fileHeadersIndex.put(FileFields.CommonFields.KEY, 0);
    fileHeadersIndex.put(FileFields.CommonFields.CODE, 1);
    fileHeadersIndex.put(FileFields.CommonFields.NAME, 2);
    fileHeadersIndex.put(FileFields.CommonFields.DESCRIPTION, 3);
    fileHeadersIndex.put(FileFields.CommonFields.ACTIVE, 4);

    ParsedData<T> parsedData =
        ParsedData.<T>builder()
            .entity(parsedEntity)
            .errors(Collections.singletonList("entity error"))
            .build();

    ParsedData<T> parsedData2 = ParsedData.<T>builder().entity(parsedEntity2).build();

    Map<String, ParsedData<T>> parsedDataMap = new HashMap<>();
    parsedDataMap.put(parsedEntity.getCode(), parsedData);
    parsedDataMap.put(parsedEntity2.getCode(), parsedData2);

    EntitiesParserResult<T> parserResult =
        EntitiesParserResult.<T>builder()
            .format(format)
            .fileHeadersIndex(fileHeadersIndex)
            .parsedDataMap(parsedDataMap)
            .build();

    Contact contact = new Contact();
    contact.setKey(1);
    contact.setFirstName("name1");
    contact.setLastName("lastn1");
    contact.setPosition(Collections.singletonList("tester"));
    int contactHash = Objects.hash(Arrays.asList("name1", "lastn1", "tester"));

    ParsedData<Contact> contactParsedData =
        ParsedData.<Contact>builder()
            .entity(contact)
            .errors(Collections.singletonList("contact error"))
            .build();

    Map<String, Integer> contactsHeadersIndex = new HashMap<>();
    contactsHeadersIndex.put(FileFields.CommonFields.KEY, 0);
    contactsHeadersIndex.put(FileFields.ContactFields.FIRST_NAME, 1);
    contactsHeadersIndex.put(FileFields.ContactFields.LAST_NAME, 2);
    contactsHeadersIndex.put(FileFields.ContactFields.POSITION, 3);

    ContactsParserResult contactsParserResult =
        ContactsParserResult.builder()
            .format(format)
            .fileHeadersIndex(contactsHeadersIndex)
            .contactsByKey(Collections.singletonMap(String.valueOf(contactHash), contactParsedData))
            .build();

    Path resultFile =
        batchHandler.createResultFile(
            StreamUtils.copyToByteArray(institutionsFile.getInputStream()),
            StreamUtils.copyToByteArray(contactsFile.getInputStream()),
            parserResult,
            contactsParserResult,
            1);

    List<Path> unzippedFiles = ZipUtils.unzip(resultFile, "src/test/resources/collections");
    assertEquals(2, unzippedFiles.size());

    // csv options
    CSVParser csvParser = new CSVParserBuilder().withSeparator(format.getDelimiter()).build();

    for (Path unzipped : unzippedFiles) {
      try (CSVReader csvReader =
          new CSVReaderBuilder(new BufferedReader(new FileReader(unzipped.toFile())))
              .withCSVParser(csvParser)
              .build()) {
        List<String> headers = Arrays.asList(csvReader.readNextSilently());
        assertTrue(headers.contains(FileFields.CommonFields.KEY));
        assertTrue(headers.contains(FileFields.CommonFields.ERRORS));

        // it should have one more column with the errors
        if (unzipped.getFileName().toString().startsWith("result-")) {
          assertEquals(fileHeadersIndex.size() + 1, headers.size());
        } else if (unzipped.getFileName().toString().startsWith("contacts-")) {
          assertEquals(contactsHeadersIndex.size() + 1, headers.size());
        }

        String[] values;
        while ((values = csvReader.readNextSilently()) != null) {
          values = normalizeValues(headers.size(), values);
          assertNotNull(values[headers.indexOf(FileFields.CommonFields.KEY)]);
          assertNotNull(values[headers.indexOf(FileFields.CommonFields.ERRORS)]);
        }
      }
    }

    Files.deleteIfExists(resultFile);
    for (Path f : unzippedFiles) {
      Files.deleteIfExists(f);
    }
  }

  abstract T newEntity();
}
