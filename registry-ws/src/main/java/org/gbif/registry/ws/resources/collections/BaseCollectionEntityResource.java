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
package org.gbif.registry.ws.resources.collections;

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.annotation.Trim;
import org.gbif.api.documentation.CommonParameters;
import org.gbif.api.model.collections.Batch;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.MasterSourceMetadata;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.duplicates.DuplicatesRequest;
import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.api.model.collections.merge.MergeParams;
import org.gbif.api.model.collections.suggestions.ApplySuggestionResult;
import org.gbif.api.model.collections.suggestions.ChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.collections.view.BatchView;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.BatchService;
import org.gbif.api.service.collections.ChangeSuggestionService;
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.registry.persistence.mapper.collections.params.DuplicatesSearchParams;
import org.gbif.registry.service.collections.duplicates.DuplicatesService;
import org.gbif.registry.service.collections.merge.MergeService;
import org.gbif.registry.ws.resources.Docs;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Preconditions;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.SneakyThrows;

import static com.google.common.base.Preconditions.checkArgument;

/** Base class to implement the CRUD methods of a {@link CollectionEntity}. */
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public abstract class BaseCollectionEntityResource<
    T extends CollectionEntity & Taggable & Identifiable & MachineTaggable & Commentable,
    R extends ChangeSuggestion<T>> {

  private static final Logger LOG = LoggerFactory.getLogger(BaseCollectionEntityResource.class);

  protected final Class<T> objectClass;
  protected final CollectionEntityService<T> collectionEntityService;
  protected final MergeService<T> mergeService;
  protected final ChangeSuggestionService<T, R> changeSuggestionService;
  protected final DuplicatesService duplicatesService;
  protected final BatchService batchService;
  protected String apiBaseUrl;

  protected BaseCollectionEntityResource(
      MergeService<T> mergeService,
      CollectionEntityService<T> collectionEntityService,
      ChangeSuggestionService<T, R> changeSuggestionService,
      DuplicatesService duplicatesService,
      BatchService batchService,
      String apiBaseUrl,
      Class<T> objectClass) {
    this.objectClass = objectClass;
    this.mergeService = mergeService;
    this.changeSuggestionService = changeSuggestionService;
    this.collectionEntityService = collectionEntityService;
    this.duplicatesService = duplicatesService;
    this.batchService = batchService;
    this.apiBaseUrl = apiBaseUrl;
  }

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameters(
      value = {
        @Parameter(
            name = "code",
            description = "Code of a GrSciColl institution or collection",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "name",
            description = "Name of a GrSciColl institution or collection",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "alternativeCode",
            description = "Alternative code of a GrSciColl institution or collection",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "contact",
            description =
                "Filters collections and institutions whose contacts contain the person key specified",
            schema = @Schema(implementation = UUID.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "machineTagNamespace",
            description = "Filters for entities with a machine tag in the specified namespace.",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "machineTagName",
            description =
                "Filters for entities with a machine tag with the specified name (use in combination with the machineTagNamespace parameter).",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "machineTagValue",
            description =
                "Filters for entities with a machine tag with the specified value (use in combination with the machineTagNamespace and machineTagName parameters).",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "identifierType",
            description = "An identifier type for the identifier parameter.",
            schema = @Schema(implementation = IdentifierType.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "identifier",
            description =
                "An identifier of the type given by the identifierType parameter, for example a DOI or UUID.",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "country",
            description = "Filters by country given as a ISO 639-1 (2 letter) country code.",
            schema = @Schema(implementation = Country.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "city",
            description = "TODO",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "fuzzyName",
            description = "TODO",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "active",
            description = "Active status of a GrSciColl institution or collection",
            schema = @Schema(implementation = Boolean.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "masterSourceType",
            description = "The master source type of a GRSciColl institution or collection",
            schema = @Schema(implementation = MasterSourceType.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "numberSpecimens",
            description = "TODO",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "displayOnNHCPortal",
            description = "TODO",
            schema = @Schema(implementation = Boolean.class),
            in = ParameterIn.QUERY),
        @Parameter(name = "searchRequest", hidden = true)
      })
  @CommonParameters.QParameter
  @Pageable.OffsetLimitParameters
  public @interface SearchRequestParameters {}

  // OpenAPI documentation on subclasses
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public UUID create(@RequestBody @Trim T entity) {
    return collectionEntityService.create(entity);
  }

  // OpenAPI documentation on subclasses
  @PutMapping(value = "{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public void update(@PathVariable("key") UUID key, @RequestBody @Trim T entity) {
    checkArgument(key.equals(entity.getKey()));
    collectionEntityService.update(entity);
  }

  @Operation(
      operationId = "addContactPerson",
      summary = "Add a contact person to the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0411")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Contact person added, contact key returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(
      value = "{key}/contactPerson",
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public int addContactPerson(
      @PathVariable("key") UUID entityKey, @RequestBody @Trim Contact contact) {
    return collectionEntityService.addContactPerson(entityKey, contact);
  }

  @Operation(
      operationId = "updateContactPerson",
      summary = "Update an existing contact person on the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0412")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Contact person updated")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping(
      value = "{key}/contactPerson/{contactKey}",
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public void updateContactPerson(
      @PathVariable("key") UUID entityKey,
      @PathVariable("contactKey") int contactKey,
      @RequestBody @Trim Contact contact) {
    checkArgument(
        contactKey == contact.getKey(),
        "The contact key in the path has to match the key in the body");
    collectionEntityService.updateContactPerson(entityKey, contact);
  }

  @Operation(
      operationId = "deleteContactPerson",
      summary = "Delete a contact person from the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0413")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Contact person deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/contactPerson/{contactKey}")
  public void removeContactPerson(
      @PathVariable("key") UUID entityKey, @PathVariable int contactKey) {
    collectionEntityService.removeContactPerson(entityKey, contactKey);
  }

  @Operation(
      operationId = "listContactPeople",
      summary = "Retrieve all contact people of the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0461")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "List of contact people")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/contactPerson")
  @Nullable
  public List<Contact> listContactPersons(@PathVariable UUID key) {
    return collectionEntityService.listContactPersons(key);
  }

  @Operation(
      operationId = "addOccurrenceMapping",
      summary = "Add a occurrence mapping to the record")
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(
      responseCode = "200",
      description = "Occurrence mapping added, contact key returned",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0461")))
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "{key}/occurrenceMapping", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public int addOccurrenceMapping(
      @PathVariable("key") UUID entityKey, @RequestBody @Trim OccurrenceMapping occurrenceMapping) {
    return collectionEntityService.addOccurrenceMapping(entityKey, occurrenceMapping);
  }

  @Operation(
      operationId = "listOccurrenceMappings",
      summary = "Retrieve all occurrence mappings of the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0460")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "List of occurrence mappings")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/occurrenceMapping")
  @Nullable
  public List<OccurrenceMapping> listOccurrenceMappings(@PathVariable("key") UUID uuid) {
    return collectionEntityService.listOccurrenceMappings(uuid);
  }

  @Operation(
      operationId = "deleteOccurrenceMapping",
      summary = "Delete an occurrence mapping from the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0463")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Occurrence mapping deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/occurrenceMapping/{occurrenceMappingKey}")
  public void deleteOccurrenceMapping(
      @PathVariable("key") UUID entityKey, @PathVariable int occurrenceMappingKey) {
    collectionEntityService.deleteOccurrenceMapping(entityKey, occurrenceMappingKey);
  }

  @Operation(
      operationId = "merge",
      summary = "Merges a record with another record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0490")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Records merged")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "{key}/merge")
  public void merge(@PathVariable("key") UUID entityKey, @RequestBody MergeParams params) {
    mergeService.merge(entityKey, params.getReplacementEntityKey());
  }

  @Operation(
      operationId = "listPossibleDuplicates",
      summary = "Retrieve a list of all possible duplicates",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0510")))
  @ApiResponse(responseCode = "200", description = "List of possible duplicates")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("possibleDuplicates")
  public DuplicatesResult findPossibleDuplicates(DuplicatesRequest request) {
    Preconditions.checkArgument(
        !request.isEmpty(), "At least one param to check the same field is required");

    return duplicatesService.findPossibleDuplicates(
        DuplicatesSearchParams.builder()
            .sameFuzzyName(request.getSameFuzzyName())
            .sameName(request.getSameName())
            .sameCode(request.getSameCode())
            .sameCountry(request.getSameCountry())
            .sameCity(request.getSameCity())
            .inCountries(request.getInCountries())
            .notInCountries(request.getNotInCountries())
            .excludeKeys(request.getExcludeKeys())
            .build());
  }

  @Operation(
      operationId = "addChangeSuggestion",
      summary = "Add a change suggestion to the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0482")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Change suggestion added, contact key returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "changeSuggestion")
  public int createChangeSuggestion(@RequestBody @Trim R createSuggestion) {
    return changeSuggestionService.createChangeSuggestion(createSuggestion);
  }

  @Operation(
      operationId = "updateChangeSuggestion",
      summary = "Update an existing change suggestion on the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0483")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Change suggestion updated")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping(value = "changeSuggestion/{key}")
  public void updateChangeSuggestion(
      @PathVariable("key") int key, @RequestBody @Trim R suggestion) {
    checkArgument(key == suggestion.getKey());
    changeSuggestionService.updateChangeSuggestion(suggestion);
  }

  @Operation(
      operationId = "getChangeSuggestion",
      summary = "Retrieve a single change suggestion of a record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0481")))
  @ApiResponse(responseCode = "200", description = "A change suggestion")
  @Docs.DefaultUnsuccessfulReadResponses
  @NullToNotFound
  @GetMapping(value = "changeSuggestion/{key}")
  public R getChangeSuggestion(@PathVariable("key") int key) {
    return changeSuggestionService.getChangeSuggestion(key);
  }

  @Operation(
      operationId = "listChangeSuggestion",
      summary = "Retrieve all change suggestions of the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0480")))
  @ApiResponse(responseCode = "200", description = "List of change suggestions")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping(value = "changeSuggestion")
  public PagingResponse<R> listChangeSuggestion(
      @RequestParam(value = "status", required = false) Status status,
      @RequestParam(value = "type", required = false) Type type,
      @RequestParam(value = "proposerEmail", required = false) String proposerEmail,
      @RequestParam(value = "entityKey", required = false) UUID entityKey,
      Pageable page) {
    return changeSuggestionService.list(status, type, proposerEmail, entityKey, page);
  }

  @Operation(
      operationId = "discardChangeSuggestion",
      summary = "Discard a collection change suggestion",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0484")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Change suggestion discarded")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping(value = "changeSuggestion/{key}/discard")
  public void discardChangeSuggestion(@PathVariable("key") int key) {
    changeSuggestionService.discardChangeSuggestion(key);
  }

  @Operation(
      operationId = "applyChangeSuggestion",
      summary = "Apply a collection change suggestion",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0485")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Apply suggestion discarded")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping(value = "changeSuggestion/{key}/apply")
  public ApplySuggestionResult applyChangeSuggestion(@PathVariable("key") int key) {
    UUID entityCreatedKey = changeSuggestionService.applyChangeSuggestion(key);
    ApplySuggestionResult result = new ApplySuggestionResult();
    result.setEntityCreatedKey(entityCreatedKey);
    return result;
  }

  @Operation(
      operationId = "addMasterSourceMetadata",
      summary = "Add master source metadata to the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0451")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Master source metadata added, key returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "{key}/masterSourceMetadata", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public int addMasterSourceMetadata(
      @PathVariable("key") UUID entityKey,
      @RequestBody @Trim MasterSourceMetadata masterSourceMetadata) {
    return collectionEntityService.addMasterSourceMetadata(entityKey, masterSourceMetadata);
  }

  @Operation(
      operationId = "getMasterSourceMetadata",
      summary = "Retrieve a master source metadata record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0450")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Master source metadata record")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/masterSourceMetadata")
  @Nullable
  public MasterSourceMetadata getMasterSourceMetadata(@PathVariable("key") UUID entityKey) {
    return collectionEntityService.getMasterSourceMetadata(entityKey);
  }

  @Operation(
      operationId = "deleteMasterSourceMetadata",
      summary = "Delete a master source metadata from a record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0453")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Deletes a master source metadata record")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/masterSourceMetadata")
  public void deleteMasterSourceMetadata(@PathVariable("key") UUID entityKey) {
    collectionEntityService.deleteMasterSourceMetadata(entityKey);
  }

  // OpenAPI documentation on subclasses
  @DeleteMapping("{key}")
  public void delete(@PathVariable UUID key) {
    collectionEntityService.delete(key);
  }

  @Operation(
      operationId = "addIdentifier",
      summary = "Add an identifier to the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0431")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Identifier added, identifier key returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "{key}/identifier", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public int addIdentifier(
      @PathVariable("key") UUID entityKey, @RequestBody @Trim Identifier identifier) {
    return collectionEntityService.addIdentifier(entityKey, identifier);
  }

  @Operation(
      operationId = "deleteIdentifier",
      summary = "Delete an identifier from the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0433")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Endpoint deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/identifier/{identifierKey}")
  @Transactional
  public void deleteIdentifier(
      @PathVariable("key") UUID entityKey, @PathVariable int identifierKey) {
    collectionEntityService.deleteIdentifier(entityKey, identifierKey);
  }

  @Operation(
      operationId = "getIdentifier",
      summary = "Retrieve all identifiers of the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0430")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Identifiers list")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @GetMapping("{key}/identifier")
  @Nullable
  public List<Identifier> listIdentifiers(@PathVariable UUID key) {
    return collectionEntityService.listIdentifiers(key);
  }

  @Operation(
      operationId = "addTag",
      summary = "Add a tag to the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0471")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Tag added, tag key returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "{key}/tag", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public int addTag(@PathVariable("key") UUID entityKey, @RequestBody @Trim Tag tag) {
    return collectionEntityService.addTag(entityKey, tag);
  }

  @Operation(
      operationId = "deleteTag",
      summary = "Delete a tag from the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0473")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Tag deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/tag/{tagKey}")
  @Transactional
  public void deleteTag(@PathVariable("key") UUID entityKey, @PathVariable int tagKey) {
    collectionEntityService.deleteTag(entityKey, tagKey);
  }

  @Operation(
      operationId = "getTag",
      summary = "Retrieve all tags of the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0470")))
  @Docs.DefaultEntityKeyParameter
  @Parameter(name = "owner", hidden = true)
  @ApiResponse(responseCode = "200", description = "Tag list")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("{key}/tag")
  @Nullable
  public List<Tag> listTags(
      @PathVariable("key") UUID key,
      @RequestParam(value = "owner", required = false) String owner) {
    return collectionEntityService.listTags(key, owner);
  }

  @Operation(
      operationId = "addMachineTag",
      summary = "Add a machine tag to the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0441")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Machine tag added, machine tag key returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "{key}/machineTag", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public int addMachineTag(
      @PathVariable("key") UUID targetEntityKey, @RequestBody @Trim MachineTag machineTag) {
    return collectionEntityService.addMachineTag(targetEntityKey, machineTag);
  }

  @Operation(
      operationId = "deleteMachineTag",
      summary = "Delete a machine tag from the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0442")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Machine tag deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/machineTag/{machineTagKey:[0-9]+}")
  public void deleteMachineTagByMachineTagKey(
      @PathVariable("key") UUID targetEntityKey, @PathVariable("machineTagKey") int machineTagKey) {
    collectionEntityService.deleteMachineTag(targetEntityKey, machineTagKey);
  }

  @Operation(
      operationId = "deleteMachineTagsInNamespace",
      summary = "Delete all machine tags in a namespace from the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0443")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Machine tags in namespace deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/machineTag/{namespace:.*[^0-9]+.*}")
  public void deleteMachineTagsByNamespace(
      @PathVariable("key") UUID targetEntityKey, @PathVariable("namespace") String namespace) {
    collectionEntityService.deleteMachineTags(targetEntityKey, namespace);
  }

  @Operation(
      operationId = "deleteMachineTagInNamespaceName",
      summary = "Delete all machine tags of a name in a namespace from the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0444")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Named machine tags in namespace deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/machineTag/{namespace}/{name}")
  public void deleteMachineTags(
      @PathVariable("key") UUID targetEntityKey,
      @PathVariable("namespace") String namespace,
      @PathVariable("name") String name) {
    collectionEntityService.deleteMachineTags(targetEntityKey, namespace, name);
  }

  @Operation(
      operationId = "listMachineTag",
      summary = "List all machine tags on the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0440")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Machine tags list")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @GetMapping("{key}/machineTag")
  public List<MachineTag> listMachineTags(@PathVariable("key") UUID targetEntityKey) {
    return collectionEntityService.listMachineTags(targetEntityKey);
  }

  @Operation(
      operationId = "addComment",
      summary = "Add a comment to the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0401")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Comment added, comment key returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "{key}/comment", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  public int addComment(
      @PathVariable("key") UUID targetEntityKey, @RequestBody @Trim Comment comment) {
    return collectionEntityService.addComment(targetEntityKey, comment);
  }

  @Operation(
      operationId = "deleteComment",
      summary = "Delete a comment from the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0403")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Comment deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/comment/{commentKey}")
  public void deleteComment(
      @PathVariable("key") UUID targetEntityKey, @PathVariable("commentKey") int commentKey) {
    collectionEntityService.deleteComment(targetEntityKey, commentKey);
  }

  @Operation(
      operationId = "getComment",
      summary = "Retrieve all comments of the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0400")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "List of comments")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping(value = "{key}/comment")
  public List<Comment> listComments(@PathVariable("key") UUID targetEntityKey) {
    return collectionEntityService.listComments(targetEntityKey);
  }

  @Operation(operationId = "importBatch", summary = "Process a batch of GRSciColl entities")
  @ApiResponse(
      responseCode = "201",
      description = "Batch created and being handled. URL with the batch information to check its status.",
      content = @Content)
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @Parameter(
      name = "format",
      description = "Format of the files(CSV or TSV)",
      required = true,
      in = ParameterIn.QUERY,
      schema = @Schema(implementation = ExportFormat.class))
  @Parameter(
      name = "entitiesFile",
      description = "File with the entities of the batch",
      required = true,
      in = ParameterIn.QUERY)
  @Parameter(
      name = "contactsFile",
      description = "File with the contacts associated to the entities",
      required = true,
      in = ParameterIn.QUERY)
  @SneakyThrows
  @PostMapping(value = "batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<String> importBatch(
      HttpServletRequest request,
      @RequestParam("format") ExportFormat format,
      @RequestPart("entitiesFile") MultipartFile entitiesFile,
      @RequestPart("contactsFile") MultipartFile contactsFile) {
    int batchKey =
        batchService.handleBatch(
            StreamUtils.copyToByteArray(entitiesFile.getResource().getInputStream()),
            StreamUtils.copyToByteArray(contactsFile.getResource().getInputStream()),
            format);

    String batchUri = getNormalizedApiBaseUrl() + request.getRequestURI() + "/" + batchKey;
    return ResponseEntity.created(new URI(batchUri)).body(batchUri);
  }

  @Operation(
      operationId = "getBatch",
      summary = "Get details of a batch",
      description = "Details of a batch.")
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Batch information")
  @Docs.DefaultUnsuccessfulReadResponses
  @GetMapping("batch/{key}")
  @NullToNotFound
  public BatchView getBatch(HttpServletRequest request, @PathVariable("key") int batchKey) {
    Batch batch = batchService.get(batchKey);

    if (batch == null) {
      return null;
    }

    BatchView batchView = new BatchView(batch);

    // Add file result link
    String resultFilePath = batch.getResultFilePath();
    if (resultFilePath != null) {
      String resultFileUri = getNormalizedApiBaseUrl() + request.getRequestURI() + "/resultFile";
      batchView.setResultFileLink(resultFileUri);
    }

    return batchView;
  }

  @Operation(
      operationId = "getBatchResultFile",
      summary =
          "Get a file with the result of a batch that includes keys of the new entities created and errors found",
      description = "Result file of a batch.")
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Result file found and returned as attachment")
  @Docs.DefaultUnsuccessfulReadResponses
  @ApiResponse(
      responseCode = "410",
      description = "The file was probably removed from the server",
      content = @Content)
  @GetMapping(value = "batch/{key}/resultFile", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<Resource> getBatchResultFile(@PathVariable("key") int batchKey)
      throws IOException {
    Batch batch = batchService.get(batchKey);

    if (batch == null) {
      return ResponseEntity.notFound().build();
    }

    String resultFilePath = batch.getResultFilePath();
    if (resultFilePath != null) {
      Path resultFile = Paths.get(resultFilePath);

      if (!Files.exists(resultFile)) {
        // the file was probably removed from the server to save space
        return ResponseEntity.status(HttpStatus.GONE).build();
      }

      ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(resultFile));
      return ResponseEntity.ok()
          .header("Content-Disposition", "attachment; filename=" + resultFile.getFileName())
          .body(resource);
    }

    return ResponseEntity.notFound().build();
  }

  private String getNormalizedApiBaseUrl() {
    return apiBaseUrl.endsWith("/") ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1) : apiBaseUrl;
  }
}
