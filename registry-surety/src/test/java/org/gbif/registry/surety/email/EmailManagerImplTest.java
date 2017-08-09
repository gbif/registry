package org.gbif.registry.surety.email;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.mail.Address;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Unit tests related to {@link EmailManagerImpl}.
 */
public class EmailManagerImplTest {


  @Test
  public void testGenerateBccArray() {
    Set<Address> bccAddresses = new HashSet<>();
    BaseEmailModel baseEmailModel = new BaseEmailModel( "email@b.com", "subject", "body",
            Collections.singletonList("b@b.com"));

    bccAddresses.add(EmailManagerImpl.toAddress("a@b.com").orElse(null));
    Address[] bccAddressesArray = EmailManagerImpl.generateBccArray(bccAddresses, baseEmailModel);

    assertEquals(2, bccAddressesArray.length);
  }
}
