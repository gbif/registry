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
package org.gbif.registry.service.collections.batch;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.gbif.api.model.collections.CollectionEntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.KEY;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileFields {

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class InstitutionFields {
    public static final String INSTITUTION_TYPE = "TYPE";
    public static final String ADDITIONAL_NAMES = "ADDITIONAL_NAMES";
    public static final String LOGO_URL = "LOGO_URL";
    public static final String INSTITUTIONAL_GOVERNANCE = "INSTITUTIONAL_GOVERNANCE";
    public static final String DISCIPLINES = "DISCIPLINES";
    public static final String LATITUDE = "LATITUDE";
    public static final String LONGITUDE = "LONGITUDE";
    public static final String FOUNDING_DATE = "FOUNDING_DATE";

    protected static final List<String> ALL_FIELDS =
        Arrays.asList(
            INSTITUTION_TYPE,
            ADDITIONAL_NAMES,
            LOGO_URL,
            INSTITUTIONAL_GOVERNANCE,
            DISCIPLINES,
            LATITUDE,
            LONGITUDE,
            FOUNDING_DATE);
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class CollectionFields {
    public static final String CONTENT_TYPES = "CONTENT_TYPES";
    public static final String PERSONAL_COLLECTION = "PERSONAL_COLLECTION";
    public static final String DOI = "DOI";
    public static final String PRESERVATION_TYPES = "PRESERVATION_TYPES";
    public static final String ACCESSION_STATUS = "ACCESSION_STATUS";
    public static final String INSTITUTION_KEY = "INSTITUTION_KEY";
    public static final String TAXONOMIC_COVERAGE = "TAXONOMIC_COVERAGE";
    public static final String GEOGRAPHIC_COVERAGE = "GEOGRAPHIC_COVERAGE";
    public static final String NOTES = "NOTES";
    public static final String INCORPORATED_COLLECTIONS = "INCORPORATED_COLLECTIONS";
    public static final String IMPORTANT_COLLECTIONS = "IMPORTANT_COLLECTIONS";
    public static final String IMPORTANT_COLLECTORS = "IMPORTANT_COLLECTORS";
    public static final String COLLECTION_SUMMARY = "COLLECTION_SUMMARY";
    public static final String DEPARTMENT = "DEPARTMENT";
    public static final String DIVISION = "DIVISION";
    public static final String TEMPORAL_COVERAGE = "TEMPORAL_COVERAGE";

    protected static final List<String> ALL_FIELDS =
        Arrays.asList(
            CONTENT_TYPES,
            PERSONAL_COLLECTION,
            DOI,
            PRESERVATION_TYPES,
            ACCESSION_STATUS,
            INSTITUTION_KEY,
            TAXONOMIC_COVERAGE,
            GEOGRAPHIC_COVERAGE,
            NOTES,
            INCORPORATED_COLLECTIONS,
            IMPORTANT_COLLECTIONS,
            IMPORTANT_COLLECTORS,
            COLLECTION_SUMMARY,
            DEPARTMENT,
            DIVISION,
            TEMPORAL_COVERAGE);
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class ContactFields {
    public static final String INSTITUTION_CODE = "INSTITUTION_CODE";
    public static final String INSTITUTION_KEY = "INSTITUTION_KEY";
    public static final String COLLECTION_CODE = "COLLECTION_CODE";
    public static final String COLLECTION_KEY = "COLLECTION_KEY";
    public static final String FIRST_NAME = "FIRST_NAME";
    public static final String LAST_NAME = "LAST_NAME";
    public static final String POSITION = "POSITION";
    public static final String PHONE = "PHONE";
    public static final String FAX = "FAX";
    public static final String EMAIL = "EMAIL";
    public static final String ADDRESS = "ADDRESS";
    public static final String CITY = "CITY";
    public static final String PROVINCE = "PROVINCE";
    public static final String COUNTRY = "COUNTRY";
    public static final String POSTAL_CODE = "POSTAL_CODE";
    public static final String PRIMARY = "PRIMARY";
    public static final String TAXONOMIC_EXPERTISE = "TAXONOMIC_EXPERTISE";
    public static final String NOTES = "NOTES";
    public static final String USER_IDS = "USER_IDS";

    public static String getEntityCode(CollectionEntityType entityType) {
      return entityType == CollectionEntityType.INSTITUTION ? INSTITUTION_CODE : COLLECTION_CODE;
    }

    protected static final List<String> ALL_FIELDS =
        Arrays.asList(
            INSTITUTION_CODE,
            INSTITUTION_KEY,
            COLLECTION_CODE,
            COLLECTION_KEY,
            KEY,
            FIRST_NAME,
            LAST_NAME,
            POSITION,
            PHONE,
            FAX,
            EMAIL,
            ADDRESS,
            CITY,
            PROVINCE,
            COUNTRY,
            POSTAL_CODE,
            PRIMARY,
            TAXONOMIC_EXPERTISE,
            NOTES,
            USER_IDS);
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  public static class CommonFields {

    public static final String ERRORS = "ERRORS";
    public static final String KEY = "KEY";
    public static final String CODE = "CODE";
    public static final String NAME = "NAME";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String ACTIVE = "ACTIVE";
    public static final String EMAIL = "EMAIL";
    public static final String PHONE = "PHONE";
    public static final String ALT_CODES = "ALTERNATIVE_CODES";
    public static final String IDENTIFIERS = "IDENTIFIERS";
    public static final String HOMEPAGE = "HOMEPAGE";
    public static final String CATALOG_URL = "CATALOG_URL";
    public static final String API_URL = "API_URL";
    public static final String NUMBER_SPECIMENS = "NUMBER_SPECIMENS";
    public static final String FEATURED_IMAGE_URL = "FEATURED_IMAGE_URL";
    public static final String FEATURED_IMAGE_LICENSE = "FEATURED_IMAGE_LICENSE";

    /** address fields */
    public static final String ADDRESS_PREFIX = "ADDRESS_";

    public static final String MAILING_ADDRESS_PREFIX = "MAILING_ADDRESS_";
    private static final String ADDRESS_SUFFIX = "ADDRESS";
    private static final String CITY_SUFFIX = "CITY";
    private static final String PROVINCE_SUFFIX = "PROVINCE";
    private static final String POSTAL_CODE_SUFFIX = "POSTAL_CODE";
    private static final String COUNTRY_SUFFIX = "COUNTRY";
    public static final String ADDRESS = ADDRESS_PREFIX + ADDRESS_SUFFIX;
    public static final String CITY = ADDRESS_PREFIX + CITY_SUFFIX;
    public static final String PROVINCE = ADDRESS_PREFIX + PROVINCE_SUFFIX;
    public static final String POSTAL_CODE = ADDRESS_PREFIX + POSTAL_CODE_SUFFIX;
    public static final String COUNTRY = ADDRESS_PREFIX + COUNTRY_SUFFIX;
    public static final String MAIL_ADDRESS = MAILING_ADDRESS_PREFIX + ADDRESS_SUFFIX;
    public static final String MAIL_CITY = MAILING_ADDRESS_PREFIX + CITY_SUFFIX;
    public static final String MAIL_PROVINCE = MAILING_ADDRESS_PREFIX + PROVINCE_SUFFIX;
    public static final String MAIL_POSTAL_CODE = MAILING_ADDRESS_PREFIX + POSTAL_CODE_SUFFIX;
    public static final String MAIL_COUNTRY = MAILING_ADDRESS_PREFIX + COUNTRY_SUFFIX;

    protected static final List<String> ALL_FIELDS =
        Arrays.asList(
            KEY,
            CODE,
            NAME,
            DESCRIPTION,
            ACTIVE,
            EMAIL,
            PHONE,
            ALT_CODES,
            IDENTIFIERS,
            HOMEPAGE,
            CATALOG_URL,
            API_URL,
            NUMBER_SPECIMENS,
            ADDRESS,
            CITY,
            PROVINCE,
            POSTAL_CODE,
            COUNTRY,
            MAIL_ADDRESS,
            MAIL_CITY,
            MAIL_PROVINCE,
            MAIL_POSTAL_CODE,
            MAIL_COUNTRY,
            FEATURED_IMAGE_URL,
            FEATURED_IMAGE_LICENSE);
  }

  public static boolean isEntityField(String field, CollectionEntityType entityType) {
    if (CommonFields.ALL_FIELDS.contains(field.toUpperCase())) {
      return true;
    }

    if (entityType == CollectionEntityType.INSTITUTION) {
      return InstitutionFields.ALL_FIELDS.contains(field.toUpperCase());
    }

    if (entityType == CollectionEntityType.COLLECTION) {
      return CollectionFields.ALL_FIELDS.contains(field.toUpperCase());
    }

    return false;
  }

  public static List<String> getEntityFields(CollectionEntityType entityType) {
    List<String> fields = new ArrayList<>(ContactFields.ALL_FIELDS);

    if (entityType == CollectionEntityType.INSTITUTION) {
      fields.addAll(InstitutionFields.ALL_FIELDS);
    }

    if (entityType == CollectionEntityType.COLLECTION) {
      fields.addAll(CollectionFields.ALL_FIELDS);
    }

    return fields;
  }

  public static boolean isContactField(String field) {
    return ContactFields.ALL_FIELDS.contains(field.toUpperCase());
  }
}
