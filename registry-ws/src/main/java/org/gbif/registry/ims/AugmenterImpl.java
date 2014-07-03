package org.gbif.registry.ims;

import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Node;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.drupal.mybatis.ImsNodeMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AugmenterImpl implements Augmenter {
  private static Logger LOG = LoggerFactory.getLogger(AugmenterImpl.class);

  private ImsNodeMapper mapper;

  @Inject
  public AugmenterImpl(ImsNodeMapper mapper) {
    this.mapper = mapper;
  }

  private Integer findImsParticipantID(Node node) {
    for (Identifier id : node.getIdentifiers()) {
      if (IdentifierType.GBIF_PARTICIPANT == id.getType()) {
        try {
          return Integer.parseInt(id.getIdentifier());
        } catch (NumberFormatException e) {
          LOG.error("IMS Participant ID is no integer: %s", id.getIdentifier());
        }
      }
    }
    return null;
  }

  @Override
  public Node augment(Node node) {
    if (node != null) {
      try {
        Integer imsId = findImsParticipantID(node);
        if (imsId != null) {
          Node imsNode = mapper.get(imsId);
          if (imsNode != null) {
            // update node with IMS info if it exists
            node.setParticipantTitle(imsNode.getParticipantTitle());
            node.setContacts(imsNode.getContacts());
            node.setAbbreviation(imsNode.getAbbreviation());
            node.setDescription(imsNode.getDescription());
            node.setParticipantSince(imsNode.getParticipantSince());
            node.setOrganization(imsNode.getOrganization());
            node.setAddress(imsNode.getAddress());
            node.setPostalCode(imsNode.getPostalCode());
            node.setCity(imsNode.getCity());
            node.setProvince(imsNode.getProvince());
            node.setEmail(imsNode.getEmail());
            node.setPhone(imsNode.getPhone());
            node.setHomepage(imsNode.getHomepage());
          }
        }
      } catch (Exception e) {
        LOG.error("Failed to augment node %s with IMS information", node.getKey(), e);
      }
    }

    return node;
  }
}
