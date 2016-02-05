package org.gbif.registry.guice;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.directory.Augmenter;

import com.google.inject.AbstractModule;

public class DirectoryMockModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Augmenter.class).to(AugmenterMock.class);
  }

  private static class AugmenterMock implements Augmenter {

    @Override
    public Node augment(Node node) {
      if(node != null) {
        node.setParticipantSince(2001);
        node.setAbbreviation("GBIF.ES");
        node.setCity("Madrid");
        node.setPostalCode("E-28014");
        node.setOrganization("Real Jardín Botánico - CSIC");
        for (int i = 1; i < 8; i++) {
          Contact c = new Contact();
          c.setKey(i);
          c.setLastName("Name " + i);
          c.setType(ContactType.NODE_STAFF);
          node.getContacts().add(c);
        }
      }
      return node;
    }
  }
}
