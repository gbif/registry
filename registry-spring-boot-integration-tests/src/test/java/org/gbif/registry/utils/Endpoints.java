package org.gbif.registry.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.gbif.api.model.registry.Endpoint;
import org.hibernate.validator.HibernateValidator;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class Endpoints extends JsonBackedData<Endpoint> {

  private static final Endpoints INSTANCE = new Endpoints();

  public static Endpoint newInstance() {
    Endpoint endpoint = INSTANCE.newTypedInstance();
    // Endpoint is unique in that nested machine tags will be created
    endpoint.addMachineTag(MachineTags.newInstance());
    return endpoint;
  }

  public Endpoints() {
    super("data/endpoint.json", new TypeReference<Endpoint>() {
    });
  }

  @Test
  public void testConstraints() {
    ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory();
    Validator validator = validatorFactory.getValidator();

    Set<ConstraintViolation<Endpoint>> violations = validator.validate(Endpoints.newInstance());
    assertTrue(violations.isEmpty());
  }
}
