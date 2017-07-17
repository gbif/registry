package org.gbif.identity;

/**
 * Constants relataed to the identity module.
 */
public class IdentityConstants {

  /**Utility class*/
  private IdentityConstants() {}

  //last part of the path here is not a folder but the prefix of the ResourceBundle (email_subjects_en, email_subjects_fr)
  public static final String EMAIL_SUBJECTS_RESOURCE = "email/subjects/identity_email_subjects";
  public static final String USER_CREATE_SUBJECT_KEY = "createAccount";
  public static final String RESET_PASSWORD_SUBJECT_KEY = "resetPassword";

  public static final String PROPERTY_PREFIX = "identity.";
  public static final String DB_PROPERTY_PREFIX = "registry.db.";

  public static final String CONFIRM_ORGANIZATION_URL_TEMPLATE = "mail.urlTemplate.confirmUser";
  public static final String RESET_PASSWORD_URL_TEMPLATE = "mail.urlTemplate.resetPassword";

  //Feemarker templates
  public static final String USER_CREATE_FTL_TEMPLATE = "create_confirmation_en.ftl";
  public static final String RESET_PASSWORD_FTL_TEMPLATE = "reset_password_en.ftl";

}
