package org.gbif.registry.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.gbif.api.model.registry.Comment;
import org.hibernate.validator.HibernateValidator;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class Comments extends JsonBackedData<Comment> {

  private static final Comments INSTANCE = new Comments();

  public static Comment newInstance() {
    return INSTANCE.newTypedInstance();
  }

  public Comments() {
    super("data/comment.json", new TypeReference<Comment>() {
    });
  }

  @Test
  public void testConstraints() {
    ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory();
    Validator validator = validatorFactory.getValidator();

    Set<ConstraintViolation<Comment>> violations = validator.validate(Comments.newInstance());
    assertTrue(violations.isEmpty());
  }
}
