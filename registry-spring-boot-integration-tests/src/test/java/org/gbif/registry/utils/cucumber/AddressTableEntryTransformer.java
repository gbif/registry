package org.gbif.registry.utils.cucumber;

import io.cucumber.datatable.TableEntryTransformer;
import org.gbif.api.model.collections.Address;
import org.gbif.api.vocabulary.Country;

import java.util.Map;
import java.util.Optional;

public class AddressTableEntryTransformer
  implements TableEntryTransformer<Address> {

  @Override
  public Address transform(Map<String, String> entry) {
    Address result = new Address();

    result.setCity(entry.get("city"));
    result.setProvince(entry.get("province"));
    Optional.ofNullable(entry.get("country"))
      .map(Country::valueOf)
      .ifPresent(result::setCountry);
    result.setAddress(entry.get("address"));
    result.setPostalCode(entry.get("postalCode"));

    return result;
  }
}
