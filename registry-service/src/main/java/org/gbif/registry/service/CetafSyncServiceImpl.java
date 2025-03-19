package org.gbif.registry.service;


import com.opencsv.CSVWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

import java.util.stream.Collectors;

import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Collection;

import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.metasync.cetaf.CetafCollectionResponse;
import org.gbif.api.model.registry.metasync.cetaf.ChildCollection;
import org.gbif.api.model.registry.metasync.cetaf.DataItem;
import org.gbif.api.model.registry.metasync.cetaf.DataListItem;
import org.gbif.api.model.registry.metasync.cetaf.MainMetadata;
import org.gbif.api.model.registry.metasync.cetaf.Storage;
import org.gbif.api.model.registry.metasync.cetaf.StorageCategory;
import org.gbif.api.service.collections.CollectionService;

import org.gbif.api.service.collections.ContactService;
import org.gbif.api.service.collections.DescriptorsService;

import org.gbif.registry.ws.client.CetafClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CetafSyncServiceImpl {

  private static final String CETAF_DESCRIPTOR_PREFIX = "cetaf.descriptor";
  private static final String GEOGRAPHIC_AREAS_TITLE = "Area list descriptors from CETAF";
  private static final String COUNTRIES_TITLE = "Country descriptors from CETAF";
  private static final String STORAGE_TITLE = "Storage descriptors from CETAF";

  @Value("${cetaf.api.url}")
  private String cetafApiUrl;

  private final CetafClient cetafClient;
  private final CollectionService collectionService;
  private final DescriptorsService descriptorsService;
  private static final Logger LOG = LoggerFactory.getLogger(CetafSyncServiceImpl.class);

  @Autowired
  public CetafSyncServiceImpl(
    CetafClient cetafClient,
    CollectionService collectionService,
    DescriptorsService descriptorsService,
    @Qualifier("defaultCollectionService") ContactService contactService) {
    this.cetafClient = cetafClient;
    this.collectionService = collectionService;
    this.descriptorsService = descriptorsService;
  }

  public void updateCollectionFromCetaf(String sourceId, UUID collectionKey) {
    try {
      LOG.info("Fetching CETAF data for source ID: {} and collection key: {}", sourceId, collectionKey);
      LOG.info("Using CETAF API URL: {}", cetafApiUrl);

      CetafCollectionResponse response = cetafClient.getCollectionById("get_by_id", "cetaf", sourceId);

      if (response != null) {
        LOG.info("Received response from CETAF API");
        if (!response.getData().isEmpty()) {
          LOG.info("Response contains data, updating collection");
          updateCollection(response, collectionKey);
        } else {
          LOG.error("Response data is empty for source ID: {}", sourceId);
        }
      } else {
        LOG.error("Received null response from CETAF API for source ID: {}", sourceId);
      }
    } catch (Exception e) {
      LOG.error("Error fetching/parsing CETAF data for source ID: {} and collection key: {}", sourceId, collectionKey, e);
    }
  }

  private MainMetadata getMainMetadata(DataItem cetafCollectionData) {
    if (cetafCollectionData == null || cetafCollectionData.getData() == null) {
      return null;
    }

    List<DataListItem> dataListItem = cetafCollectionData.getData().getDataList();
    if (dataListItem == null || dataListItem.isEmpty()) {
      return null;
    }

    return dataListItem.get(0).getData().getMainMetadata();
  }

  private void updateCollection(CetafCollectionResponse cetafData, UUID collectionKey) {
    Collection collection = collectionService.get(collectionKey);

    // Extract data once for readability
    DataItem cetafCollectionData = cetafData.getData().get(0);
    MainMetadata metadata = getMainMetadata(cetafCollectionData);

    if (metadata == null) {
      LOG.error("Could not extract metadata from CETAF data for collection: {}", collectionKey);
      return;
    }

    // Set basic collection properties
    collection.setCode(cetafCollectionData.getLocalIdentifier());
    collection.setDescription(metadata.getDescription().getAbstractText());
    collection.setGeographicCoverage(metadata.getGeographicCoverage().toString());

    //Add initial alternative code and process child collections
    addAlternativeCode(collection, cetafCollectionData);
    processChildCollections(collection, cetafCollectionData);
    processContacts(collection, cetafCollectionData);
    // Update the collection and create descriptors
    collectionService.update(collection);
    createDescriptors(cetafCollectionData, collection);
  }

  private String getCuratorEmail(DataItem cetafCollectionData) {
    if (cetafCollectionData == null || cetafCollectionData.getData() == null) {
      return null;
    }

    List<DataListItem> dataListItem = cetafCollectionData.getData().getDataList();
    if (dataListItem == null || dataListItem.isEmpty()) {
      return null;
    }

    MainMetadata metadata = dataListItem.get(0).getData().getMainMetadata();
    if (metadata == null || metadata.getContact() == null || metadata.getContact().getCurator() == null) {
      return null;
    }

    return metadata.getContact().getCurator().getMail();
  }

  private void processContacts(Collection collection, DataItem cetafCollectionData) {
    String email = getCuratorEmail(cetafCollectionData);
    if (email == null || email.trim().isEmpty()) {
      return;
    }

    // Get existing contacts
    List<Contact> existingContacts = collection.getContactPersons();
    if (existingContacts == null) {
      existingContacts = new ArrayList<>();
    }

    // Try to find existing contact with matching email
    Optional<Contact> existingContact = existingContacts.stream()
        .filter(c -> c.getEmail() != null && c.getEmail().contains(email))
        .findFirst();

    if (existingContact.isEmpty()) {
      Contact newContact = new Contact();
      newContact.setEmail(Collections.singletonList(email));
      collectionService.replaceContactPersons(collection.getKey(), Collections.singletonList(newContact));
    }
  }

  private void addAlternativeCode(Collection collection, DataItem collectionData) {
    String identifier = collectionData.getIdentifier();

    List<AlternativeCode> alternativeCodes = new ArrayList<>(collection.getAlternativeCodes());
    boolean identifierExists = collection.getAlternativeCodes().stream()
        .anyMatch(code -> code.getCode().equals(identifier));

    if (!identifierExists) {
        alternativeCodes.add(new AlternativeCode(identifier, null));
    }

    collection.setAlternativeCodes(alternativeCodes);
  }

  private void processChildCollections(Collection collection, DataItem collectionData) {
    List<List<ChildCollection>> childCollections = collectionData.getData().getChildCollections();
    if (childCollections != null && !childCollections.isEmpty()) {
        List<String> incorporatedCollections = new ArrayList<>();
        List<AlternativeCode> alternativeCodes = new ArrayList<>(collection.getAlternativeCodes());

        for (List<ChildCollection> childList : childCollections) {
            for (ChildCollection child : childList) {
                if (child.getName() != null) {
                    // Remove leading and trailing whitespace
                    String childName = child.getName().trim();

                    // Skip empty strings after trimming
                    if (!childName.isEmpty()) {
                        // Add to incorporated collections if not already present
                        if (!incorporatedCollections.contains(childName)) {
                            incorporatedCollections.add(childName);
                        }

                        // Add to alternative codes if not already present
                        AlternativeCode newCode = new AlternativeCode(childName, child.getUri());
                        boolean codeExists = alternativeCodes.stream()
                            .anyMatch(code -> code.getCode().equals(childName));

                        if (!codeExists) {
                            alternativeCodes.add(newCode);
                        }
                    }
                }
            }
        }
        collection.setAlternativeCodes(alternativeCodes);
        collection.setIncorporatedCollections(incorporatedCollections);
    } else {
        collection.setIncorporatedCollections(Collections.emptyList());
    }
  }

  private void createDescriptors(DataItem cetafCollectionData, Collection collection) {
    // Create or update geographic area descriptors
    createGeographicAreaDescriptors(cetafCollectionData, collection);

    // Create or update country descriptors
    createCountryDescriptors(cetafCollectionData, collection);

    // Create or update storage descriptors
    createStorageDescriptors(cetafCollectionData, collection);
  }

  private void updateDescriptorMachineTag(Collection collection, String title, String description) {
    String namespace = CETAF_DESCRIPTOR_PREFIX + "." + title.toLowerCase().replace(" ", "_");

    // Find existing machine tag if it exists
    Optional<MachineTag> existingTag = collection.getMachineTags().stream()
        .filter(mt -> namespace.equals(mt.getNamespace()) && title.equals(mt.getName()))
        .findFirst();

    if (existingTag.isPresent()) {
        // Update existing tag while preserving key and created fields
        MachineTag tag = existingTag.get();
        tag.setValue(description);
    } else {
        // Create new machine tag
        MachineTag machineTag = new MachineTag();
        machineTag.setNamespace(namespace);
        machineTag.setName(title);
        machineTag.setValue(description);
        collectionService.addMachineTag(collection.getKey(),machineTag);
    }
  }

  private void createGeographicAreaDescriptors(DataItem collectionData, Collection collection) {
    String[] headers = {
        "CETAF zones", "ltc:bioregion", "dwc:continent", "ltc:biome",
        "ltc:objectClassificationName", "dwc:individualCount"
    };

    List<String[]> rows = new ArrayList<>();
    Map<String, Map<String, Map<String, Object>>> geographicAreas =
        collectionData.getData().getDataList().get(0).getData().getCollectionData().getGeospatialCoverage().getGeographicAreas();

    // Process each zone type (MARINE ZONES, TERRESTRIAL ZONES, Region Unknown)
    for (Map.Entry<String, Map<String, Map<String, Object>>> zoneEntry : geographicAreas.entrySet()) {
        String zoneType = zoneEntry.getKey();

        // Process each region
        for (Map.Entry<String, Map<String, Object>> regionEntry : zoneEntry.getValue().entrySet()) {
            String regionName = regionEntry.getKey().trim();
            Map<String, Object> regionData = regionEntry.getValue();

            if (regionData != null && regionData.containsKey("object_quantity")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> objectQuantity = (Map<String, Object>) regionData.get("object_quantity");

                if (objectQuantity != null && objectQuantity.containsKey("DETAIL")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> detail = (Map<String, Object>) objectQuantity.get("DETAIL");

                    // Get the continent based on the region name
                    String continent = getContinent(regionName);

                    // Determine biome based on zone type
                    String biome = "MARINE ZONES".equals(zoneType) ? "Marine" :
                                 "TERRESTRIAL ZONES".equals(zoneType) ? "Terrestrial" : "";

                    // Process each classification in DETAIL
                    for (Map.Entry<String, Object> detailEntry : detail.entrySet()) {
                        String classification = detailEntry.getKey();
                        Object countObj = detailEntry.getValue();

                        // Handle the count value
                        String countStr = countObj != null ? countObj.toString().trim() : "";
                        if (!countStr.isEmpty() && !" ".equals(countStr)) {
                            try {
                                int count = Integer.parseInt(countStr);
                                if (count > 0) {
                                    rows.add(new String[]{
                                        zoneType,
                                        regionName,
                                        continent,
                                        biome,
                                        classification,
                                        String.valueOf(count)
                                    });
                                }
                            } catch (NumberFormatException e) {
                                LOG.debug("Skipping non-numeric count value: {} for classification: {}", countStr, classification);
                            }
                        }
                    }
                }
            }
        }
    }

    if (!rows.isEmpty()) {
        byte[] csvContent = generateCsvFile(headers, rows);
        try {
            // Try to find existing machine tag with descriptor group key
            Optional<MachineTag> existingTag = collection.getMachineTags().stream()
                .filter(mt -> mt.getNamespace().equals(CETAF_DESCRIPTOR_PREFIX + "." + GEOGRAPHIC_AREAS_TITLE.toLowerCase().replace(" ", "_")))
                .findFirst();

            if (existingTag.isPresent() && existingTag.get().getValue() != null) {
                // Update existing descriptor using stored key
                descriptorsService.updateDescriptorGroup(
                  Long.parseLong(existingTag.get().getValue()),
                    csvContent,
                    ExportFormat.CSV,
                    GEOGRAPHIC_AREAS_TITLE,
                    collectionData.getData().getDataList().get(0).getSource()
                );
            } else {
                // Create new descriptor and store its key
                Long descriptorKey = descriptorsService.createDescriptorGroup(
                    csvContent,
                    ExportFormat.CSV,
                    GEOGRAPHIC_AREAS_TITLE,
                    collectionData.getData().getDataList().get(0).getSource(),
                    collection.getKey()
                );
                // Store the descriptor key in machine tag
                updateDescriptorMachineTag(collection, GEOGRAPHIC_AREAS_TITLE, String.valueOf(descriptorKey));
            }
        } catch (Exception e) {
            LOG.error("Error creating/updating geographic area descriptors", e);
        }
    }
  }

  private String getContinent(String regionName) {
    // Remove any whitespace and convert to lowercase for comparison
    String normalizedName = regionName.trim().toLowerCase();

    if (normalizedName.contains("undefined continent")) {
        return "";
    }

    // Direct continent matches
    if (normalizedName.equals("europe")) return "Europe";
    if (normalizedName.equals("africa")) return "Africa";
    if (normalizedName.equals("north america")) return "North America";
    if (normalizedName.equals("south america")) return "South America";
    if (normalizedName.contains("antarctica")) return "Antarctica";
    if (normalizedName.contains("pacific")) return "Pacific";

    // Special cases for Asia
    if (normalizedName.contains("asia temperate") ||
        normalizedName.contains("asia tropical")) {
        return "Asia";
    }

    // If no match found, return empty string
    return "";
  }

  private void createCountryDescriptors(DataItem collectionData, Collection collection) {
    String[] headers = {
        "dwc:country", "ltc:objectClassificationName", "dwc:continent"
    };

    List<String[]> rows = new ArrayList<>();
    Map<String, Object> countryList =
        collectionData.getData().getDataList().get(0).getData().getCollectionData().getGeospatialCoverage().getCountryList();

    for (Map.Entry<String, Object> continentEntry : countryList.entrySet()) {
        String continent = continentEntry.getKey();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, String>> countries = (Map<String, Map<String, String>>) continentEntry.getValue();

        for (Map.Entry<String, Map<String, String>> countryEntry : countries.entrySet()) {
            // Extract the ISO code from the country string (e.g., "IQ - Iraq" -> "IQ")
            String countryString = countryEntry.getKey();
            String isoCode = countryString.split(" - ")[0];

            Map<String, String> countryData = countryEntry.getValue();

            // Iterate through all classifications in the country data
            for (Map.Entry<String, String> classificationEntry : countryData.entrySet()) {
                String classification = classificationEntry.getKey();
                String value = classificationEntry.getValue();

                if ("Yes".equals(value)) {
                    rows.add(new String[]{
                        isoCode,
                        classification,
                        continent
                    });
                }
            }
        }
    }

    if (!rows.isEmpty()) {
        byte[] csvContent = generateCsvFile(headers, rows);
        try {
            // Try to find existing machine tag with descriptor group key
            Optional<MachineTag> existingTag = collection.getMachineTags().stream()
                .filter(mt -> mt.getNamespace().equals(CETAF_DESCRIPTOR_PREFIX + "." + COUNTRIES_TITLE.toLowerCase().replace(" ", "_")))
                .findFirst();

            if (existingTag.isPresent() && existingTag.get().getValue() != null) {
                // Update existing descriptor using stored key
                descriptorsService.updateDescriptorGroup(
                    Long.parseLong(existingTag.get().getValue()),
                    csvContent,
                    ExportFormat.CSV,
                    COUNTRIES_TITLE,
                    collectionData.getData().getDataList().get(0).getSource()

                );
            } else {
                // Create new descriptor and store its key
                Long descriptorKey = descriptorsService.createDescriptorGroup(
                    csvContent,
                    ExportFormat.CSV,
                    COUNTRIES_TITLE,
                    collectionData.getData().getDataList().get(0).getSource(),
                    collection.getKey()
                );
                // Store the descriptor key in machine tag
                updateDescriptorMachineTag(collection, COUNTRIES_TITLE, String.valueOf(descriptorKey));
            }
        } catch (Exception e) {
            LOG.error("Error creating/updating country descriptors", e);
        }
    }
  }

  private void createStorageDescriptors(DataItem collectionData, Collection collection) {
    String[] headers = {
        "ltc:objectClassificationName", "dwc:individualCount", "ltc:preservationMethod"
    };

    List<String[]> rows = new ArrayList<>();
    Storage storage =
        collectionData.getData().getDataList().get(0).getData().getCollectionData().getStorage();

    if (storage != null && storage.getMethods() != null) {
        for (Map.Entry<String, StorageCategory> entry : storage.getMethods().entrySet()) {
            String preservationMethod = entry.getKey().replace("_", " - ");
            StorageCategory category = entry.getValue();

            if (category != null && category.getObjectQuantity() != null &&
                category.getObjectQuantity().getDetail() != null) {

                for (Map.Entry<String, Integer> detail : category.getObjectQuantity().getDetail().entrySet()) {
                    String classification = detail.getKey();
                    Integer count = detail.getValue();

                    // Only add if count is greater than 0
                    if (count != null && count > 0) {
                        rows.add(new String[]{
                            classification,
                            String.valueOf(count),
                            preservationMethod
                        });
                    }
                }
            }
        }
    }

    if (!rows.isEmpty()) {
        byte[] csvContent = generateCsvFile(headers, rows);
        try {
            // Try to find existing machine tag with descriptor group key
            Optional<MachineTag> existingTag = collection.getMachineTags().stream()
                .filter(mt -> mt.getNamespace().equals(CETAF_DESCRIPTOR_PREFIX + "." + STORAGE_TITLE.toLowerCase().replace(" ", "_")))
                .findFirst();

            if (existingTag.isPresent() && existingTag.get().getValue() != null) {
                // Update existing descriptor using stored key
                descriptorsService.updateDescriptorGroup(
                    Long.parseLong(existingTag.get().getValue()),
                    csvContent,
                    ExportFormat.CSV,
                    STORAGE_TITLE,
                    collectionData.getData().getDataList().get(0).getSource()
                );
            } else {
                // Create new descriptor and store its key
                Long descriptorKey = descriptorsService.createDescriptorGroup(
                    csvContent,
                    ExportFormat.CSV,
                    STORAGE_TITLE,
                    collectionData.getData().getDataList().get(0).getSource(),
                    collection.getKey()
                );
                // Store the descriptor key in machine tag
                updateDescriptorMachineTag(collection, STORAGE_TITLE, String.valueOf(descriptorKey));
            }
        } catch (Exception e) {
            LOG.error("Error creating/updating storage descriptors", e);
        }
    }
  }

  private byte[] generateCsvFile(String[] headers, List<String[]> rows) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
         CSVWriter writer = new CSVWriter(new OutputStreamWriter(baos))) {

        writer.writeNext(headers);
        writer.writeAll(rows);
        writer.flush();

        return baos.toByteArray();
    } catch (IOException e) {
        LOG.error("Error generating CSV file", e);
        throw new RuntimeException("Error generating CSV file", e);
    }
  }
}
