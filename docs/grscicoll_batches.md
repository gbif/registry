# How to import or update a batch of institutions or collections

For each operation you need 2 CSV or TSV files(both of them need to have the same format, you cannot use a CSV and a TSV
together):

- Entities file: it contains the institutions or the collections. You can use these fields:
    - Institutions: the `InstitutionFields` and `CommonFields`
      defined [here](../registry-service/src/main/java/org/gbif/registry/service/collections/batch/FileFields.java)
    - Collections: the `CollectionFields` and `CommonFields`
      defined [here](../registry-service/src/main/java/org/gbif/registry/service/collections/batch/FileFields.java)

  Example of an entities file:
  ```
  CODE,NAME,DESCRIPTION,ACTIVE,ADDRESS_COUNTRY
  c1,n1,descr1,true,ES
  ```

  If you are doing an update of existing entities you must specify the key of each entity in the `KEY` column.
- Contacts file: it contains the contacts of the institutions or the collections. You can use any of the fields of
  the `ContactFields` defined [here](../registry-service/src/main/java/org/gbif/registry/service/collections/batch/FileFields.java)

  If you are doing an update of existing contacts you must specify the key of each entity in the `KEY` column and the
  key of the institution(`INSTITUTION_KEY`) or the collection(`COLLECTION_KEY`). If it is an initial import and the
  corresponding institution or collection doesn't exist yet because it'll be created during the import, you have to use
  the `INSTITUTION_CODE` and `COLLECTION_CODE`to link them.

  Example of a contacts file:
  ```
  KEY,FIRST_NAME,LAST_NAME,POSITION,INSTITUTION_CODE
  1,name1,lastn1,tester,c1
  ```

Also, there are these general rules:

- For multivalue fields you have to separate the values with a `|`. E.g: `value1|value2`
- For fields that contain subentities such as identifiers or alternative codes you have to separate the subentity fields
  with a `:`: E.g.: `altCode1:example1|altCode2:example2`
- The names of the columns in the files are not case-sensitive and can be organized in any order.

Once the files are ready you need to use these endpoints:
  - Batch of institutions: POST to `{gbif_api_base_path}/v1/grscicoll/institution/batch`
  - Batch of collections: POST to `{gbif_api_base_path}/v1/grscicoll/collection/batch`

Example:
```
curl {api_url}/v1/grscicoll/institution/batch?format=CSV -u "username:password" \
-F 'entitiesFile=@"/your_path/your_entities_file.csv"' \
-F 'contactsFile=@"/your_path/your_contacts_file.csv"'
```

You can find more information about these endpoints in the official API documentation.

The processing of the batches is done asynchronously so those endpoints return a URL that contains information about the
batch: `{gbif_api_base_path}/v1/grscicoll/institution/batch/{batchKey}`.  The processing is not done until the state is `FINISHED` or 'FAILED'. If it failed you can find the errors in the
`errors field`. Also, this processing generates another CSV or TSV file with the original file plus a column called `ERRORS`
where you can check if there has been with the import or update of that particular entity. Also, for the case of batches to
import new entities it returns the key of the created entities in the `KEY` column. The same applies for contacts.

These generated files can be found at `{gbif_api_base_path}/v1/grscicoll/collection/batch/{batchKet}/resultFile`




