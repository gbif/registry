package org.gbif.registry.surety.email;

public interface EmailType {

  String getSubjectKey();

  String getFtlTemplate();
}
