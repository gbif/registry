/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.pipelines.issues;

import java.util.List;
import java.util.Set;

import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.Data;

public interface GithubApiClient {
  @PostMapping("/issues")
  void createIssue(@RequestBody Issue issue);

  @GetMapping("/issues")
  List<IssueResult> listIssues(
      @RequestParam("labels") List<String> labels,
      @RequestParam("state") String state,
      @RequestParam("page") int page,
      @RequestParam("per_page") int perPage);

  @PutMapping("/issues/{id}/labels")
  void updateIssueLabels(@PathVariable("id") long id, @RequestBody IssueLabels issueLabels);

  @PostMapping("/issues/{id}/comments")
  void addIssueComment(@PathVariable("id") long id, @RequestBody IssueComment issueComment);

  @Data
  @Builder
  class IssueComment {
    private String body;
  }

  @Data
  @Builder
  class IssueLabels {
    private Set<String> labels;
  }

  @Data
  @Builder
  class Issue {
    @JsonIgnore private long number;
    private String title;
    private String body;
    private Set<String> labels;
  }

  @Data
  class IssueResult {
    private long number;
    private String title;
    private List<Label> labels;
    private List<Assignee> assignees;

    @Data
    public static class Label {
      private String name;
    }

    @Data
    static class Assignee {
      private String login;
    }
  }
}
