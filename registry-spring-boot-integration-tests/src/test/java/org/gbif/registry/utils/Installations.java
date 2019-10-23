package org.gbif.registry.utils;

import org.codehaus.jackson.type.TypeReference;
import org.gbif.api.model.registry.Installation;
import org.hibernate.validator.HibernateValidator;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class Installations extends JsonBackedData<Installation> {

  private static final Installations INSTANCE = new Installations();

  public Installations() {
    super("data/installation.json", new TypeReference<Installation>() {});
  }

  public static Installation newInstance(UUID organizationKey) {
    Installation i = INSTANCE.newTypedInstance();
    i.setOrganizationKey(organizationKey);
    return i;
  }

  @Test
  public void testConstraints() {
    ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory();
    Validator validator = validatorFactory.getValidator();

    Set<ConstraintViolation<Installation>> violations = validator.validate(Installations.newInstance(UUID.randomUUID()));
    assertTrue(violations.isEmpty());
  }
}
