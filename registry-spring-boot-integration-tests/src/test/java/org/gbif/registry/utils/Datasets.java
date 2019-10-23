package org.gbif.registry.utils;

import org.codehaus.jackson.type.TypeReference;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.License;
import org.hibernate.validator.HibernateValidator;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class Datasets extends JsonBackedData<Dataset> {

  private static final Datasets INSTANCE = new Datasets();

  public static final License DATASET_LICENSE = License.CC_BY_NC_4_0;
  public static final DOI DATASET_DOI = new DOI(DOI.TEST_PREFIX, "gbif.2014.XSD123");

  public Datasets() {
    super("data/dataset.json", new TypeReference<Dataset>() {
    });
  }

  public static Dataset newInstance(UUID publishingOrganizationKey, UUID installationKey) {
    Dataset d = INSTANCE.newTypedInstance();
    d.setPublishingOrganizationKey(publishingOrganizationKey);
    d.setInstallationKey(installationKey);
    d.setDoi(DATASET_DOI);
    d.setLicense(DATASET_LICENSE);
    return d;
  }

  @Test
  public void testConstraints() {
    ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory();
    Validator validator = validatorFactory.getValidator();

    Set<ConstraintViolation<Dataset>> violations = validator.validate(Datasets.newInstance(UUID.randomUUID(), UUID.randomUUID()));
    assertTrue(violations.isEmpty());
  }
}
