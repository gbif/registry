package org.gbif.registry.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.gbif.api.model.registry.Tag;
import org.hibernate.validator.HibernateValidator;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class Tags extends JsonBackedData<Tag> {

  private static final Tags INSTANCE = new Tags();

  public static Tag newInstance() {
    return INSTANCE.newTypedInstance();
  }

  public Tags() {
    super("data/tag.json", new TypeReference<Tag>() {
    });
  }

  @Test
  public void testConstraints() {
    ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory();
    Validator validator = validatorFactory.getValidator();

    Set<ConstraintViolation<Tag>> violations = validator.validate(Tags.newInstance());
    assertTrue(violations.isEmpty());
  }
}
