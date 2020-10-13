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
package org.gbif.registry.ws.resources.external;

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.persistence.mapper.collections.external.CollectionDto;
import org.gbif.registry.persistence.mapper.collections.external.IDigBioMapper;
import org.gbif.registry.persistence.mapper.collections.external.IdentifierDto;
import org.gbif.registry.persistence.mapper.collections.external.MachineTagDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This endpoint exists only to provide a custom feed for the iDigBio portal visible on
 * https://www.idigbio.org/portal/collections.
 */
@RestController
@RequestMapping(
    value = "/external/idigbio/collections",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class IDigBioExternalResource {

  private final IDigBioMapper idigBioMapper;

  public IDigBioExternalResource(IDigBioMapper idigBioMapper) {
    this.idigBioMapper = idigBioMapper;
  }

  @NullToNotFound
  @GetMapping
  public List<IDigBioCollection> getCollectionList() {
    List<MachineTagDto> machineTagDtos = idigBioMapper.getMachineTags(null);
    Map<UUID, Set<MachineTagDto>> machineTagsByCollection =
        machineTagDtos.stream()
            .collect(
                Collectors.groupingBy(
                    MachineTagDto::getEntityKey, HashMap::new, Collectors.toSet()));

    Set<UUID> collectionKeys = machineTagsByCollection.keySet();

    if (collectionKeys.isEmpty()) {
      return null;
    }

    CompletableFuture<List<CollectionDto>> collectionsDtoFuture =
        CompletableFuture.supplyAsync(() -> idigBioMapper.getCollections(collectionKeys));
    CompletableFuture<List<IdentifierDto>> identifiersDtoFuture =
        CompletableFuture.supplyAsync(() -> idigBioMapper.getIdentifiers(collectionKeys));

    CompletableFuture.allOf(collectionsDtoFuture, identifiersDtoFuture).join();

    List<CollectionDto> collectionDtos = collectionsDtoFuture.join();
    Map<UUID, CollectionDto> collectionsByKey =
        collectionDtos.stream().collect(Collectors.toMap(CollectionDto::getCollectionKey, c -> c));

    List<IdentifierDto> identifierDtos = identifiersDtoFuture.join();
    Map<UUID, Set<IdentifierDto>> identifiersByCollection =
        identifierDtos.stream()
            .collect(
                Collectors.groupingBy(
                    IdentifierDto::getEntityKey, HashMap::new, Collectors.toSet()));

    List<IDigBioCollection> result = new ArrayList<>();
    for (UUID k : collectionKeys) {
      CollectionDto collection = collectionsByKey.get(k);
      Set<IdentifierDto> identifiers =
          identifiersByCollection.getOrDefault(k, Collections.emptySet());
      Set<MachineTagDto> machineTags = machineTagsByCollection.get(k);

      IDigBioCollection iDigBioCollection =
          convertToIDigBioCollection(collection, identifiers, machineTags);

      result.add(iDigBioCollection);
    }

    return result;
  }

  @NullToNotFound
  @GetMapping("{iDigBioKey}")
  public IDigBioCollection getCollection(@PathVariable UUID iDigBioKey) {
    UUID collectionKey = idigBioMapper.findCollectionByIDigBioUuid("urn:uuid:" + iDigBioKey);

    if (collectionKey == null) {
      return null;
    }

    CompletableFuture<List<CollectionDto>> collectionsDtoFuture =
        CompletableFuture.supplyAsync(
            () -> idigBioMapper.getCollections(Collections.singleton(collectionKey)));
    CompletableFuture<List<MachineTagDto>> machineTagsDtoFuture =
        CompletableFuture.supplyAsync(
            () -> idigBioMapper.getMachineTags(Collections.singleton(collectionKey)));
    CompletableFuture<List<IdentifierDto>> identifiersDtoFuture =
        CompletableFuture.supplyAsync(
            () -> idigBioMapper.getIdentifiers(Collections.singleton(collectionKey)));

    CompletableFuture.allOf(collectionsDtoFuture, machineTagsDtoFuture, identifiersDtoFuture)
        .join();

    List<CollectionDto> collectionDtos = collectionsDtoFuture.join();
    if (collectionDtos.isEmpty()) {
      return null;
    }

    List<MachineTagDto> machineTagDtos = machineTagsDtoFuture.join();
    List<IdentifierDto> identifierDtos = identifiersDtoFuture.join();

    return convertToIDigBioCollection(
        collectionDtos.get(0), new HashSet<>(identifierDtos), new HashSet<>(machineTagDtos));
  }

  @NotNull
  private IDigBioCollection convertToIDigBioCollection(
      CollectionDto collection, Set<IdentifierDto> identifiers, Set<MachineTagDto> machineTags) {
    IDigBioCollection iDigBioCollection = new IDigBioCollection();
    iDigBioCollection.setInstitutionKey(collection.getInstitutionKey());
    iDigBioCollection.setCollectionKey(collection.getCollectionKey());
    iDigBioCollection.setInstitution(collection.getInstitutionName());
    iDigBioCollection.setCollection(collection.getCollectionName());

    machineTags.stream()
        .filter(t -> t.getName().equals("recordsets"))
        .map(MachineTagDto::getValue)
        .findFirst()
        .ifPresent(iDigBioCollection::setRecordsets);
    machineTags.stream()
        .filter(t -> t.getName().equals("recordsetQuery"))
        .map(MachineTagDto::getValue)
        .findFirst()
        .ifPresent(iDigBioCollection::setRecordsetQuery);

    Set<String> institutionCodes =
        new HashSet<>(collection.getInstitutionAlternativeCodes().keySet());
    institutionCodes.add(collection.getInstitutionCode());
    iDigBioCollection.setInstitutionCode(String.join(", ", institutionCodes).trim());

    Set<String> collectionCodes =
        new HashSet<>(collection.getCollectionAlternativeCodes().keySet());
    collectionCodes.add(collection.getCollectionCode());
    iDigBioCollection.setCollectionCode(String.join(", ", collectionCodes).trim());

    machineTags.stream()
        .filter(t -> t.getName().equals("CollectionUUID"))
        .map(MachineTagDto::getValue)
        .findFirst()
        .ifPresent(iDigBioCollection::setCollectionUuid);

    identifiers.stream()
        .filter(i -> i.getType() == IdentifierType.LSID)
        .map(IdentifierDto::getIdentifier)
        .findFirst()
        .ifPresent(iDigBioCollection::setCollectionLsid);

    iDigBioCollection.setCollectionUrl(collection.getHomepage());
    iDigBioCollection.setCollectionCatalogUrl(collection.getCatalogUrl());
    iDigBioCollection.setDescription(collection.getDescription());
    iDigBioCollection.setCataloguedSpecimens(collection.getNumberSpecimens());
    iDigBioCollection.setTaxonCoverage(collection.getTaxonomicCoverage());
    iDigBioCollection.setGeographicRange(collection.getGeographicRange());

    iDigBioCollection.setContact(
        collection.getContact() != null ? collection.getContact().trim() : null);
    iDigBioCollection.setContactRole(collection.getContactPosition());
    iDigBioCollection.setContactEmail(collection.getContactEmail());

    iDigBioCollection.setMailingAddress(collection.getMailingAddress());
    iDigBioCollection.setMailingCity(collection.getMailingCity());
    iDigBioCollection.setMailingState(collection.getMailingState());
    iDigBioCollection.setMailingZip(collection.getMailingZip());

    iDigBioCollection.setPhysicalAddress(collection.getPhysicalAddress());
    iDigBioCollection.setPhysicalCity(collection.getPhysicalCity());
    iDigBioCollection.setPhysicalState(collection.getPhysicalState());
    iDigBioCollection.setPhysicalZip(collection.getPhysicalZip());

    iDigBioCollection.setUniqueNameUUID(collection.getUniqueNameUUID());

    identifiers.stream()
        .filter(i -> i.getType() == IdentifierType.IH_IRN)
        .map(IdentifierDto::getIdentifier)
        .findFirst()
        .ifPresent(
            v ->
                iDigBioCollection.setSameAs(
                    "http://sweetgum.nybg.org/science/ih/herbarium_details.php?irn="
                        + decodeIRN(v)));

    iDigBioCollection.setLat(collection.getLatitude());
    iDigBioCollection.setLon(collection.getLongitude());
    return iDigBioCollection;
  }

  private static String decodeIRN(String irn) {
    return irn.replace("gbif:ih:irn:", "");
  }
}
