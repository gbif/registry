package org.gbif.registry.surety;

import org.gbif.registry.surety.email.BaseEmailModel;
import org.gbif.registry.surety.email.EmailManager;

/**
 * Email manager that does nothing.
 * TODO should probably save something to disk to allow testing
 */
public class EmptyEmailManager implements EmailManager {

  @Override
  public void send(BaseEmailModel baseEmailModel) {
  }
}
