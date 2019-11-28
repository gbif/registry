DELETE FROM contact;
DELETE FROM endpoint;
DELETE FROM tag;
DELETE FROM identifier;
DELETE FROM comment;
DELETE FROM node_identifier;
DELETE FROM node_machine_tag;
DELETE FROM node_tag;
DELETE FROM node_comment;
DELETE FROM occurrence_download;
DELETE FROM organization_contact;
DELETE FROM organization_endpoint;
DELETE FROM organization_machine_tag;
DELETE FROM organization_tag;
DELETE FROM organization_identifier;
DELETE FROM organization_comment;
DELETE FROM installation_contact;
DELETE FROM installation_endpoint;
DELETE FROM installation_machine_tag;
DELETE FROM installation_tag;
DELETE FROM installation_comment;
DELETE FROM dataset_contact;
DELETE FROM dataset_endpoint;
DELETE FROM dataset_machine_tag;
DELETE FROM dataset_tag;
DELETE FROM dataset_identifier;
DELETE FROM dataset_comment;
DELETE FROM network_contact;
DELETE FROM network_endpoint;
DELETE FROM network_machine_tag;
DELETE FROM network_tag;
DELETE FROM network_comment;
DELETE FROM machine_tag;
DELETE FROM metadata;
DELETE FROM editor_rights;
DELETE FROM network;
DELETE FROM dataset;
DELETE FROM installation;
DELETE FROM organization;
DELETE FROM node;
DELETE FROM public."user";
DELETE FROM challenge_code;
DELETE FROM collection_collection_person;
DELETE FROM collection_identifier;
DELETE FROM collection_tag;
DELETE FROM institution_collection_person;
DELETE FROM institution_identifier;
DELETE FROM institution_tag;
DELETE FROM collection_person;
DELETE FROM collection;
DELETE FROM institution;
DELETE FROM address;
TRUNCATE gbif_doi;
DELETE FROM pipeline_step;
DELETE FROM pipeline_process;


INSERT INTO public."user" (key, username, email, password, first_name, last_name, roles, settings, system_settings,
                           created, last_login, deleted, challenge_code_key)
VALUES (2, 'justuser', 'justuser@gbif.org', '$S$DSLeulP5GbaEzGpqDSJJVG8mFUisQP.Bmy/S15VVbG9aadZQ6KNp', 'John',
        'Doe', '{USER}', '', '', '2019-07-12 09:57:42.629508', null, null, null);

INSERT INTO public."user" (key, username, email, password, first_name, last_name, roles, settings, system_settings,
                           created, last_login, deleted, challenge_code_key)
VALUES (3, 'justadmin', 'justadmin@gbif.org', '$S$DSLeulP5GbaEzGpqDSJJVG8mFUisQP.Bmy/S15VVbG9aadZQ6KNp', 'Joe',
        'Doe', '{REGISTRY_ADMIN}', '', 'my.settings.key => 100_tacos=100$', '2019-07-12 10:02:03.778207', null, null,
        null);
