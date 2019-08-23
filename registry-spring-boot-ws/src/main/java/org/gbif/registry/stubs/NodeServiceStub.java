package org.gbif.registry.stubs;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.directory.Node;
import org.gbif.api.service.directory.NodeService;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@Service
public class NodeServiceStub implements NodeService {
  @Override
  public Node create(@NotNull Node node) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public Node get(@NotNull Integer integer) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void update(@NotNull Node node) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void delete(@NotNull Integer integer) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public PagingResponse<Node> list(@Nullable String s, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented yet");
  }
}
