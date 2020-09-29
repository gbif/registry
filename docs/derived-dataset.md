# Derived dataset service


| Request                                    | Description                                                                        | Content-type         | Request Example                                          |
| ------------------------------------------ | -----------------------------------------------------------------------------------| ---------------------| -------------------------------------------------------- |
| `POST /derivedDataset`                     | Create a new derived dataset                                                       | application/json     | [Example](examples/createDerivedDataset.http)            |
| `POST /derivedDataset`                     | Create a new derived dataset attaching a file with the related datasets            | multipart/form-data  | [Example](examples/createDerivedDatasetWithFile.http)    |
| `PUT /derivedDataset/{doi}`                | Update the derived dataset with the DOI                                            | application/json     | [Example](examples/updateDerivedDataset.http)            |
| `GET /derivedDataset/{doi}`                | Get the derived dataset by the DOI                                                 | application/json     | [Example](examples/getDerivedDataset.http)               |
| `GET /derivedDataset/dataset/{datasetKey}` | Get all derived datasets which use the dataset with the key                        | application/json     | [Example](examples/getDerivedDatasetsByDatasetKey.http)  |
| `GET /derivedDataset/dataset/{datasetDoi}` | Get all derived datasets which use the dataset with the DOI                        | application/json     | [Example](examples/getDerivedDatasetsByDatasetDoi.http)  |
| `GET /derivedDataset/{doi}/citation`       | Get derived dataset citation string                                                | application/json     | [Example](examples/getDerivedDatasetCitation.http)       | 
| `GET /derivedDataset/{doi}/datasets`       | Get all the datasets of the derived dataset                                        | application/json     | [Example](examples/getDerivedDatasetUsedDatasets.http)   |
