/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.identity.util;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegistryPasswordEncoderTest {

  private final RegistryPasswordEncoder encoder = new RegistryPasswordEncoder();

  public static Stream<Arguments> testData() {
    return Stream.of(
        Arguments.of("adrian", "$S$DNbBTrkalsPChLsqajHUQS18pBBxzSTQW0310SzivTy7HDQ.zgyG"),
        Arguments.of("test", "$S$DxVn7wubSRzoK9X2pkGx4njeDRkLEgdqPphc2ZXkkb8Viy8JEGf3"),
        Arguments.of("markus", "$S$Dy6/BMI3AoImMSlZGHhEkXgKbUenX7yS2SNgmj7NsrA9JPqH01kW"),
        Arguments.of("carla", "$S$DN6evrfX6JzGw./kDKHhON8VEUp7tDbF0gyZYhX1Uw4Y3udFN47w"),
        Arguments.of("pia", "$S$DnuHhGOmUVLBYF.kXnE9ZD6ffqnFz7GPxWaikVFLh1JAymnJkdB3"),
        Arguments.of("password1", "$S$D1UoWp.wjyDqA1oxwy/MjuSuQnYWsRtSzJGF4vVzdKAN1eh9sIVd"));
  }

  /** Verify that preencoded passwords can be encoded again (e.g. for auth). */
  @ParameterizedTest
  @MethodSource("testData")
  public void testPreEncoded(String password, String hash) {
    String encoded = encoder.encode(password, hash);
    assertEquals(hash, encoded);
  }

  /**
   * Verify that passwords can be re-encoded again when we randomise the salting (e.g. for user
   * creation).
   */
  @ParameterizedTest
  @MethodSource("testData")
  public void testWithSalting(String password, String hash) {
    String encoded1 = encoder.encode(password); // encode it which will generate a random salt
    String encoded2 =
        encoder.encode(password, encoded1); // encode again reading the salt genarate above
    assertEquals(encoded1, encoded2); // verify they
  }

  @Test
  public void test() {
    System.out.println(encoder.encode("P1p3r!!"));
  }
}
