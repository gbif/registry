package org.gbif.registry.guice;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.service.common.UserService;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.drupal.mybatis.ImsNodeMapper;

import com.google.inject.AbstractModule;

/**
 * Mocks the regular DrupalMyBatisModule.
 */
public class DrupalMockModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(UserService.class).to(UserServiceMock.class);
      bind(ImsNodeMapper.class).to(ImsNodeMapperMock.class);
    }

  private static class ImsNodeMapperMock implements ImsNodeMapper {
    @Override
    public Node get(Integer integer) {
      Node mock = new Node();
      mock.setParticipantSince(2001);
      mock.setAbbreviation("GBIF.ES");
      mock.setCity("Madrid");
      mock.setPostalCode("E-28014");
      mock.setOrganization("Real Jardín Botánico - CSIC");
      for (int i = 1; i<8; i++) {
        Contact c = new Contact();
        c.setKey(i);
        c.setLastName("Name " + i);
        c.setType(ContactType.NODE_STAFF);
        mock.getContacts().add(c);
      }
      return mock;
    }
  }
}

