package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.model.common.GbifUser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class GbifUserTableEntryTransformer implements TableEntryTransformer<GbifUser> {

  @Override
  public GbifUser transform(Map<String, String> entry) {
    GbifUser result = new GbifUser();
    Optional.ofNullable(entry.get("key"))
      .map(Integer::parseInt)
      .ifPresent(result::setKey);
    result.setUserName(entry.get("userName"));
    result.setFirstName(entry.get("firstName"));
    result.setLastName(entry.get("lastName"));
    result.setEmail(entry.get("email"));
    Optional.ofNullable(entry.get("roles"))
      .map(roles -> roles.split(","))
      .map(Arrays::asList)
      .map((Function<List<String>, HashSet>) HashSet::new)
      .ifPresent(result::setRoles);

    return result;
  }
}
