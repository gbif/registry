package org.gbif.registry.cli.directoryupdate.mapper;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.persistence.mapper.NodeMapper;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import com.beust.jcommander.internal.Lists;
import org.apache.ibatis.annotations.Param;

/**
 *
 */
public class RegistryNodeMockMapper implements NodeMapper {

  private List<Node> nodes = Lists.newArrayList();
  private List<Node> updated = Lists.newArrayList();
  private List<Node> created = Lists.newArrayList();

  public void setNodes(List<Node> nodes){
    this.nodes = nodes;
  }

  @Override
  public List<Country> listNodeCountries() {
    return null;
  }

  @Override
  public List<Country> listActiveCountries() {
    return null;
  }

  @Override
  public Node getByCountry(@Param("country") Country country) {
    return null;
  }

  @Override
  public int addContact(UUID entityKey, int contactKey, ContactType contactType, boolean isPrimary) {
    return 0;
  }

  @Override
  public void updatePrimaryContacts(UUID entityKey, ContactType contactType) {

  }

  @Override
  public void updateContact(@Param("targetEntityKey") UUID entityKey, @Param("contactKey") Integer contactKey, @Param("type") ContactType contactType, @Param("primary") boolean primary) {

  }

  @Override
  public int deleteContact(UUID entityKey, int contactKey) {
    return 0;
  }

  @Override
  public List<KeyTitleResult> suggest(@Nullable String q) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int deleteContacts(@Param("targetEntityKey") UUID entityKey) {
    return 0;
  }

  @Override
  public List<Contact> listContacts(@Param("targetEntityKey") UUID targetEntityKey) {
    return null;
  }

  @Override
  public Boolean areRelated(@Param("targetEntityKey") UUID targetEntityKey, @Param("contactKey") int contactKey) {
    return null;
  }

  @Override
  public int addComment(@Param("targetEntityKey") UUID entityKey, @Param("commentKey") int commentKey) {
    return 0;
  }

  @Override
  public int deleteComment(@Param("targetEntityKey") UUID entityKey, @Param("commentKey") int commentKey) {
    return 0;
  }

  @Override
  public List<Comment> listComments(@Param("targetEntityKey") UUID identifierKey) {
    return null;
  }

  @Override
  public int addEndpoint(@Param("targetEntityKey") UUID entityKey, @Param("endpointKey") int endpointKey) {
    return 0;
  }

  @Override
  public int deleteEndpoint(@Param("targetEntityKey") UUID entityKey, @Param("endpointKey") int endpointKey) {
    return 0;
  }

  @Override
  public List<Endpoint> listEndpoints(@Param("targetEntityKey") UUID targetEntityKey) {
    return null;
  }

  @Override
  public int addIdentifier(@Param("targetEntityKey") UUID entityKey, @Param("identifierKey") int identifierKey) {
    return 0;
  }

  @Override
  public int deleteIdentifier(@Param("targetEntityKey") UUID entityKey, @Param("identifierKey") int identifierKey) {
    return 0;
  }

  @Override
  public List<Identifier> listIdentifiers(@Param("targetEntityKey") UUID identifierKey) {
    return null;
  }

  @Override
  public int addMachineTag(@Param("targetEntityKey") UUID entityKey, @Param("machineTagKey") int machineTagKey) {
    return 0;
  }

  @Override
  public int deleteMachineTag(@Param("targetEntityKey") UUID entityKey, @Param("machineTagKey") int machineTagKey) {
    return 0;
  }

  @Override
  public int deleteMachineTags(@Param("targetEntityKey") UUID entityKey, @Param("namespace") String namespace, @Param("name") String name) {
    return 0;
  }

  @Override
  public List<MachineTag> listMachineTags(@Param("targetEntityKey") UUID targetEntityKey) {
    return null;
  }

  @Override
  public long countByMachineTag(@Nullable @Param("namespace") String namespace, @Param("name") String name, @Param("value") String value) {
    return 0;
  }

  @Override
  public List listByMachineTag(@Nullable @Param("namespace") String namespace, @Param("name") String name, @Param("value") String value, Pageable page) {
    return null;
  }

  @Override
  public Node get(@Param("key") UUID key) {
    return null;
  }

  @Override
  public String title(@Param("key") UUID key) {
    return null;
  }

  @Override
  public void create(Node entity) {
    created.add(entity);
  }

  @Override
  public void delete(@Param("key") UUID key) {

  }

  @Override
  public void update(Node entity) {
    updated.add(entity);
  }

  @Override
  public List<Node> list(@Nullable @Param("page") Pageable page) {
    return nodes;
  }

  @Override
  public List<Node> search(@Nullable @Param("query") String query, @Nullable @Param("page") Pageable page) {
    return null;
  }

  @Override
  public int count() {
    return 0;
  }

  @Override
  public int count(@Nullable @Param("query") String query) {
    return 0;
  }

  @Override
  public long countByIdentifier(@Nullable @Param("type") IdentifierType type, @Param("identifier") String identifier) {
    return 0;
  }

  @Override
  public List<Node> listByIdentifier(@Nullable @Param("type") IdentifierType type, @Param("identifier") String identifier, @Param("page") Pageable page) {
    return null;
  }

  @Override
  public int addTag(@Param("targetEntityKey") UUID entityKey, @Param("tagKey") int tagKey) {
    return 0;
  }

  @Override
  public int deleteTag(@Param("targetEntityKey") UUID entityKey, @Param("tagKey") int tagKey) {
    return 0;
  }

  @Override
  public List<Tag> listTags(@Param("targetEntityKey") UUID targetEntityKey) {
    return null;
  }

  /**
   * Return updated nodes in context of mock and tests
   * @return
   */
  public List<Node> getUpdatedNodes(){
    return updated;
  }

  /**
   * Return created nodes in context of mock and tests
   */
  public List<Node> getCreatedNodes(){
    return created;
  }
}
