package org.gbif.registry.persistence.mapper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for representing entity-concept relationships during vocabulary updates.
 * Used to preserve and restore links between institutions/collections and concepts.
 * Uses vocabulary concept keys for direct mapping.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrsciCollConceptLinkDto {
  private UUID entityKey;
  private Long conceptKey;
  private String conceptName;
}
