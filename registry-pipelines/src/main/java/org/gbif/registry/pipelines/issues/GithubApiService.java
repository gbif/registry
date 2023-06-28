package org.gbif.registry.pipelines.issues;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.Data;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface GithubApiService {
  @POST("issues")
  Call<Void> createIssue(@Body Issue issue);

  @GET("issues")
  Call<List<IssueResult>> listIssues(
      @Query("labels") List<String> labels,
      @Query("state") String state,
      @Query("page") int page,
      @Query("per_page") int perPage);

  @PATCH("issues/{id}")
  Call<Void> updateIssueLabels(@Path("id") long id, @Body IssueLabels issueLabels);

  @POST("issues/{id}/comments")
  Call<Void> addIssueComment(@Path("id") long id, @Body IssueComment issueComment);

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
