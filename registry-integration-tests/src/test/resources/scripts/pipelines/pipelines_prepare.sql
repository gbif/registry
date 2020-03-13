INSERT INTO public.node (key, gbif_region, continent, title, country, created_by, modified_by, created, modified,
                         deleted, fulltext_search, type, participation_status)
VALUES ('710970cf-e3f1-4e74-b09c-d8c86b9819d9', 'EUROPE', 'EUROPE', 'Org', null, 'mike', 'mike',
        '2019-09-12 12:10:28.917000', '2019-09-12 12:10:32.189000', null, null, 'COUNTRY', 'ASSOCIATE');

INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title, abbreviation,
                                 description, language, logo_url, city, province, country, postal_code, latitude,
                                 longitude, created_by, modified_by, created, modified, deleted, fulltext_search, email,
                                 phone, homepage, address, challenge_code_key)
VALUES ('36107c15-771c-4810-a298-b7558828b8bd', '710970cf-e3f1-4e74-b09c-d8c86b9819d9', true, 'welcome', 'Org', null,
        null, 'en', null, null, null, null, null, null, null, 'WS TEST', 'WS TEST', '2019-09-12 12:06:01.835000',
        '2019-09-12 12:06:04.366000', null, null, null, null, null, null, null);

INSERT INTO public.installation (key, organization_key, type, title, description, created_by, modified_by, created,
                                 modified, deleted, fulltext_search, password, disabled)
VALUES ('2fe63cec-9b23-4974-bab1-9f4118ef7711', '36107c15-771c-4810-a298-b7558828b8bd', 'IPT_INSTALLATION',
        'Test IPT Registry2', 'Description of Test IPT', 'WS TEST',
        'WS TEST', '2019-11-01 14:48:02.486307', '2019-11-01 14:48:02.486307', null, null, 'welcome', false);

INSERT INTO public.dataset (key, parent_dataset_key, duplicate_of_dataset_key, installation_key,
                            publishing_organization_key, external, type, sub_type, title, alias, abbreviation,
                            description, language, homepage, logo_url, citation, citation_identifier, rights,
                            locked_for_auto_update, created_by, modified_by, created, modified, deleted,
                            fulltext_search, doi, license, maintenance_update_frequency, version)
VALUES ('d82273f6-9738-48a5-a639-2086f9c49d18', null, null, '2fe63cec-9b23-4974-bab1-9f4118ef7711',
        '36107c15-771c-4810-a298-b7558828b8bd', false, 'OCCURRENCE', null, 'Test Dataset Registry', null, null,
        'Description of Test Dataset', 'en', 'http://www.homepage.com', 'http://www.logo.com/1', 'Citation stuff', null,
        null, false, 'registry_user', 'registry_user', '2019-11-12 08:49:53.062721', '2019-11-12 08:49:53.062721', null,
        '''dataset'':2,8 ''descript'':5 ''occurr'':4 ''registry2'':3 ''test'':1,7 ''www.homepage.com'':9',
        '10.21373/abc', 'UNSPECIFIED', null, null);
INSERT INTO public.dataset (key, parent_dataset_key, duplicate_of_dataset_key, installation_key,
                            publishing_organization_key, external, type, sub_type, title, alias, abbreviation,
                            description, language, homepage, logo_url, citation, citation_identifier, rights,
                            locked_for_auto_update, created_by, modified_by, created, modified, deleted,
                            fulltext_search, doi, license, maintenance_update_frequency, version)
VALUES ('4348adaa-d744-4241-92a0-ebf9d55eb9bb', null, null, '2fe63cec-9b23-4974-bab1-9f4118ef7711',
        '36107c15-771c-4810-a298-b7558828b8bd', false, 'OCCURRENCE', null, 'Test Dataset Registry 2', null, null,
        'Description of Test Dataset 2', 'en', 'http://www.homepage.com', 'http://www.logo.com/2', 'Citation stuff',
        null, null, false, 'WS TEST', 'WS TEST', '2019-11-12 08:49:53.062721', '2019-11-12 08:49:53.062721', null,
        '''dataset'':2,8 ''descript'':5 ''occurr'':4 ''registry2'':3 ''test'':1,7 ''www.homepage.com'':9',
        '10.21373/cba', 'UNSPECIFIED', null, null);
