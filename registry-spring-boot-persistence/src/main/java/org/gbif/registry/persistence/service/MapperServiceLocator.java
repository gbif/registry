package org.gbif.registry.persistence.service;

import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.ContactMapper;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.DatasetProcessStatusMapper;
import org.gbif.registry.persistence.mapper.EndpointMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.MetadataMapper;
import org.gbif.registry.persistence.mapper.MetasyncHistoryMapper;
import org.gbif.registry.persistence.mapper.NetworkMapper;
import org.gbif.registry.persistence.mapper.NodeMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class MapperServiceLocator {

  private final ApplicationContext appContext;

  public MapperServiceLocator(ApplicationContext appContext) {
    this.appContext = appContext;
  }

  public ContactMapper getContactMapper() {
    return appContext.getBean(MapperType.CONTACT.getName(), ContactMapper.class);
  }

  public OrganizationMapper getOrganizationMapper() {
    return appContext.getBean(MapperType.ORGANIZATION.getName(), OrganizationMapper.class);
  }

  public EndpointMapper getEndpointMapper() {
    return appContext.getBean(MapperType.ENDPOINT.getName(), EndpointMapper.class);
  }

  public MachineTagMapper getMachineTagMapper() {
    return appContext.getBean(MapperType.MACHINE_TAG.getName(), MachineTagMapper.class);
  }

  public TagMapper getTagMapper() {
    return appContext.getBean(MapperType.TAG.getName(), TagMapper.class);
  }

  public IdentifierMapper getIdentifierMapper() {
    return appContext.getBean(MapperType.IDENTIFIER.getName(), IdentifierMapper.class);
  }

  public CommentMapper getCommentMapper() {
    return appContext.getBean(MapperType.COMMENT.getName(), CommentMapper.class);
  }

  public DatasetMapper getDatasetMapper() {
    return appContext.getBean(MapperType.DATASET.getName(), DatasetMapper.class);
  }

  public InstallationMapper getInstallationMapper() {
    return appContext.getBean(MapperType.INSTALLATION.getName(), InstallationMapper.class);
  }

  public NodeMapper getNodeMapper() {
    return appContext.getBean(MapperType.NODE.getName(), NodeMapper.class);
  }

  public NetworkMapper getNetworkMapper() {
    return appContext.getBean(MapperType.NETWORK.getName(), NetworkMapper.class);
  }

  public MetadataMapper getMetadataMapper() {
    return appContext.getBean(MapperType.METADATA.getName(), MetadataMapper.class);
  }

  public DatasetProcessStatusMapper getDatasetProcessStatusMapper() {
    return appContext.getBean(MapperType.DATASET_PROCESS_STATUS.getName(), DatasetProcessStatusMapper.class);
  }

  public MetasyncHistoryMapper getMetasyncHistoryMapper() {
    return appContext.getBean(MapperType.METASYNC_HISTORY_MAPPER.getName(), MetasyncHistoryMapper.class);
  }
}
