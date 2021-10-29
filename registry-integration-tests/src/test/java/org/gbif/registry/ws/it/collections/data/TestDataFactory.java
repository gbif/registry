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
package org.gbif.registry.ws.it.collections.data;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.vocabulary.collections.AccessionStatus;
import org.gbif.api.vocabulary.collections.PreservationType;

import java.net.URI;
import java.util.Collections;
import java.util.UUID;

import static org.gbif.registry.ws.it.fixtures.TestConstants.WS_TEST;

public class TestDataFactory {

  private TestDataFactory() {}

  public static <T extends CollectionEntity> TestData<T> create(Class<T> clazz) {
    if (clazz.isAssignableFrom(Institution.class)) {
      return (TestData<T>) new InstitutionTestData();
    } else if (clazz.isAssignableFrom(Collection.class)) {
      return (TestData<T>) new CollectionTestData();
    } else if (clazz.isAssignableFrom(Person.class)) {
      return (TestData<T>) new PersonTestData();
    }
    throw new UnsupportedOperationException();
  }

  public static class CollectionTestData implements TestData<Collection> {

    public static final String NAME = "name";
    public static final String DESCRIPTION = "dummy description";
    public static final AccessionStatus ACCESSION_STATUS = AccessionStatus.INSTITUTIONAL;
    public static final String CODE_UPDATED = "code2";
    public static final String NAME_UPDATED = "name2";
    public static final String DESCRIPTION_UPDATED = "dummy description updated";
    public static final AccessionStatus ACCESSION_STATUS_UPDATED = AccessionStatus.PROJECT;

    private CollectionTestData() {}

    @Override
    public Collection newEntity() {
      Collection collection = new Collection();
      collection.setCode(UUID.randomUUID().toString());
      collection.setName(NAME);
      collection.setDescription(DESCRIPTION);
      collection.setActive(true);
      collection.setAccessionStatus(ACCESSION_STATUS);
      collection.setPreservationTypes(
          Collections.singletonList(PreservationType.SAMPLE_CRYOPRESERVED));
      return collection;
    }

    @Override
    public Collection updateEntity(Collection collection) {
      collection.setCode(CODE_UPDATED);
      collection.setName(NAME_UPDATED);
      collection.setDescription(DESCRIPTION_UPDATED);
      collection.setAccessionStatus(ACCESSION_STATUS_UPDATED);
      return collection;
    }

    @Override
    public Collection newInvalidEntity() {
      return new Collection();
    }
  }

  public static class InstitutionTestData implements TestData<Institution> {

    public static final String NAME = "name";
    public static final String DESCRIPTION = "dummy description";
    public static final URI HOMEPAGE = URI.create("http://dummy");
    public static final String CODE_UPDATED = "code2";
    public static final String NAME_UPDATED = "name2";
    public static final String DESCRIPTION_UPDATED = "dummy description updated";
    public static final String ADDITIONAL_NAME = "additional name";

    private InstitutionTestData() {}

    @Override
    public Institution newEntity() {
      Institution institution = new Institution();
      institution.setCode(UUID.randomUUID().toString());
      institution.setName(NAME);
      institution.setDescription(DESCRIPTION);
      institution.setHomepage(HOMEPAGE);
      institution.setAdditionalNames(Collections.singletonList("other"));
      return institution;
    }

    @Override
    public Institution updateEntity(Institution institution) {
      institution.setCode(CODE_UPDATED);
      institution.setName(NAME_UPDATED);
      institution.setDescription(DESCRIPTION_UPDATED);
      institution.setAdditionalNames(Collections.singletonList(ADDITIONAL_NAME));
      return institution;
    }

    @Override
    public Institution newInvalidEntity() {
      return new Institution();
    }
  }

  public static class PersonTestData implements TestData<Person> {

    public static final String FIRST_NAME = "first name";
    public static final String LAST_NAME = "last name";
    public static final String POSITION = "position";
    public static final String PHONE = "134235435";
    public static final String EMAIL = "dummy@dummy.com";

    public static final String FIRST_NAME_UPDATED = "first name updated";
    public static final String POSITION_UPDATED = "new position";
    public static final String PHONE_UPDATED = "134235433";

    private PersonTestData() {}

    @Override
    public Person newEntity() {
      Person person = new Person();
      person.setFirstName(FIRST_NAME);
      person.setLastName(LAST_NAME);
      person.setPosition(POSITION);
      person.setPhone(PHONE);
      person.setEmail(EMAIL);
      person.setCreatedBy(WS_TEST);
      person.setModifiedBy(WS_TEST);
      return person;
    }

    @Override
    public Person updateEntity(Person person) {
      person.setFirstName(FIRST_NAME_UPDATED);
      person.setPosition(POSITION_UPDATED);
      person.setPhone(PHONE_UPDATED);
      return person;
    }

    @Override
    public Person newInvalidEntity() {
      return new Person();
    }
  }
}
