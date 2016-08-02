package org.gbif.registry.cli.directoryupdate.service;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.directory.Node;
import org.gbif.api.service.directory.NodeService;

import java.util.List;

/**
 *
 */
public class NodeServiceMock implements NodeService {

  private List<Node> nodes;

  public void setNodes(List<Node> nodes){
    this.nodes = nodes;
  }

  @Override
  public Node create(Node node) {
    return null;
  }

  @Override
  public Node get(Integer integer) {
    return null;
  }

  @Override
  public void update(Node node) {

  }

  @Override
  public void delete(Integer integer) {

  }

  @Override
  public PagingResponse<Node> list(String q, Pageable pageable) {
    return new PagingResponse<Node>(new PagingRequest(), (long)nodes.size(), nodes);
  }

}
