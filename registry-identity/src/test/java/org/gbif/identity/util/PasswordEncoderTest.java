package org.gbif.identity.util;

import java.util.Collection;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

//@RunWith(value = Parameterized.class)
public class PasswordEncoderTest {

  private PasswordEncoder encoder = new PasswordEncoder();
  private String password;
  private String hash;

//  public PasswordEncoderTest(String password, String hash) {
//    this.password = password;
//    this.hash = hash;
//  }

//  @Parameterized.Parameters
//	 public static Collection<Object[]> testData() {
//    return Lists.newArrayList(
//      new Object[]{"adrian", "$S$DNbBTrkalsPChLsqajHUQS18pBBxzSTQW0310SzivTy7HDQ.zgyG"},
//      new Object[]{"test", "$S$DxVn7wubSRzoK9X2pkGx4njeDRkLEgdqPphc2ZXkkb8Viy8JEGf3"},
//      new Object[]{"markus", "$S$Dy6/BMI3AoImMSlZGHhEkXgKbUenX7yS2SNgmj7NsrA9JPqH01kW"},
//      new Object[]{"carla", "$S$DN6evrfX6JzGw./kDKHhON8VEUp7tDbF0gyZYhX1Uw4Y3udFN47w"},
//      new Object[]{"pia", "$S$DnuHhGOmUVLBYF.kXnE9ZD6ffqnFz7GPxWaikVFLh1JAymnJkdB3"},
//      new Object[]{"password1", "$S$D1UoWp.wjyDqA1oxwy/MjuSuQnYWsRtSzJGF4vVzdKAN1eh9sIVd"}
//    );
//	 }

  /**
   * Verify that preencoded passwords can be encoded again (e.g. for auth).
   */
  @Test
  public void testPreEncoded() throws Exception {
    String encoded = encoder.encode(password, hash);
    assertEquals(hash, encoded);
  }

  /**
   * Verify that passwords can be re-encoded again when we randomise the salting (e.g. for user creation).
   */
  @Test
  public void testWithSalting() throws Exception {
    String encoded1 = encoder.encode(password); // encode it which will generate a random salt
    String encoded2 = encoder.encode(password, encoded1); // encode again reading the salt genarate above
    assertEquals(encoded1, encoded2); // verify they
  }

  @Test
  public void test() {
    System.out.println(encoder.encode("MejJ2kqKB9PL77p4"));
    System.out.println(encoder.encode("MejJ2kqKB9PL77p4"));
  }

}
