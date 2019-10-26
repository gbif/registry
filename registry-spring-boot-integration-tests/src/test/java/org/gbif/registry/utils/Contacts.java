package org.gbif.registry.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.gbif.api.model.registry.Contact;
import org.hibernate.validator.HibernateValidator;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class Contacts extends JsonBackedData<Contact> {

  private static final Contacts INSTANCE = new Contacts();

  public static Contact newInstance() {
    return INSTANCE.newTypedInstance();
  }

  public Contacts() {
    super("data/contact.json", new TypeReference<Contact>() {
    });
  }

  @Test
  public void testConstraints() {
    ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory();
    Validator validator = validatorFactory.getValidator();

    Set<ConstraintViolation<Contact>> violations = validator.validate(Contacts.newInstance());
    assertTrue(violations.isEmpty());
  }
}
