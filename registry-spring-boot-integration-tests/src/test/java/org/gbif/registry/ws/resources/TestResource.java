package org.gbif.registry.ws.resources;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.APP_ROLE;

// for security testing
@RestController
@RequestMapping("/test")
public class TestResource {

  @RequestMapping(method = RequestMethod.GET)
  @Secured(ADMIN_ROLE)
  public ResponseEntity<Void> testGet() {
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(method = RequestMethod.POST)
  @Secured(ADMIN_ROLE)
  public ResponseEntity<Void> testPost() {
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @PostMapping("/app")
  @Secured(APP_ROLE)
  public ResponseEntity<Void> testApp() {
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @PostMapping("/app2")
  @Secured(APP_ROLE)
  public ResponseEntity<String> testApp2(@RequestBody final TestRequest testRequest) {
    return ResponseEntity.status(HttpStatus.CREATED).body(testRequest.getValue());
  }

  public static class TestRequest {

    private String value;

    public TestRequest() {
    }

    public TestRequest(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }
}
