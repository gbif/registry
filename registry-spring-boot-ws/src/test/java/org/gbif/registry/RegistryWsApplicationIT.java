package org.gbif.registry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

// TODO: 2019-07-01 move to integration-tests module
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class RegistryWsApplicationIT {

  @Test
  public void contextLoads() {
  }
}
