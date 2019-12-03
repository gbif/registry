package org.gbif.registry.mail;

public interface EmailType {

  String getSubjectKey();

  String getFtlTemplate();
}
