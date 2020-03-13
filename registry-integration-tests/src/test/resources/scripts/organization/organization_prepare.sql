-- Prepare 2 organization for suggest testing
INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title, abbreviation,
                                 description, language, logo_url, city, province, country, postal_code, latitude,
                                 longitude, created_by, modified_by, created, modified, deleted, fulltext_search, email,
                                 phone, homepage, address, challenge_code_key)
VALUES ('f433944a-ad93-4ea8-bad7-68de7348e65a', 'f698c938-d36a-41ac-8120-c35903e1acb9', false, 'password', 'The ORG',
        'BGBM', 'The Berlin Botanical...', 'de', 'http://www.example.org', 'BERLIN', 'BERLIN', 'DE', '1408', null, null,
        'WS TEST', 'WS TEST', '2019-10-15 08:44:43.732781', '2019-10-15 08:44:43.732781', null,
        '''1408'':15 ''2920202'':8 ''a@b.com'':7 ''berlin'':4,11,12,13 ''bgbm'':2 ''botan'':5 ''de'':6,14 ''tim'':1 ''www.example.org'':9,10',
        '{a@b.com}', '{2920202}', '{http://www.example.org}', '{Berliner}', null);
INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title, abbreviation,
                                 description, language, logo_url, city, province, country, postal_code, latitude,
                                 longitude, created_by, modified_by, created, modified, deleted, fulltext_search, email,
                                 phone, homepage, address, challenge_code_key)
VALUES ('180bc881-9c8f-445b-89d9-40cd099cbdc3', 'f698c938-d36a-41ac-8120-c35903e1acb9', false, 'password', 'The BGBM',
        'BGBM', 'The Berlin Botanical...', 'de', 'http://www.example.org', 'BERLIN', 'BERLIN', 'AO', '1408', null, null,
        'WS TEST', 'WS TEST', '2019-10-15 12:41:34.879457', '2019-10-15 12:41:34.879457', null,
        '''1408'':16 ''2920202'':9 ''a@b.com'':8 ''ao'':15 ''berlin'':5,12,13,14 ''bgbm'':2,3 ''botan'':6 ''de'':7 ''www.example.org'':10,11',
        '{a@b.com}', '{2920202}', '{http://www.example.org}', '{Berliner}', null);
INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title, abbreviation,
                                 description, language, logo_url, city, province, country, postal_code, latitude,
                                 longitude, created_by, modified_by, created, modified, deleted, fulltext_search, email,
                                 phone, homepage, address, challenge_code_key)
VALUES ('e47e4958-7dee-475b-98c7-07a2d7de8f96', 'f698c938-d36a-41ac-8120-c35903e1acb9', false, 'password', 'The BGBM',
        'BGBM', 'The Berlin Botanical...', 'de', 'http://www.example.org', 'BERLIN', 'BERLIN', 'AO', '1408', null, null,
        'WS TEST', 'WS TEST', '2019-10-15 12:41:34.915196', '2019-10-15 12:41:34.915196', null,
        '''1408'':16 ''2920202'':9 ''a@b.com'':8 ''ao'':15 ''berlin'':5,12,13,14 ''bgbm'':2,3 ''botan'':6 ''de'':7 ''www.example.org'':10,11',
        '{a@b.com}', '{2920202}', '{http://www.example.org}', '{Berliner}', null);
INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title, abbreviation,
                                 description, language, logo_url, city, province, country, postal_code, latitude,
                                 longitude, created_by, modified_by, created, modified, deleted, fulltext_search, email,
                                 phone, homepage, address, challenge_code_key)
VALUES ('99ba86c3-c295-46d4-9c51-812e985c34ef', 'f698c938-d36a-41ac-8120-c35903e1acb9', false, 'password', 'The BGBM',
        'BGBM', 'The Berlin Botanical...', 'de', 'http://www.example.org', 'BERLIN', 'BERLIN', 'DK', '1408', null, null,
        'WS TEST', 'WS TEST', '2019-10-15 12:41:34.946753', '2019-10-15 12:41:34.946753', null,
        '''1408'':16 ''2920202'':9 ''a@b.com'':8 ''berlin'':5,12,13,14 ''bgbm'':2,3 ''botan'':6 ''de'':7 ''dk'':15 ''www.example.org'':10,11',
        '{a@b.com}', '{2920202}', '{http://www.example.org}', '{Berliner}', null);
INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title, abbreviation,
                                 description, language, logo_url, city, province, country, postal_code, latitude,
                                 longitude, created_by, modified_by, created, modified, deleted, fulltext_search, email,
                                 phone, homepage, address, challenge_code_key)
VALUES ('adbf4090-d519-40d3-982b-a95021da6284', 'f698c938-d36a-41ac-8120-c35903e1acb9', false, 'password', 'The BGBM',
        'BGBM', 'The Berlin Botanical...', 'de', 'http://www.example.org', 'BERLIN', 'BERLIN', 'FR', '1408', null, null,
        'WS TEST', 'WS TEST', '2019-10-15 12:41:34.981241', '2019-10-15 12:41:34.981241', null,
        '''1408'':16 ''2920202'':9 ''a@b.com'':8 ''berlin'':5,12,13,14 ''bgbm'':2,3 ''botan'':6 ''de'':7 ''fr'':15 ''www.example.org'':10,11',
        '{a@b.com}', '{2920202}', '{http://www.example.org}', '{Berliner}', null);
INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title, abbreviation,
                                 description, language, logo_url, city, province, country, postal_code, latitude,
                                 longitude, created_by, modified_by, created, modified, deleted, fulltext_search, email,
                                 phone, homepage, address, challenge_code_key)
VALUES ('96161e4f-32be-4af4-ac69-cfddbd04b514', 'f698c938-d36a-41ac-8120-c35903e1acb9', false, 'password', 'The BGBM',
        'BGBM', 'The Berlin Botanical...', 'de', 'http://www.example.org', 'BERLIN', 'BERLIN', 'FR', '1408', null, null,
        'WS TEST', 'WS TEST', '2019-10-15 12:41:35.011999', '2019-10-15 12:41:35.011999', null,
        '''1408'':16 ''2920202'':9 ''a@b.com'':8 ''berlin'':5,12,13,14 ''bgbm'':2,3 ''botan'':6 ''de'':7 ''fr'':15 ''www.example.org'':10,11',
        '{a@b.com}', '{2920202}', '{http://www.example.org}', '{Berliner}', null);
INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title, abbreviation,
                                 description, language, logo_url, city, province, country, postal_code, latitude,
                                 longitude, created_by, modified_by, created, modified, deleted, fulltext_search, email,
                                 phone, homepage, address, challenge_code_key)
VALUES ('00e84fc3-3503-495d-ae0f-24e2e7671a71', 'f698c938-d36a-41ac-8120-c35903e1acb9', false, 'password', 'The BGBM',
        'BGBM', 'The Berlin Botanical...', 'de', 'http://www.example.org', 'BERLIN', 'BERLIN', null, '1408', null, null,
        'WS TEST', 'WS TEST', '2019-10-15 12:41:35.041543', '2019-10-15 12:41:35.041543', null,
        '''1408'':15 ''2920202'':9 ''a@b.com'':8 ''berlin'':5,12,13,14 ''bgbm'':2,3 ''botan'':6 ''de'':7 ''www.example.org'':10,11',
        '{a@b.com}', '{2920202}', '{http://www.example.org}', '{Berliner}', null);