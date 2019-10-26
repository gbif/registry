package org.gbif.registry.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.gbif.api.model.registry.Node;
import org.hibernate.validator.HibernateValidator;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class Nodes extends JsonBackedData<Node> {

  private static final Nodes INSTANCE = new Nodes();

  public static Node newInstance() {
    return INSTANCE.newTypedInstance();
  }

  public static String newInstanceRawJson() {
    return INSTANCE.newInstanceAsRawJson();
  }

  public Nodes() {
    super("data/node.json", new TypeReference<Node>() {
    });
  }

  @Test
  public void testConstraints() {
    ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory();
    Validator validator = validatorFactory.getValidator();

    Set<ConstraintViolation<Node>> violations = validator.validate(Nodes.newInstance());
    assertTrue(violations.isEmpty());
  }
}
