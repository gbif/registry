package org.gbif.registry.metasync.protocols.biocase.model.capabilities;

import org.gbif.registry.metasync.protocols.biocase.model.BiocaseConcept;

import java.net.URI;
import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetNext;
import org.apache.commons.digester3.annotations.rules.SetProperty;

@ObjectCreate(pattern = "response/content/capabilities/SupportedSchemas")
public class SupportedSchema {

  @SetProperty(pattern = "response/content/capabilities/SupportedSchemas", attributeName = "namespace")
  private URI namespace;
  @SetProperty(pattern = "response/content/capabilities/SupportedSchemas", attributeName = "request")
  private boolean request;
  @SetProperty(pattern = "response/content/capabilities/SupportedSchemas", attributeName = "response")
  private boolean response;
  private List<BiocaseConcept> concepts = Lists.newArrayList();

  public URI getNamespace() {
    return namespace;
  }

  public void setNamespace(URI namespace) {
    this.namespace = namespace;
  }

  public boolean isRequest() {
    return request;
  }

  public void setRequest(boolean request) {
    this.request = request;
  }

  public boolean isResponse() {
    return response;
  }

  public void setResponse(boolean response) {
    this.response = response;
  }

  public List<BiocaseConcept> getConcepts() {
    return concepts;
  }

  public void setConcepts(List<BiocaseConcept> concepts) {
    this.concepts = concepts;
  }

  @SetNext
  public void addConcept(BiocaseConcept concept) {
    concepts.add(concept);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("namespace", namespace)
      .add("request", request)
      .add("response", response)
      .add("concepts", concepts)
      .toString();
  }

}
