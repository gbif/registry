/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.persistence.guice;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.crawler.CrawlJob;
import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.predicate.Predicate;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.metasync.MetasyncHistory;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.mybatis.guice.MyBatisModule;
import org.gbif.mybatis.type.CountryTypeHandler;
import org.gbif.mybatis.type.LanguageTypeHandler;
import org.gbif.mybatis.type.StringArrayTypeHandler;
import org.gbif.mybatis.type.UriArrayTypeHandler;
import org.gbif.mybatis.type.UriTypeHandler;
import org.gbif.mybatis.type.UuidTypeHandler;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.ContactMapper;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.DatasetOccurrenceDownloadMapper;
import org.gbif.registry.persistence.mapper.DatasetProcessStatusMapper;
import org.gbif.registry.persistence.mapper.DoiMapper;
import org.gbif.registry.persistence.mapper.EndpointMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.MetadataMapper;
import org.gbif.registry.persistence.mapper.MetasyncHistoryMapper;
import org.gbif.registry.persistence.mapper.NetworkMapper;
import org.gbif.registry.persistence.mapper.NodeMapper;
import org.gbif.registry.persistence.mapper.OccurrenceDownloadMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.UserRightsMapper;
import org.gbif.registry.persistence.mapper.handler.DOITypeHandler;
import org.gbif.registry.persistence.mapper.handler.OccurrenceDownloadStatusTypeHandler;
import org.gbif.registry.persistence.mapper.handler.PredicateTypeHandler;
import org.gbif.service.guice.PrivateServiceModule;

import java.net.URI;
import java.util.Properties;
import java.util.UUID;

/**
 * Sets up the persistence layer using the properties supplied.
 */
public class RegistryMyBatisModule extends PrivateServiceModule {

  /**
   * Sets up the MyBatis structure. Note that MyBatis Guice uses named injection parameters (e.g. JDBC.url), and they
   * are filtered and bound in the enclosing class.
   */
  public static class InternalRegistryServiceMyBatisModule extends MyBatisModule {

    public static final String DATASOURCE_BINDING_NAME = "registry";

    public InternalRegistryServiceMyBatisModule(Properties props) {
      super(DATASOURCE_BINDING_NAME, props);
    }

    @Override
    protected void bindManagers() {
    }

    @Override
    protected void bindMappers() {
      // network entities
      addMapperClass(NodeMapper.class);
      addMapperClass(OrganizationMapper.class);
      addMapperClass(InstallationMapper.class);
      addMapperClass(DatasetMapper.class);
      addMapperClass(NetworkMapper.class);

      // components
      addMapperClass(ContactMapper.class);
      addMapperClass(EndpointMapper.class);
      addMapperClass(MachineTagMapper.class);
      addMapperClass(TagMapper.class);
      addMapperClass(IdentifierMapper.class);
      addMapperClass(CommentMapper.class);
      addMapperClass(MetadataMapper.class);
      addMapperClass(OccurrenceDownloadMapper.class);
      addMapperClass(DatasetOccurrenceDownloadMapper.class);
      addMapperClass(DatasetProcessStatusMapper.class);
      addMapperClass(MetasyncHistoryMapper.class);
      addMapperClass(UserRightsMapper.class);
      addMapperClass(DoiMapper.class);


      // reduce mapper verboseness with aliases
      addAlias("Node").to(Node.class);
      addAlias("Organization").to(Organization.class);
      addAlias("Installation").to(Installation.class);
      addAlias("Dataset").to(Dataset.class);
      addAlias("Network").to(Network.class);

      addAlias("Citation").to(Citation.class);
      addAlias("Contact").to(Contact.class);
      addAlias("Endpoint").to(Endpoint.class);
      addAlias("MachineTag").to(MachineTag.class);
      addAlias("Tag").to(Tag.class);
      addAlias("Identifier").to(Identifier.class);
      addAlias("Comment").to(Comment.class);
      addAlias("Metadata").to(Metadata.class);
      addAlias("Download").to(Download.class);
      addAlias("DatasetOccurrenceDownload").to(DatasetOccurrenceDownloadUsage.class);
      addAlias("DatasetProcessStatus").to(DatasetProcessStatus.class);
      addAlias("CrawlJob").to(CrawlJob.class);
      addAlias("MetasyncHistory").to(MetasyncHistory.class);
      addAlias("DoiData").to(DoiData.class);
      addAlias("DOI").to(DOI.class);
      addAlias("Pageable").to(Pageable.class);
      addAlias("UUID").to(UUID.class);
      addAlias("Country").to(Country.class);
      addAlias("Language").to(Language.class);

      addAlias("UuidTypeHandler").to(UuidTypeHandler.class);
      addAlias("LanguageTypeHandler").to(LanguageTypeHandler.class);
      addAlias("CountryTypeHandler").to(CountryTypeHandler.class);
      addAlias("DownloadStatusTypeHandler").to(OccurrenceDownloadStatusTypeHandler.class);
      addAlias("DoiTypeHandler").to(DOITypeHandler.class);
      addAlias("PredicateTypeHandler").to(PredicateTypeHandler.class);
      addAlias("StringArrayTypeHandler").to(StringArrayTypeHandler.class);
      addAlias("UriArrayTypeHandler").to(UriArrayTypeHandler.class);
    }

    @Override
    protected void bindTypeHandlers() {
      handleType(UUID.class).with(UuidTypeHandler.class);
      handleType(URI.class).with(UriTypeHandler.class);
      handleType(Country.class).with(CountryTypeHandler.class);
      handleType(Language.class).with(LanguageTypeHandler.class);
      handleType(Download.Status.class).with(OccurrenceDownloadStatusTypeHandler.class);
      handleType(DOI.class).with(DOITypeHandler.class);
      handleType(Predicate.class).with(PredicateTypeHandler.class);
    }
  }

  private static final String PREFIX = "registry.db.";
  private final Properties properties;

  public RegistryMyBatisModule(Properties properties) {
    super(PREFIX, properties);
    this.properties = properties;
  }

  @Override
  protected void configureService() {
    MyBatisModule internalModule = new InternalRegistryServiceMyBatisModule(getProperties());
    install(internalModule); // the named parameters are already configured at this stage
    expose(internalModule.getDatasourceKey()); // to avoid clashes between multiple datasources
    // The Mappers are exposed to be injected in the ws resources
    expose(NodeMapper.class);
    expose(OrganizationMapper.class);
    expose(InstallationMapper.class);
    expose(DatasetMapper.class);
    expose(NetworkMapper.class);
    expose(ContactMapper.class);
    expose(EndpointMapper.class);
    expose(MachineTagMapper.class);
    expose(TagMapper.class);
    expose(IdentifierMapper.class);
    expose(CommentMapper.class);
    expose(MetadataMapper.class);
    expose(OccurrenceDownloadMapper.class);
    expose(DatasetOccurrenceDownloadMapper.class);
    expose(DatasetProcessStatusMapper.class);
    expose(MetasyncHistoryMapper.class);
    expose(UserRightsMapper.class);
    expose(DoiMapper.class);
  }

}