INSERT INTO public.dataset (key, parent_dataset_key, duplicate_of_dataset_key, installation_key,
                            publishing_organization_key, external, type, sub_type, title, alias, abbreviation,
                            description, language, homepage, logo_url, citation, citation_identifier, rights,
                            locked_for_auto_update, created_by, modified_by, created, modified, deleted,
                            fulltext_search, doi, license, maintenance_update_frequency, version)
VALUES ('7c57400d-1ee7-449b-a152-aefed2f70a2c', null, null, '2fe63cec-9b23-4974-bab1-9f4118ef7711',
        '36107c15-771c-4810-a298-b7558828b8bd', false, 'OCCURRENCE', null, 'Test Dataset Registry 3', null, null,
        'Description of Test Dataset 3', 'en', 'http://www.homepage.com', 'http://www.logo.com/2', 'Citation stuff',
        null, null, false, 'WS TEST', 'WS TEST', '2019-11-12 08:49:53.062721', '2019-11-12 08:49:53.062721', null,
        '''dataset'':2,8 ''descript'':5 ''occurr'':4 ''registry2'':3 ''test'':1,7 ''www.homepage.com'':9',
        '10.21373/dcb', 'UNSPECIFIED', null, null);


INSERT INTO public.pipeline_process (key, dataset_key, attempt, created, created_by) VALUES (1, 'd82273f6-9738-48a5-a639-2086f9c49d18', 1, '2019-12-20 09:22:33.137140', 'WS TEST');
INSERT INTO public.pipeline_process (key, dataset_key, attempt, created, created_by) VALUES (2, 'd82273f6-9738-48a5-a639-2086f9c49d18', 2, '2019-12-20 09:22:33.177763', 'WS TEST');
INSERT INTO public.pipeline_process (key, dataset_key, attempt, created, created_by) VALUES (3, 'd82273f6-9738-48a5-a639-2086f9c49d18', 3, '2019-12-20 09:22:33.188937', 'WS TEST');
INSERT INTO public.pipeline_process (key, dataset_key, attempt, created, created_by) VALUES (4, '7c57400d-1ee7-449b-a152-aefed2f70a2c', 1, '2019-12-20 09:22:33.137140', 'WS TEST');

INSERT INTO public.pipeline_execution (key, pipeline_process_key, steps_to_run, created, created_by, rerun_reason, remarks)
VALUES (11, 1, '{DWCA_TO_VERBATIM}', '2019-12-20 13:45:20.141051', 'WS TEST', 'rerun', 'remarks');
INSERT INTO public.pipeline_execution (key, pipeline_process_key, steps_to_run, created, created_by, rerun_reason, remarks)
VALUES (12, 3, '{DWCA_TO_VERBATIM,XML_TO_VERBATIM,ABCD_TO_VERBATIM,VERBATIM_TO_INTERPRETED,INTERPRETED_TO_INDEX,HDFS_VIEW}', '2019-12-20 13:45:21.141051', 'WS TEST', 'rerun', 'remarks');
INSERT INTO public.pipeline_execution (key, pipeline_process_key, steps_to_run, created, created_by, rerun_reason, remarks)
VALUES (21, 3, '{DWCA_TO_VERBATIM,XML_TO_VERBATIM,ABCD_TO_VERBATIM,VERBATIM_TO_INTERPRETED,INTERPRETED_TO_INDEX,HDFS_VIEW}', '2019-12-20 13:45:22.141051', 'WS TEST', 'rerun', 'remarks');

INSERT INTO public.pipeline_execution (key, pipeline_process_key, steps_to_run, created, created_by, rerun_reason, remarks)
VALUES (13, 4, '{DWCA_TO_VERBATIM}', '2019-12-20 13:45:23.141051', 'WS TEST', 'rerun', 'remarks');

INSERT INTO public.pipeline_step (key, type, runner, started, finished, state, message, metrics, created_by, modified, modified_by, pipeline_execution_key, number_records, pipelines_version)
VALUES (101, 'ABCD_TO_VERBATIM', 'STANDALONE', '2019-12-20 13:45:20.167000', null, 'RUNNING', 'message', '', 'WS TEST', null, null, 11, null, null);

INSERT INTO public.pipeline_step (key, type, runner, started, finished, state, message, metrics, created_by, modified, modified_by, pipeline_execution_key, number_records, pipelines_version)
VALUES (102, 'DWCA_TO_VERBATIM', 'STANDALONE', '2019-12-27 09:42:11.519000', null, 'COMPLETED', '{"datasetUuid":"d82273f6-9738-48a5-a639-2086f9c49d18","datasetType":"OCCURRENCE","source":"http://gbif.vm.ntnu.no/ipt/archive.do?r=setesdal_veg_data","attempt":109,"validationReport":{"datasetKey":"418a6571-b6c1-4db0-b90e-8f36bde4c80e","occurrenceReport":{"checkedRecords":11961,"uniqueTriplets":0,"allRecordsChecked":true,"recordsWithInvalidTriplets":11961,"uniqueOccurrenceIds":11961,"recordsMissingOccurrenceId":0,"invalidationReason":null,"valid":true},"genericReport":{"checkedRecords":1630,"allRecordsChecked":true,"duplicateIds":[],"rowNumbersMissingId":[],"invalidationReason":null,"valid":true},"invalidationReason":null,"valid":true},"pipelineSteps":["DWCA_TO_VERBATIM","HDFS_VIEW","VERBATIM_TO_INTERPRETED","INTERPRETED_TO_INDEX"],"endpointType":"DWC_ARCHIVE","platform":"ALL"}', '', 'WS TEST', null, null, 12, null, null);
INSERT INTO public.pipeline_step (key, type, runner, started, finished, state, message, metrics, created_by, modified, modified_by, pipeline_execution_key, number_records, pipelines_version)
VALUES (111, 'XML_TO_VERBATIM', 'STANDALONE', '2019-12-27 09:42:12.519000', null, 'COMPLETED', '{"datasetUuid":"d82273f6-9738-48a5-a639-2086f9c49d18","attempt":109,"totalRecordCount":0,"reason":"NORMAL","pipelineSteps":["DWCA_TO_VERBATIM","HDFS_VIEW","VERBATIM_TO_INTERPRETED","INTERPRETED_TO_INDEX"],"endpointType":"DWC_ARCHIVE","platform":"ALL"}', '', 'WS TEST', null, null, 12, null, null);
INSERT INTO public.pipeline_step (key, type, runner, started, finished, state, message, metrics, created_by, modified, modified_by, pipeline_execution_key, number_records, pipelines_version)
VALUES (112, 'ABCD_TO_VERBATIM', 'STANDALONE', '2019-12-27 09:42:13.519000', null, 'COMPLETED', '{"datasetUuid":"d82273f6-9738-48a5-a639-2086f9c49d18","datasetType":"OCCURRENCE","source":"http://gbif.vm.ntnu.no/ipt/archive.do?r=setesdal_veg_data","attempt":109,"validationReport":{"datasetKey":"418a6571-b6c1-4db0-b90e-8f36bde4c80e","occurrenceReport":{"checkedRecords":11961,"uniqueTriplets":0,"allRecordsChecked":true,"recordsWithInvalidTriplets":11961,"uniqueOccurrenceIds":11961,"recordsMissingOccurrenceId":0,"invalidationReason":null,"valid":true},"genericReport":{"checkedRecords":1630,"allRecordsChecked":true,"duplicateIds":[],"rowNumbersMissingId":[],"invalidationReason":null,"valid":true},"invalidationReason":null,"valid":true},"pipelineSteps":["DWCA_TO_VERBATIM","HDFS_VIEW","VERBATIM_TO_INTERPRETED","INTERPRETED_TO_INDEX"],"endpointType":"DWC_ARCHIVE","platform":"ALL"}', '', 'WS TEST', null, null, 12, null, null);
INSERT INTO public.pipeline_step (key, type, runner, started, finished, state, message, metrics, created_by, modified, modified_by, pipeline_execution_key, number_records, pipelines_version)
VALUES (113, 'VERBATIM_TO_INTERPRETED', 'STANDALONE', '2019-12-27 09:42:14.519000', null, 'COMPLETED', '{"datasetUuid":"d82273f6-9738-48a5-a639-2086f9c49d18","attempt":109,"pipelineSteps":["DWCA_TO_VERBATIM","HDFS_VIEW","VERBATIM_TO_INTERPRETED","INTERPRETED_TO_INDEX"],"interpretTypes":["TYPE"],"endpointType":"DWC_ARCHIVE"}', '', 'WS TEST', null, null, 12, null, null);
INSERT INTO public.pipeline_step (key, type, runner, started, finished, state, message, metrics, created_by, modified, modified_by, pipeline_execution_key, number_records, pipelines_version)
VALUES (114, 'INTERPRETED_TO_INDEX', 'STANDALONE', '2019-12-27 09:42:15.519000', null, 'COMPLETED', '{"datasetUuid":"d82273f6-9738-48a5-a639-2086f9c49d18","datasetType":"OCCURRENCE","source":"http://gbif.vm.ntnu.no/ipt/archive.do?r=setesdal_veg_data","attempt":109,"validationReport":{"datasetKey":"418a6571-b6c1-4db0-b90e-8f36bde4c80e","occurrenceReport":{"checkedRecords":11961,"uniqueTriplets":0,"allRecordsChecked":true,"recordsWithInvalidTriplets":11961,"uniqueOccurrenceIds":11961,"recordsMissingOccurrenceId":0,"invalidationReason":null,"valid":true},"genericReport":{"checkedRecords":1630,"allRecordsChecked":true,"duplicateIds":[],"rowNumbersMissingId":[],"invalidationReason":null,"valid":true},"invalidationReason":null,"valid":true},"pipelineSteps":["DWCA_TO_VERBATIM","HDFS_VIEW","VERBATIM_TO_INTERPRETED","INTERPRETED_TO_INDEX"],"endpointType":"DWC_ARCHIVE","platform":"ALL"}', '', 'WS TEST', null, null, 12, null, null);
INSERT INTO public.pipeline_step (key, type, runner, started, finished, state, message, metrics, created_by, modified, modified_by, pipeline_execution_key, number_records, pipelines_version)
VALUES (115, 'HDFS_VIEW', 'STANDALONE', '2019-12-27 09:42:16.519000', null, 'COMPLETED', '{"datasetUuid":"d82273f6-9738-48a5-a639-2086f9c49d18","datasetType":"OCCURRENCE","source":"http://gbif.vm.ntnu.no/ipt/archive.do?r=setesdal_veg_data","attempt":109,"validationReport":{"datasetKey":"418a6571-b6c1-4db0-b90e-8f36bde4c80e","occurrenceReport":{"checkedRecords":11961,"uniqueTriplets":0,"allRecordsChecked":true,"recordsWithInvalidTriplets":11961,"uniqueOccurrenceIds":11961,"recordsMissingOccurrenceId":0,"invalidationReason":null,"valid":true},"genericReport":{"checkedRecords":1630,"allRecordsChecked":true,"duplicateIds":[],"rowNumbersMissingId":[],"invalidationReason":null,"valid":true},"invalidationReason":null,"valid":true},"pipelineSteps":["DWCA_TO_VERBATIM","HDFS_VIEW","VERBATIM_TO_INTERPRETED","INTERPRETED_TO_INDEX"],"endpointType":"DWC_ARCHIVE","platform":"ALL"}', '', 'WS TEST', null, null, 12, null, null);

INSERT INTO public.pipeline_step (key, type, runner, started, finished, state, message, metrics, created_by, modified, modified_by, pipeline_execution_key, number_records, pipelines_version)
VALUES (103, 'DWCA_TO_VERBATIM', 'STANDALONE', '2019-12-27 09:42:17.519000', null, 'COMPLETED', '{"datasetUuid":"4348adaa-d744-4241-92a0-ebf9d55eb9bb","datasetType":"OCCURRENCE","source":"http://gbif.vm.ntnu.no/ipt/archive.do?r=setesdal_veg_data","attempt":109,"validationReport":{"datasetKey":"418a6571-b6c1-4db0-b90e-8f36bde4c80e","occurrenceReport":{"checkedRecords":11961,"uniqueTriplets":0,"allRecordsChecked":true,"recordsWithInvalidTriplets":11961,"uniqueOccurrenceIds":11961,"recordsMissingOccurrenceId":0,"invalidationReason":null,"valid":true},"genericReport":{"checkedRecords":1630,"allRecordsChecked":true,"duplicateIds":[],"rowNumbersMissingId":[],"invalidationReason":null,"valid":true},"invalidationReason":null,"valid":true},"pipelineSteps":["DWCA_TO_VERBATIM","HDFS_VIEW","VERBATIM_TO_INTERPRETED","INTERPRETED_TO_INDEX"],"endpointType":"DWC_ARCHIVE","platform":"ALL"}', '', 'WS TEST', null, null, 13, null, null);
