package org.gbif.registry.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.gbif.api.model.registry.MachineTag;
import org.hibernate.validator.HibernateValidator;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class MachineTags extends JsonBackedData<MachineTag> {

  private static final MachineTags INSTANCE = new MachineTags();

  public static MachineTag newInstance() {
    return INSTANCE.newTypedInstance();
  }

  public MachineTags() {
    super("data/machine_tag.json", new TypeReference<MachineTag>() {
    });
  }

  @Test
  public void testConstraints() {
    ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory();
    Validator validator = validatorFactory.getValidator();

    Set<ConstraintViolation<MachineTag>> violations = validator.validate(MachineTags.newInstance());
    assertTrue(violations.isEmpty());
  }
}
