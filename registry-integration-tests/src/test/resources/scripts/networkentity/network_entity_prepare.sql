INSERT INTO public.node (key, gbif_region, continent, title, country, created_by, modified_by, created, modified,
                         deleted, fulltext_search, type, participation_status)
VALUES ('f698c938-d36a-41ac-8120-c35903e1acb9', 'EUROPE', 'EUROPE', 'UK Node', 'GB', 'WS TEST', 'WS TEST',
        '2019-10-15 08:41:46.006372', '2019-10-15 08:41:46.006372', null,
        '''countri'':5 ''europ'':7,8 ''gb'':9 ''nation'':3 ''node'':4 ''uk'':2 ''vote'':6', 'COUNTRY', 'VOTING');

INSERT INTO public.network (key, title, description, language, logo_url, city, province, country, postal_code,
                            created_by, modified_by, created, modified, deleted, fulltext_search, email, phone,
                            homepage, address)
VALUES ('83a2c5d5-5d85-4c9e-ac80-dc9e14766e7e', 'SPANISH NETWORK', 'This holds some datasets', 'es',
        'http://www.example.org', 'Madrid', 'MADRID', 'ES', '1408', 'WS TEST', 'WS TEST', '2019-12-06 17:22:56.218372',
        '2019-12-06 17:22:56.218372', null,
        '''1408'':19 ''2920202'':12 ''come'':7 ''dataset'':6 ''es'':10,18 ''hold'':4 ''madrid'':15,16,17 ''network'':2 ''spain'':9 ''spanish'':1 ''www.example.org'':11,13,14',
        '{http://www.example.org}', '{2920202}', '{http://www.example.org}', '{Madrid}');

INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title, abbreviation,
                                 description, language, logo_url, city, province, country, postal_code, latitude,
                                 longitude, created_by, modified_by, created, modified, deleted, fulltext_search, email,
                                 phone, homepage, address, challenge_code_key)
VALUES ('f433944a-ad93-4ea8-bad7-68de7348e65a', 'f698c938-d36a-41ac-8120-c35903e1acb9', false, 'password', 'The ORG',
        'BGBM', 'The Berlin Botanical...', 'de', 'http://www.example.org', 'BERLIN', 'BERLIN', 'DE', '1408', null, null,
        'WS TEST', 'WS TEST', '2019-10-15 08:44:43.732781', '2019-10-15 08:44:43.732781', null,
        '''1408'':15 ''2920202'':8 ''a@b.com'':7 ''berlin'':4,11,12,13 ''bgbm'':2 ''botan'':5 ''de'':6,14 ''tim'':1 ''www.example.org'':9,10',
        '{a@b.com}', '{2920202}', '{http://www.example.org}', '{Berliner}', null);

INSERT INTO public.installation (key, organization_key, type, title, description, created_by, modified_by, created,
                                 modified, deleted, fulltext_search, password, disabled)
VALUES ('70d1ffaf-8e8a-4f40-9c5d-00a0ddbefa4c', 'f433944a-ad93-4ea8-bad7-68de7348e65a', 'IPT_INSTALLATION', 'New Title',
        'The Berlin Botanical...', 'WS TEST', 'WS TEST', '2019-10-23 11:45:22.742157',
        '2019-10-23 11:45:22.948114',
        null, '''berlin'':6 ''botan'':7 ''instal'':4 ''ipt'':3 ''new'':1 ''titl'':2', null, false);

INSERT INTO public.dataset (key, parent_dataset_key, duplicate_of_dataset_key, installation_key,
                            publishing_organization_key, external, type, sub_type, title, alias, abbreviation,
                            description, language, homepage, logo_url, citation, citation_identifier, rights,
                            locked_for_auto_update, created_by, modified_by, created, modified, deleted,
                            fulltext_search, doi, license, maintenance_update_frequency, version)
VALUES ('d82273f6-9738-48a5-a639-2086f9c49d18', null, null, '70d1ffaf-8e8a-4f40-9c5d-00a0ddbefa4c',
        'f433944a-ad93-4ea8-bad7-68de7348e65a', false, 'OCCURRENCE', null, 'Test Dataset Registry2', null, null,
        'Description of Test Dataset', 'en', 'http://www.homepage.com', 'http://www.logo.com/1', null, null, null,
        false, 'WS TEST', 'WS TEST',
        '2019-11-12 08:49:53.062721', '2019-11-12 08:49:53.062721', null,
        '''dataset'':2,8 ''descript'':5 ''occurr'':4 ''registry2'':3 ''test'':1,7 ''www.homepage.com'':9',
        '10.21373/h9c3vc', 'UNSPECIFIED', null, null);
