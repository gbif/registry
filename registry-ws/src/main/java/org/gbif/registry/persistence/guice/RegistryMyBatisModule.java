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

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.crawler.CrawlJob;
import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadRequest;
import org.gbif.api.model.occurrence.PredicateDownloadRequest;
import org.gbif.api.model.occurrence.SqlDownloadRequest;
import org.gbif.api.model.occurrence.predicate.Predicate;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.registry.*;
import org.gbif.api.model.registry.metasync.MetasyncHistory;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.mybatis.guice.MyBatisModule;
import org.gbif.mybatis.type.*;
import org.gbif.registry.doi.DoiPersistenceService;
import org.gbif.registry.doi.DoiType;
import org.gbif.registry.persistence.mapper.*;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;
import org.gbif.registry.persistence.mapper.handler.*;
import org.gbif.registry.persistence.mapper.pipelines.PipelineProcessMapper;
import org.gbif.registry.surety.persistence.ChallengeCodeMapper;
import org.gbif.registry.surety.persistence.ChallengeCodeSupportMapper;
import org.gbif.service.guice.PrivateServiceModule;

import java.net.URI;
import java.util.Properties;
import java.util.UUID;

import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;

/**
 * Sets up the persistence layer using the properties supplied.
 */
public class RegistryMyBatisModule extends PrivateServiceModule {

  public static final TypeLiteral<ChallengeCodeSupportMapper<UUID>> CHALLENGE_CODE_SUPPORT_MAPPER_TYPE_LITERAL =
          new TypeLiteral<ChallengeCodeSupportMapper<UUID>>() {};

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
      failFast(true);
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

      // collections
      addMapperClass(InstitutionMapper.class);
      addMapperClass(CollectionMapper.class);
      addMapperClass(PersonMapper.class);
      addMapperClass(AddressMapper.class);

      //from registry-surety module
      addMapperClass(ChallengeCodeMapper.class);

      // pipelines
      addMapperClass(PipelineProcessMapper.class);

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
      addAlias("DownloadRequest").to(DownloadRequest.class);
      addAlias("PredicateDownloadRequest").to(PredicateDownloadRequest.class);
      addAlias("SqlDownloadRequest").to(SqlDownloadRequest.class);
      addAlias("DatasetOccurrenceDownload").to(DatasetOccurrenceDownloadUsage.class);
      addAlias("DatasetProcessStatus").to(DatasetProcessStatus.class);
      addAlias("CrawlJob").to(CrawlJob.class);
      addAlias("MetasyncHistory").to(MetasyncHistory.class);
      addAlias("DoiData").to(DoiData.class);
      addAlias("DOI").to(DOI.class);
      addAlias("DoiType").to(DoiType.class);
      addAlias("Pageable").to(Pageable.class);
      addAlias("UUID").to(UUID.class);
      addAlias("Country").to(Country.class);
      addAlias("Language").to(Language.class);
      addAlias("Count").to(Facet.Count.class);
      addAlias("Institution").to(Institution.class);
      addAlias("SciCollection").to(Collection.class);
      addAlias("CollectionPerson").to(Person.class);
      addAlias("Address").to(Address.class);

      addAlias("UriTypeHandler").to(UriTypeHandler.class);
      addAlias("UuidTypeHandler").to(UuidTypeHandler.class);
      addAlias("LanguageTypeHandler").to(LanguageTypeHandler.class);
      addAlias("CountryTypeHandler").to(CountryTypeHandler.class);
      addAlias("DownloadStatusTypeHandler").to(OccurrenceDownloadStatusTypeHandler.class);
      addAlias("DoiTypeHandler").to(DOITypeHandler.class);
      addAlias("PredicateTypeHandler").to(PredicateTypeHandler.class);
      addAlias("StringArrayTypeHandler").to(StringArrayTypeHandler.class);
      addAlias("UriArrayTypeHandler").to(UriArrayTypeHandler.class);
      addAlias("DisciplineArrayTypeHandler").to(DisciplineArrayTypeHandler.class);
      addAlias("CollectionContentTypeArrayTypeHandler").to(CollectionContentTypeArrayTypeHandler.class);
      addAlias("PreservationTypeArrayTypeHandler").to(PreservationTypeArrayTypeHandler.class);

      // pipelines aliases
      addAlias("PipelineProcess").to(PipelineProcess.class);
      addAlias("Step").to(PipelineStep.class);
      addAlias("MetricInfoTypeHandler").to(MetricInfoTypeHandler.class);
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
      addTypeHandlerClass(MetricInfoTypeHandler.class);
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
    expose(ChallengeCodeMapper.class);
    expose(InstitutionMapper.class);
    expose(CollectionMapper.class);
    expose(PersonMapper.class);
    expose(AddressMapper.class);
    expose(PipelineProcessMapper.class);

    // Bind the DoiMapper as DoiPersistenceService
    bind(DoiPersistenceService.class).to(DoiMapper.class).in(Scopes.SINGLETON);
    expose(DoiPersistenceService.class);

    //TODO use Named to avoid conflicts with Identity
    bind(CHALLENGE_CODE_SUPPORT_MAPPER_TYPE_LITERAL).to(OrganizationMapper.class).in(Scopes.SINGLETON);
    expose(CHALLENGE_CODE_SUPPORT_MAPPER_TYPE_LITERAL);
  }

}
