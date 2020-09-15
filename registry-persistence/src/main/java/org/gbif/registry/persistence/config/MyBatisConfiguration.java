/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.persistence.config;

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
import org.gbif.api.model.occurrence.predicate.Predicate;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
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
import org.gbif.mybatis.type.CountryTypeHandler;
import org.gbif.mybatis.type.LanguageTypeHandler;
import org.gbif.mybatis.type.StringArrayTypeHandler;
import org.gbif.mybatis.type.UriArrayTypeHandler;
import org.gbif.mybatis.type.UriTypeHandler;
import org.gbif.mybatis.type.UuidTypeHandler;
import org.gbif.registry.domain.doi.DoiType;
import org.gbif.registry.domain.ws.DerivedDataset;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;
import org.gbif.registry.persistence.mapper.auxhandler.AlternativeCodesTypeHandler;
import org.gbif.registry.persistence.mapper.auxhandler.CollectionSummaryTypeHandler;
import org.gbif.registry.persistence.mapper.handler.CollectionContentTypeArrayTypeHandler;
import org.gbif.registry.persistence.mapper.handler.DOITypeHandler;
import org.gbif.registry.persistence.mapper.handler.DisciplineArrayTypeHandler;
import org.gbif.registry.persistence.mapper.handler.LocaleTypeHandler;
import org.gbif.registry.persistence.mapper.handler.MetricInfoTypeHandler;
import org.gbif.registry.persistence.mapper.handler.OccurrenceDownloadStatusTypeHandler;
import org.gbif.registry.persistence.mapper.handler.PredicateTypeHandler;
import org.gbif.registry.persistence.mapper.handler.PreservationTypeArrayTypeHandler;
import org.gbif.registry.persistence.mapper.handler.StepTypeArrayTypeHandler;

import java.net.URI;
import java.util.UUID;

import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisConfiguration {

  @Bean
  ConfigurationCustomizer mybatisConfigCustomizer() {
    return configuration -> {
      configuration.getTypeHandlerRegistry().register("org.gbif.registry.persistence.handler");
      configuration.setMapUnderscoreToCamelCase(true);
      configuration.getTypeHandlerRegistry().register(UUID.class, UuidTypeHandler.class);
      configuration.getTypeHandlerRegistry().register(URI.class, UriTypeHandler.class);
      configuration.getTypeHandlerRegistry().register(Country.class, CountryTypeHandler.class);
      configuration.getTypeHandlerRegistry().register(Language.class, LanguageTypeHandler.class);
      configuration
          .getTypeHandlerRegistry()
          .register(Download.Status.class, OccurrenceDownloadStatusTypeHandler.class);
      configuration.getTypeHandlerRegistry().register(DOI.class, DOITypeHandler.class);
      configuration.getTypeHandlerRegistry().register(Predicate.class, PredicateTypeHandler.class);
      configuration.getTypeHandlerRegistry().register(MetricInfoTypeHandler.class);
      configuration.getTypeHandlerRegistry().register(LocaleTypeHandler.class);

      configuration.getTypeAliasRegistry().registerAlias("Node", Node.class);
      configuration.getTypeAliasRegistry().registerAlias("Organization", Organization.class);
      configuration.getTypeAliasRegistry().registerAlias("Installation", Installation.class);
      configuration.getTypeAliasRegistry().registerAlias("Dataset", Dataset.class);
      configuration.getTypeAliasRegistry().registerAlias("Network", Network.class);
      configuration.getTypeAliasRegistry().registerAlias("Citation", Citation.class);
      configuration.getTypeAliasRegistry().registerAlias("Contact", Contact.class);
      configuration.getTypeAliasRegistry().registerAlias("Endpoint", Endpoint.class);
      configuration.getTypeAliasRegistry().registerAlias("MachineTag", MachineTag.class);
      configuration.getTypeAliasRegistry().registerAlias("Tag", Tag.class);
      configuration.getTypeAliasRegistry().registerAlias("Identifier", Identifier.class);
      configuration.getTypeAliasRegistry().registerAlias("Comment", Comment.class);
      configuration.getTypeAliasRegistry().registerAlias("Metadata", Metadata.class);
      configuration.getTypeAliasRegistry().registerAlias("Download", Download.class);
      configuration.getTypeAliasRegistry().registerAlias("DownloadRequest", DownloadRequest.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("PredicateDownloadRequest", PredicateDownloadRequest.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("DatasetOccurrenceDownload", DatasetOccurrenceDownloadUsage.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("DatasetProcessStatus", DatasetProcessStatus.class);
      configuration.getTypeAliasRegistry().registerAlias("CrawlJob", CrawlJob.class);
      configuration.getTypeAliasRegistry().registerAlias("MetasyncHistory", MetasyncHistory.class);
      configuration.getTypeAliasRegistry().registerAlias("DoiData", DoiData.class);
      configuration.getTypeAliasRegistry().registerAlias("DOI", DOI.class);
      configuration.getTypeAliasRegistry().registerAlias("DoiType", DoiType.class);
      configuration.getTypeAliasRegistry().registerAlias("Pageable", Pageable.class);
      configuration.getTypeAliasRegistry().registerAlias("UUID", UUID.class);
      configuration.getTypeAliasRegistry().registerAlias("Country", Country.class);
      configuration.getTypeAliasRegistry().registerAlias("Language", Language.class);
      configuration.getTypeAliasRegistry().registerAlias("Count", Facet.Count.class);
      configuration.getTypeAliasRegistry().registerAlias("Institution", Institution.class);
      configuration.getTypeAliasRegistry().registerAlias("SciCollection", Collection.class);
      configuration.getTypeAliasRegistry().registerAlias("CollectionPerson", Person.class);
      configuration.getTypeAliasRegistry().registerAlias("Address", Address.class);

      configuration.getTypeAliasRegistry().registerAlias("UriTypeHandler", UriTypeHandler.class);
      configuration.getTypeAliasRegistry().registerAlias("UuidTypeHandler", UuidTypeHandler.class);
      configuration.getTypeAliasRegistry().registerAlias("DerivedDataset", DerivedDataset.class);
      configuration.getTypeAliasRegistry().registerAlias("DerivedDatasetUsage", DerivedDatasetUsage.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("LanguageTypeHandler", LanguageTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("CountryTypeHandler", CountryTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("DownloadStatusTypeHandler", OccurrenceDownloadStatusTypeHandler.class);
      configuration.getTypeAliasRegistry().registerAlias("DoiTypeHandler", DOITypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("PredicateTypeHandler", PredicateTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("StringArrayTypeHandler", StringArrayTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("UriArrayTypeHandler", UriArrayTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("DisciplineArrayTypeHandler", DisciplineArrayTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias(
              "CollectionContentTypeArrayTypeHandler", CollectionContentTypeArrayTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias(
              "PreservationTypeArrayTypeHandler", PreservationTypeArrayTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("StepTypeArrayTypeHandler", StepTypeArrayTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("LocaleTypeHandler", LocaleTypeHandler.class);

      configuration.getTypeAliasRegistry().registerAlias("PipelineProcess", PipelineProcess.class);
      configuration.getTypeAliasRegistry().registerAlias("Step", PipelineStep.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("PipelineExecution", PipelineExecution.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("MetricInfoTypeHandler", MetricInfoTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("CollectionSummaryTypeHandler", CollectionSummaryTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("AlternativeCodesTypeHandler", AlternativeCodesTypeHandler.class);
    };
  }
}
