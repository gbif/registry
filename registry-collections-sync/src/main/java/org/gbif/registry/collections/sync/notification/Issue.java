package org.gbif.registry.collections.sync.notification;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Issue {
  private String title;
  private String body;
  private List<String> assignees;
  private List<String> labels;
}
