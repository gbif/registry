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
        'WS TEST', '2019-11-01 14:48:02.486307', '2019-11-01 14:48:02.486307', null,
        null, 'welcome', false);
