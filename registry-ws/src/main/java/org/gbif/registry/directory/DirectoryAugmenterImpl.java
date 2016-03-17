package org.gbif.registry.directory;

import org.gbif.api.model.directory.NodePerson;
import org.gbif.api.model.directory.Participant;
import org.gbif.api.model.directory.ParticipantPerson;
import org.gbif.api.model.directory.Person;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Node;
import org.gbif.api.service.directory.NodeService;
import org.gbif.api.service.directory.ParticipantService;
import org.gbif.api.service.directory.PersonService;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.IdentifierType;

import java.net.URI;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DirectoryAugmenterImpl implements Augmenter {



  private static Logger LOG = LoggerFactory.getLogger(DirectoryAugmenterImpl.class);

  private ParticipantService participantService;
  private NodeService nodeService;
  private PersonService personService;

  @Inject
  public DirectoryAugmenterImpl(ParticipantService participantService, NodeService nodeService, PersonService personService) {
    this.participantService = participantService;
    this.nodeService = nodeService;
    this.personService = personService;
  }

  /**
   * Gets the participantID from the Node.
   */
  private static Integer findParticipantID(Node node) {
    for (Identifier id : node.getIdentifiers()) {
      if (IdentifierType.GBIF_PARTICIPANT == id.getType()) {
        try {
          return Integer.parseInt(id.getIdentifier());
        } catch (NumberFormatException e) {
          LOG.error("Directory participantId is no integer: {}", id.getIdentifier());
        }
      }
    }
    return null;
  }

  @Override
  public Node augment(Node registryNode) {
    if (registryNode != null) {
      try {
        Integer participantID = findParticipantID(registryNode);
        if (participantID != null) {
          Participant participant = participantService.get(participantID);
          if (participant != null) {
            List<Contact> contacts = Lists.newArrayList();
            // update node with Directory info if it exists
            List<org.gbif.api.model.directory.Node> participantNodes = getParticipantNodes(participant);
            registryNode.setParticipantTitle(participant.getName());
            contacts.addAll(getContactsForParticipant(participant));

            registryNode.setAbbreviation(participant.getAbbreviatedName());
            registryNode.setDescription(participant.getComments());
            if(participant.getMembershipStart() != null) {
              registryNode.setParticipantSince(getParticipantSinceYear(participant.getMembershipStart()));
            }
            if(!participantNodes.isEmpty()){
              contacts.addAll(getContactsForNode(participantNodes));
              registryNode.setAddress(getNodesAddresses(participantNodes));
              registryNode.setHomepage(getWebUrls(participant,participantNodes));
              registryNode.setEmail(getEmails(participantNodes));
              registryNode.setPhone(getPhones(participantNodes));
            }
            else{
              LOG.info("Empty node for participantId {}", participantID);
            }
            registryNode.setContacts(contacts);
          }
        }
      } catch (Exception e) {
        LOG.error("Failed to augment node {} with Directory information", registryNode.getKey(), e);
      }
    }
    return registryNode;
  }

  /**
   * Gets the year part of the membershipStart field.
   */
  private static Integer getParticipantSinceYear(String membershipStart) {
    String[] membershipStartComponents = membershipStart.split(" ");
    if(membershipStartComponents.length > 1) {
      return Ints.tryParse(membershipStartComponents[1].trim());
    }
    return null;
  }

  /**
   * Gets all the nodes associated to a participant.
   */
  private List<org.gbif.api.model.directory.Node> getParticipantNodes(Participant participant){
    List<org.gbif.api.model.directory.Node> nodes = Lists.newArrayList();
    if(participant.getNodes() != null){
      for(org.gbif.api.model.directory.Node node : participant.getNodes()) {
        nodes.add(nodeService.get(node.getId()));
      }
    }
    return nodes;
  }

  /**
   * Gets all the addresses associated to a participant.
   */
  private static List<String> getNodesAddresses(List<org.gbif.api.model.directory.Node> participantNodes){
    List<String> addresses = Lists.newArrayList();
    for(org.gbif.api.model.directory.Node node : participantNodes) {
      addresses.add(node.getAddress());
    }
    return  addresses;
  }

  /**
   * Gets all the web pages/urls of participant and its nodes.
   */
  private static List<URI> getWebUrls(Participant participant, List<org.gbif.api.model.directory.Node> participantNodes){
    List<URI> addresses = Lists.newArrayList();
    if(participant.getParticipantUrl() != null) {
      addresses.add(URI.create(participant.getParticipantUrl()));
    }
    for(org.gbif.api.model.directory.Node node : participantNodes) {
      if(node.getNodeUrl() != null) {
        addresses.add(URI.create(node.getNodeUrl()));
      }
    }
    return  addresses;
  }

  /**
   * Gets all the emails from the nodes list.
   */
  private static List<String> getEmails(List<org.gbif.api.model.directory.Node> participantNodes){
    List<String> emails = Lists.newArrayList();
    for(org.gbif.api.model.directory.Node node : participantNodes) {
      if(node.getEmail() != null) {
        emails.add(node.getEmail());
      }
    }
    return  emails;
  }

  /**
   * Gets all the phones numbers from the nodes list.
   */
  private static List<String> getPhones(List<org.gbif.api.model.directory.Node> participantNodes){
    List<String> phones = Lists.newArrayList();
    for(org.gbif.api.model.directory.Node node : participantNodes) {
      if(node.getPhone() != null) {
        phones.add(node.getPhone());
      }
    }
    return  phones;
  }

  /**
   * Transforms the persons associated to a participant into a list of contacts.
   */
  private List<Contact> getContactsForParticipant(Participant participant){
    List<Contact> contacts = Lists.newArrayList();
    if(participant.getPeople() != null){
      Person person;
      Contact contact;
      ContactType contactType;
      for(ParticipantPerson participantPerson : participant.getPeople()) {
        person = personService.get(participantPerson.getPersonId());
        contactType = null;
        if( participantPerson.getRole() != null) {
          contactType = DirectoryRegistryConstantsMapping.PARTICIPANT_ROLE_TO_CONTACT_TYPE.get(participantPerson.getRole());
        }
        contact = personToContact(person, participant.getName(), contactType);
        contacts.add(contact);
      }
    }
    return contacts;
  }

  /**
   * Transforms the persons associated to a node(s) into a list of contacts.
   * @param directoryNodes it theory it should never be more than one
   */
  private List<Contact> getContactsForNode(List<org.gbif.api.model.directory.Node> directoryNodes){
    List<Contact> contacts = Lists.newArrayList();
    if(directoryNodes != null){
      Person person;
      Contact contact;
      ContactType contactType;
      for(org.gbif.api.model.directory.Node currentNode : directoryNodes) {
        if(currentNode.getPeople() != null && !currentNode.getPeople().isEmpty()) {
          for (NodePerson nodePerson : currentNode.getPeople()) {
            person = personService.get(nodePerson.getPersonId());
            contactType = null;
            if (nodePerson.getRole() != null) {
              contactType = DirectoryRegistryConstantsMapping.NODE_ROLE_TO_CONTACT_TYPE.get(nodePerson.getRole());
            }
            contact = personToContact(person, currentNode.getName(), contactType);
            contacts.add(contact);
          }
        }
        else{
          LOG.info("No people found for nodeId {}", currentNode.getId());
        }
      }
    }
    return contacts;
  }

  /**
   * Transform a Directory Person into a Registry Contact.
   * @param person
   * @param organization
   * @param contactType
   * @return
   */
  private Contact personToContact(Person person, String organization, ContactType contactType){
    Contact contact = new Contact();
    contact.setKey(person.getId());
    contact.addAddress(person.getAddress());
    contact.setCountry(person.getCountryCode());
    if(person.getEmail() != null) {
      contact.addEmail(person.getEmail());
    }
    contact.setFirstName(person.getFirstName());
    contact.setLastName(person.getSurname());
    if(person.getPhone() != null) {
      contact.addPhone(person.getPhone());
    }
    contact.setModified(person.getModified());
    contact.setOrganization(organization);
    if( contactType != null) {
      contact.setType(contactType);
    }
    return contact;
  }
}
