INSERT INTO public.node (key, gbif_region, continent, title, country, created_by, modified_by, created, modified,
                         deleted, fulltext_search, type, participation_status)
VALUES ('25871695-fe22-4407-894d-cb595d209690', 'EUROPE', 'EUROPE', 'The UK National Node', 'GB', 'WS TEST', 'WS TEST',
        '2019-11-18 10:09:04.077148', '2019-11-18 10:09:04.077148', null,
        '''countri'':5 ''europ'':7,8 ''gb'':9 ''nation'':3 ''node'':4 ''uk'':2 ''vote'':6', 'COUNTRY', 'VOTING');

INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title, abbreviation,
                                 description, language, logo_url, city, province, country, postal_code, latitude,
                                 longitude, created_by, modified_by, created, modified, deleted, fulltext_search, email,
                                 phone, homepage, address, challenge_code_key)
VALUES ('0af41159-061f-4693-b2e5-d3d062a8285d', '25871695-fe22-4407-894d-cb595d209690', false, 'password', 'The BGBM',
        'BGBM', 'The Berlin Botanical...', 'de', 'http://www.example.org', 'BERLIN', 'BERLIN', 'DE', '1408', null, null,
        'WS TEST', 'WS TEST', '2019-11-18 09:28:17.396510', '2019-11-18 09:28:17.396510', null,
        '''1408'':16 ''2920202'':9 ''a@b.com'':8 ''berlin'':5,12,13,14 ''bgbm'':2,3 ''botan'':6 ''de'':7,15 ''www.example.org'':10,11',
        '{a@b.com}', '{2920202}', '{http://www.example.org}', '{Berliner}', null);
INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title, abbreviation,
                                 description, language, logo_url, city, province, country, postal_code, latitude,
                                 longitude, created_by, modified_by, created, modified, deleted, fulltext_search, email,
                                 phone, homepage, address, challenge_code_key)
VALUES ('d3894ec1-6eb0-48c8-bbb1-0a63f8731159', '25871695-fe22-4407-894d-cb595d209690', false, 'password',
        'The Second Org',
        'SO', 'Test second org', 'de', 'http://www.example.org', 'BERLIN', 'BERLIN', 'DE', '1408', null, null,
        'WS TEST', 'WS TEST', '2019-11-18 09:28:17.396510', '2019-11-18 09:28:17.396510', null,
        '''1408'':16 ''2920202'':9 ''a@b.com'':8 ''berlin'':5,12,13,14 ''bgbm'':2,3 ''botan'':6 ''de'':7,15 ''www.example.org'':10,11',
        '{a@b.com}', '{2920202}', '{http://www.example.org}', '{Berliner}', null);
INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title, abbreviation,
                                 description, language, logo_url, city, province, country, postal_code, latitude,
                                 longitude, created_by, modified_by, created, modified, deleted, fulltext_search, email,
                                 phone, homepage, address, challenge_code_key)
VALUES ('c55dc610-63a2-4fbf-919d-0ad74b4f24dd', '25871695-fe22-4407-894d-cb595d209690', false, 'password', 'The Third Org',
        'BGBM', 'The Berlin Botanical...', 'de', 'http://www.example.org', 'BERLIN', 'BERLIN', 'DE', '1408', null, null,
        'WS TEST', 'WS TEST', '2019-11-20 13:32:06.366020', '2019-11-20 13:32:06.366020', null,
        '''1408'':16 ''2920202'':9 ''a@b.com'':8 ''berlin'':5,12,13,14 ''bgbm'':2,3 ''botan'':6 ''de'':7,15 ''www.example.org'':10,11',
        '{a@b.com}', '{2920202}', '{http://www.example.org}', '{Berliner}', null);

INSERT INTO public.contact (key, last_name, description, organization, city, province, country, postal_code, created_by,
                            modified_by, created, modified, fulltext_search, first_name, user_id, homepage, position,
                            email, phone, address)
VALUES (1987, 'Robertson', 'Description stuff', 'GBIF', 'Copenhagen', 'Capital', 'DK', '2100', 'WS TEST',
        'WS TEST', '2019-11-18 10:14:36.264343', '2019-11-18 10:14:36.264343',
        '''+45'':9 ''15'':13 ''175cm'':4 ''2100'':17 ''28261487'':10 ''dk'':16 ''gbif'':11 ''geeki'':5 ''kobenhavn'':14 ''programm'':7 ''robertson'':2 ''scruffi'':6 ''sjaelland'':15 ''tim'':1 ''test@mailinator.com'':8 ''universitetsparken'':12',
        'Tim', '{}', '{}', '{Programmer}', '{test@mailinator.com}', '{+45 28261487}', '{Universitetsparken 15}');
INSERT INTO public.contact (key, last_name, description, organization, city, province, country, postal_code, created_by,
                            modified_by, created, modified, fulltext_search, first_name, user_id, homepage, position,
                            email, phone, address)
VALUES (2042, 'Robertson', 'Description stuff...', 'GBIF', 'Copenhagen', 'Capital', 'DK', '2100', 'WS TEST',
        'WS TEST', '2019-11-20 13:32:06.448911', '2019-11-20 13:32:06.448911', null, 'Tim', '{}', '{}', '{Programmer}',
        '{}', '{+45 28261487}', '{Universitetsparken 15}');

INSERT INTO public.organization_contact (organization_key, contact_key, type, is_primary)
VALUES ('c55dc610-63a2-4fbf-919d-0ad74b4f24dd', 2042, 'TECHNICAL_POINT_OF_CONTACT', true);
INSERT INTO public.organization_contact (organization_key, contact_key, type, is_primary)
VALUES ('0af41159-061f-4693-b2e5-d3d062a8285d', 1987, 'TECHNICAL_POINT_OF_CONTACT', true);
