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
package org.gbif.registry.persistence.config;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.descriptors.Descriptor;
import org.gbif.api.model.collections.descriptors.DescriptorChangeSuggestion;
import org.gbif.api.model.collections.descriptors.DescriptorGroup;
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
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineProcess;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.predicate.Predicate;
import org.gbif.api.model.registry.*;
import org.gbif.api.model.registry.metasync.MetasyncHistory;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.mybatis.type.*;
import org.gbif.registry.domain.doi.DoiType;
import org.gbif.registry.domain.ws.DerivedDataset;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;
import org.gbif.registry.persistence.facet.LtreeTypeHandler;
import org.gbif.registry.persistence.mapper.auxhandler.AlternativeCodesTypeHandler;
import org.gbif.registry.persistence.mapper.collections.dto.*;
import org.gbif.registry.persistence.mapper.collections.external.IDigBioCollectionDto;
import org.gbif.registry.persistence.mapper.collections.external.IdentifierDto;
import org.gbif.registry.persistence.mapper.collections.external.MachineTagDto;
import org.gbif.registry.persistence.mapper.dto.OrganizationGeoJsonDto;
import org.gbif.registry.persistence.mapper.handler.*;

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
      configuration.getTypeHandlerRegistry().register(ExtensionArrayTypeHandler.class);
      configuration.getTypeHandlerRegistry().register(SetArrayTypeHandler.class);

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
          .registerAlias("SqlDownloadRequest", SqlDownloadRequest.class);
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
      configuration.getTypeAliasRegistry().registerAlias("Address", Address.class);
      configuration.getTypeAliasRegistry().registerAlias("CollectionDto", CollectionDto.class);
      configuration.getTypeAliasRegistry().registerAlias("DuplicateDto", DuplicateDto.class);
      configuration.getTypeAliasRegistry().registerAlias("DescriptorGroup", DescriptorGroup.class);
      configuration.getTypeAliasRegistry().registerAlias("DescriptorChangeSuggestion", DescriptorChangeSuggestion.class);
      configuration.getTypeAliasRegistry().registerAlias("Descriptor", Descriptor.class);
      configuration.getTypeAliasRegistry().registerAlias("DescriptorDto", DescriptorDto.class);
      configuration.getTypeAliasRegistry().registerAlias("VerbatimDto", VerbatimDto.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("DuplicateMetadataDto", DuplicateMetadataDto.class);
      configuration.getTypeAliasRegistry().registerAlias("SearchDto", SearchDto.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("InstitutionSearchDto", InstitutionSearchDto.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("CollectionSearchDto", CollectionSearchDto.class);
      configuration.getTypeAliasRegistry().registerAlias("FacetDto", FacetDto.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("InstitutionMatchedDto", InstitutionMatchedDto.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("InstitutionGeoJsonDto", InstitutionGeoJsonDto.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("CollectionMatchedDto", CollectionMatchedDto.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("ChangeSuggestionDto", ChangeSuggestionDto.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("MasterSourceOrganizationDto", MasterSourceOrganizationDto.class);

      configuration.getTypeAliasRegistry().registerAlias("UriTypeHandler", UriTypeHandler.class);
      configuration.getTypeAliasRegistry().registerAlias("UuidTypeHandler", UuidTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("CountryNotNullTypeHandler", CountryNotNullTypeHandler.class);
      configuration.getTypeAliasRegistry().registerAlias("DerivedDataset", DerivedDataset.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("DerivedDatasetUsage", DerivedDatasetUsage.class);
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
          .registerAlias("CollectionContentTypeArrayTypeHandler", CollectionContentTypeArrayTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("StepTypeArrayTypeHandler", StepTypeArrayTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("LocaleTypeHandler", LocaleTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("ExtensionArrayTypeHandler", ExtensionArrayTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("InstitutionGovernanceArrayTypeHandler", InstitutionGovernanceArrayTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("RankedNameListTypeHandler", RankedNameListTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("IntegerArrayTypeHandler", IntegerArrayTypeHandler.class);
      configuration
        .getTypeAliasRegistry()
        .registerAlias("ExportFormatHandler", ExportFormatHandler.class);

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
          .registerAlias("AlternativeCodesTypeHandler", AlternativeCodesTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("SuggestedChangesTypeHandler", SuggestedChangesTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("UserIdsTypeHandler", UserIdsTypeHandler.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("OrganizationGeoJsonDto", OrganizationGeoJsonDto.class);
      configuration
        .getTypeAliasRegistry()
        .registerAlias("MachineDescriptorTypeHandler", MachineDescriptorTypeHandler.class);

      configuration.getTypeAliasRegistry().registerAlias("LtreeTypeHandler", LtreeTypeHandler.class);

      // external iDigBio
      configuration.getTypeAliasRegistry().registerAlias("MachineTagDto", MachineTagDto.class);
      configuration.getTypeAliasRegistry().registerAlias("IdentifierDto", IdentifierDto.class);
      configuration
          .getTypeAliasRegistry()
          .registerAlias("IDigBioCollectionDto", IDigBioCollectionDto.class);

      configuration
        .getTypeAliasRegistry()
        .registerAlias("DwcA", Dataset.DwcA.class);
    };
  }
}
