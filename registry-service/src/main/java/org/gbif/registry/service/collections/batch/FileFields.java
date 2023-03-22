package org.gbif.registry.service.collections.batch;

import java.util.Arrays;
import java.util.List;

public class FileFields {

  public static class InstitutionFields {
    public static final String INSTITUTION_TYPE = "TYPE";
    public static final String ADDITIONAL_NAMES = "ADDITIONAL_NAMES";
    public static final String LOGO_URL = "LOGO_URL";
    public static final String INSTITUTIONAL_GOVERNANCE = "INSTITUTIONAL_GOVERNANCE";
    public static final String DISCIPLINES = "DISCIPLINES";
    public static final String LATITUDE = "LATITUDE";
    public static final String LONGITUDE = "LONGITUDE";
    public static final String FOUNDING_DATE = "FOUNDING_DATE";
    public static final String GEOGRAPHIC_DESCRIPTION = "GEOGRAPHIC_DESCRIPTION";
    public static final String TAXONOMIC_DESCRIPTION = "TAXONOMIC_DESCRIPTION";

    protected static final List<String> ALL_FIELDS =
        Arrays.asList(
            INSTITUTION_TYPE,
            ADDITIONAL_NAMES,
            LOGO_URL,
            INSTITUTIONAL_GOVERNANCE,
            DISCIPLINES,
            LATITUDE,
            LONGITUDE,
            FOUNDING_DATE,
            GEOGRAPHIC_DESCRIPTION,
            TAXONOMIC_DESCRIPTION);
  }

  public static class ContactFields {
    public static final String INSTITUTION_CODE = "INSTITUTION_CODE";
    public static final String INSTITUTION_KEY = "INSTITUTION_KEY";
    public static final String KEY = "KEY";
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

    protected static final List<String> ALL_FIELDS =
        Arrays.asList(
            INSTITUTION_CODE,
            INSTITUTION_KEY,
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

  public static class CommonFields {

    public static final String ERRORS = "ERRORS";
    public static final String KEY = "KEY";
    public static final String CODE = "CODE";
    public static final String NAME = "NAME";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String ACTIVE = "ACTIVE";
    public static final String EMAIL = "EMAILS";
    public static final String PHONE = "PHONES";
    public static final String ALT_CODES = "ALTERNATIVE_CODES";
    public static final String IDENTIFIERS = "IDENTIFIERS";
    public static final String HOMEPAGE = "HOMEPAGE";
    public static final String CATALOG_URL = "CATALOG_URL";
    public static final String API_URL = "API_URL";
    public static final String NUMBER_SPECIMENS = "NUMBER_SPECIMENS";

    /** address fields */
    private static final String ADDRESS_PREFIX = "ADDRESS_";

    private static final String MAILING_ADDRESS_PREFIX = "MAILING_ADDRESS_";
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
            MAIL_COUNTRY);
  }

  public static boolean isInstitutionField(String field) {
    return InstitutionFields.ALL_FIELDS.contains(field.toUpperCase())
        || CommonFields.ALL_FIELDS.contains(field.toUpperCase());
  }

  public static boolean isContactField(String field) {
    return ContactFields.ALL_FIELDS.contains(field.toUpperCase());
  }
}
