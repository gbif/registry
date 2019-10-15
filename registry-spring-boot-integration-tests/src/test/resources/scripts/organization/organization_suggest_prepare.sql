INSERT INTO public.node (key, gbif_region, continent, title, country, created_by, modified_by, created, modified,
                         deleted, fulltext_search, type, participation_status)
VALUES ('f698c938-d36a-41ac-8120-c35903e1acb9', 'EUROPE', 'EUROPE', 'The UK National Node', 'GB', 'WS TEST', 'WS TEST',
        '2019-10-15 08:41:46.006372', '2019-10-15 08:41:46.006372', null,
        '''countri'':5 ''europ'':7,8 ''gb'':9 ''nation'':3 ''node'':4 ''uk'':2 ''vote'':6', 'COUNTRY', 'VOTING');


INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title, abbreviation,
                                 description, language, logo_url, city, province, country, postal_code, latitude,
                                 longitude, created_by, modified_by, created, modified, deleted, fulltext_search, email,
                                 phone, homepage, address, challenge_code_key)
VALUES ('f433944a-ad93-4ea8-bad7-68de7348e65a', 'f698c938-d36a-41ac-8120-c35903e1acb9', false, 'password', 'Tim',
        'BGBM', 'The Berlin Botanical...', 'de', 'http://www.example.org', 'BERLIN', 'BERLIN', 'DE', '1408', null, null,
        'WS TEST', 'WS TEST', '2019-10-15 08:44:43.732781', '2019-10-15 08:44:43.732781', null,
        '''1408'':15 ''2920202'':8 ''a@b.com'':7 ''berlin'':4,11,12,13 ''bgbm'':2 ''botan'':5 ''de'':6,14 ''tim'':1 ''www.example.org'':9,10',
        '{a@b.com}', '{2920202}', '{http://www.example.org}', '{Berliner}', null);
INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title, abbreviation,
                                 description, language, logo_url, city, province, country, postal_code, latitude,
                                 longitude, created_by, modified_by, created, modified, deleted, fulltext_search, email,
                                 phone, homepage, address, challenge_code_key)
VALUES ('a13be6bd-5e3d-4157-8751-81681823c391', 'f698c938-d36a-41ac-8120-c35903e1acb9', false, 'password', 'The Tim',
        'BGBM', 'The Berlin Botanical...', 'de', 'http://www.example.org', 'BERLIN', 'BERLIN', 'DE', '1408', null, null,
        'WS TEST', 'WS TEST', '2019-10-15 08:44:43.784711', '2019-10-15 08:44:43.784711', null,
        '''1408'':16 ''2920202'':9 ''a@b.com'':8 ''berlin'':5,12,13,14 ''bgbm'':3 ''botan'':6 ''de'':7,15 ''tim'':2 ''www.example.org'':10,11',
        '{a@b.com}', '{2920202}', '{http://www.example.org}', '{Berliner}', null);