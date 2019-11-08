package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.LoggedUserWithToken;

import java.util.Collections;
import java.util.Map;

public class LoggedUserWithTokenTableEntryTransformer
  implements TableEntryTransformer<LoggedUserWithToken> {

  @Override
  public LoggedUserWithToken transform(Map<String, String> entry) {
    GbifUser user = new GbifUser();
    user.setFirstName(entry.get("firstName"));
    user.setLastName(entry.get("lastName"));
    user.setUserName(entry.get("userName"));
    user.setEmail(entry.get("email"));

    return LoggedUserWithToken.from(user, null, Collections.emptyList());
  }
}
