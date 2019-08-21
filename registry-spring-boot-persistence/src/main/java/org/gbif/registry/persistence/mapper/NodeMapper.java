package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.Country;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * For simplicity we keep ContactableMapper part of the BaseNetworkEntityMapper, but this NodeMapper
 * does not implement those mapper methods but will throw exceptions instead !
 * For a Node all contacts are managed in the GBIF Filemaker IMS which we only access for reads
 * and cannot manipulate though our Java API.
 */
public interface NodeMapper extends BaseNetworkEntityMapper<Node> {

  List<Country> listNodeCountries();

  List<Country> listActiveCountries();

  Node getByCountry(@Param("country") Country country);

  /**
   * This method is not supported by the NodeMapper.
   *
   * @throws
   */
  @Override
  int addContact(UUID entityKey, int contactKey, ContactType contactType, boolean isPrimary);

  /**
   * This method is not supported by the NodeMapper.
   *
   * @throws
   */
  @Override
  void updatePrimaryContacts(UUID entityKey, ContactType contactType);

  /**
   * This method is not supported by the NodeMapper.
   *
   * @throws
   */
  @Override
  int deleteContact(UUID entityKey, int contactKey);

  /**
   * A simple suggest by title service.
   */
  List<KeyTitleResult> suggest(@Nullable @Param("q") String q);
}
