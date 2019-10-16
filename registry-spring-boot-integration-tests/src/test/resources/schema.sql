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

INSERT INTO public."user" (key, username, email, password, first_name, last_name, roles, settings, system_settings,
                           created, last_login, deleted, challenge_code_key)
VALUES (4, 'user_13', 'user_13@gbif.org', '$S$DsO2Zyy5gdu98q5XBHbLMKVkMdayt/Y5lJLvjafCL42yUSTqF1Gh', 'Tim',
        'Robertson', '{USER}', 'country => dk, language => en', '', '2019-08-02 08:54:42.327579',
        '2019-08-02 08:54:42.582421', null, null);
INSERT INTO public."user" (key, username, email, password, first_name, last_name, roles, settings, system_settings,
                           created, last_login, deleted, challenge_code_key)
VALUES (5, 'user_12', 'user_12@gbif.org', '$S$DhH0xHYrr2f/OSMJcRbD4Vg3tjceFQI798AEWrUofr8fCObUrmEC', 'Tim',
        'Robertson', '{USER}', 'country => dk, language => en', '', '2019-08-02 08:54:42.616587',
        '2019-08-02 08:54:42.667163', null, null);
INSERT INTO public."user" (key, username, email, password, first_name, last_name, roles, settings, system_settings,
                           created, last_login, deleted, challenge_code_key)
VALUES (6, 'user_reset_password', 'user_reset_password@gbif.org',
        '$S$DhH0xHYrr2f/OSMJcRbD4Vg3tjceFQI798AEWrUofr8fCObUrmEC', 'Tim',
        'Robertson', '{USER}', 'country => dk, language => en', '', '2019-08-02 08:54:42.616587',
        '2019-08-02 08:54:42.667163', null, null);
INSERT INTO public.challenge_code (key, challenge_code, created)
VALUES (1, 'd4f26f30-c006-11e9-9cb5-2a2ae2dbcce4', '2019-08-02 08:54:42.616587');
INSERT INTO public."user" (key, username, email, password, first_name, last_name, roles, settings, system_settings,
                           created, last_login, deleted, challenge_code_key)
VALUES (7, 'user_update_password', 'user_update_password@gbif.org',
        '$S$DhH0xHYrr2f/OSMJcRbD4Vg3tjceFQI798AEWrUofr8fCObUrmEC', 'Tim',
        'Robertson', '{USER}', 'country => dk, language => en', '', '2019-08-02 08:54:42.616587',
        '2019-08-02 08:54:42.667163', null, 1);

-- INSERT INTO public.node (key, gbif_region, continent, title, country, created_by, modified_by, created, modified,
--                          deleted, fulltext_search, type, participation_status)
-- VALUES ('710970cf-e3f1-4e74-b09c-d8c86b9819d9', 'EUROPE', 'EUROPE', 'Org', null, 'mike', 'mike',
--         '2019-09-12 12:10:28.917000', '2019-09-12 12:10:32.189000', null, null, 'COUNTRY', 'ASSOCIATE');
--
-- INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title, abbreviation,
--                                  description, language, logo_url, city, province, country, postal_code, latitude,
--                                  longitude, created_by, modified_by, created, modified, deleted, fulltext_search, email,
--                                  phone, homepage, address, challenge_code_key)
-- VALUES ('36107c15-771c-4810-a298-b7558828b8bd', '710970cf-e3f1-4e74-b09c-d8c86b9819d9', true, 'welcome', 'Org', null,
--         null, 'en', null, null, null, null, null, null, null, 'mike', 'mike', '2019-09-12 12:06:01.835000',
--         '2019-09-12 12:06:04.366000', null, null, null, null, null, null, null);