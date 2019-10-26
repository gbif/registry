package org.gbif.registry.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.gbif.api.model.registry.Network;
import org.hibernate.validator.HibernateValidator;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class Networks extends JsonBackedData<Network> {

  private static final Networks INSTANCE = new Networks();

  public static Network newInstance() {
    return INSTANCE.newTypedInstance();
  }

  public Networks() {
    super("data/network.json", new TypeReference<Network>() {
    });
  }

  @Test
  public void testConstraints() {
    ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory();
    Validator validator = validatorFactory.getValidator();

    Set<ConstraintViolation<Network>> violations = validator.validate(Networks.newInstance());
    assertTrue(violations.isEmpty());
  }
}
