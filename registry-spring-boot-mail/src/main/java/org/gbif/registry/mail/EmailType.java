package org.gbif.registry.mail;

/**
 * Represents email type, provides with email subject and raw template.
 */
public interface EmailType {

  /**
   * Returns email subject.
   */
  String getSubjectKey();

  /**
   * Returns a ftl template for email.
   */
  String getFtlTemplate();
}
