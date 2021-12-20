package org.gbif.registry.events.collections;

import org.gbif.api.model.collections.MasterSourceMetadata;

import java.util.UUID;

/** Event created to sync the entities when a new master source metadata is added to them. */
public class MasterSourceMetadataAddedEvent {

  private final UUID collectionEntityKey;
  private final MasterSourceMetadata metadata;

  public static MasterSourceMetadataAddedEvent newInstance(
      UUID collectionEntityKey, MasterSourceMetadata metadata) {
    return new MasterSourceMetadataAddedEvent(collectionEntityKey, metadata);
  }

  private MasterSourceMetadataAddedEvent(UUID collectionEntityKey, MasterSourceMetadata metadata) {
    this.collectionEntityKey = collectionEntityKey;
    this.metadata = metadata;
  }

  public UUID getCollectionEntityKey() {
    return collectionEntityKey;
  }

  public MasterSourceMetadata getMetadata() {
    return metadata;
  }
}
