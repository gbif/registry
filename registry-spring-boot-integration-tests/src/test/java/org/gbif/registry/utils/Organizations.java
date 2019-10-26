package org.gbif.registry.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.gbif.api.model.registry.Organization;
import org.hibernate.validator.HibernateValidator;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class Organizations extends JsonBackedData<Organization> {

  private static final Organizations INSTANCE = new Organizations();

  public Organizations() {
    super("data/organization.json", new TypeReference<Organization>() {});
  }

  public static Organization newInstance(UUID endorsingNodeKey) {
    Organization o = INSTANCE.newTypedInstance();
    o.setEndorsingNodeKey(endorsingNodeKey);
    o.setPassword("password");
    return o;
  }

  @Test
  public void testConstraints() {
    ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory();
    Validator validator = validatorFactory.getValidator();

    Set<ConstraintViolation<Organization>> violations = validator.validate(Organizations.newInstance(UUID.randomUUID()));
    assertTrue(violations.isEmpty());
  }
}
