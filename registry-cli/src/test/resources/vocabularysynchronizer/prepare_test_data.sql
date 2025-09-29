-- Prepare test data for vocabulary synchronizer tests
-- This file sets up test datasets and dataset categories for testing

-- Insert test node
INSERT INTO node (key, gbif_region, continent, title, country, created_by, modified_by,
                 created, modified, deleted, fulltext_search, type, participation_status)
VALUES ('a49e75d9-7b07-4d01-9be8-6ab2133f42f9', 'EUROPE', 'EUROPE', 'The UK National Node', 'GB',
        'WS TEST', 'WS TEST', '2020-02-22 09:54:09.835039', '2020-02-22 09:54:09.835039', null,
        '''countri'':5 ''europ'':7,8 ''gb'':9 ''nation'':3 ''node'':4 ''uk'':2 ''vote'':6',
        'COUNTRY', 'VOTING');

-- Insert test organization
INSERT INTO organization (key, endorsing_node_key, endorsement_approved, password, title,
                         abbreviation, description, language, logo_url, city, province,
                         country, postal_code, latitude, longitude, created_by, modified_by,
                         created, modified, deleted, fulltext_search, email, phone,
                         homepage, address, challenge_code_key)
VALUES ('ff593857-44c2-4011-be20-8403e8d0bd9a', 'a49e75d9-7b07-4d01-9be8-6ab2133f42f9', false,
        'password', 'The BGBM', 'BGBM', 'The Berlin Botanical...', 'de', 'http://www.example.org',
        'BERLIN', 'BERLIN', 'GB', '1408', null, null, 'WS TEST', 'WS TEST',
        '2020-02-22 09:54:09.988088', '2020-02-22 09:54:09.988088', null,
        '''1408'':16 ''2920202'':9 ''a@b.com'':8 ''berlin'':5,12,13,14 ''bgbm'':2,3 ''botan'':6 ''de'':7 ''gb'':15 ''www.example.org'':10,11',
        '{a@b.com}', '{2920202}', '{http://www.example.org}', '{Berliner}', null);

-- Insert test installation
INSERT INTO installation (key, organization_key, type, title, description, created_by,
                         modified_by, created, modified, deleted, fulltext_search, password,
                         disabled)
VALUES ('1e9136f0-78fd-40cd-8b25-26c78a376d8d', 'ff593857-44c2-4011-be20-8403e8d0bd9a',
        'IPT_INSTALLATION', 'The BGBM BIOCASE INSTALLATION', 'The Berlin Botanical...', 'WS TEST',
        'WS TEST', '2020-02-22 09:54:10.094782', '2020-02-22 09:54:10.094782', null,
        '''berlin'':8 ''bgbm'':2 ''biocas'':3 ''botan'':9 ''instal'':4,6 ''ipt'':5', null, false);

-- Insert test dataset with category array (using simple test values)
INSERT INTO dataset (key, parent_dataset_key, duplicate_of_dataset_key, installation_key, publishing_organization_key, external, type, sub_type, title, alias, abbreviation, description, language, homepage, logo_url, citation, citation_identifier, rights, locked_for_auto_update, created_by, modified_by, created, modified, deleted, fulltext_search, doi, license, maintenance_update_frequency, version, category)
VALUES (
    '38f06820-08c5-42b2-94f6-47cc3e83a54a',
    null,
    null,
    '1e9136f0-78fd-40cd-8b25-26c78a376d8d',
    'ff593857-44c2-4011-be20-8403e8d0bd9a',
    false,
    'OCCURRENCE',
    null,
    'Test Dataset for Vocabulary Synchronizer',
    'BGBM',
    'BGBM',
    'Test dataset',
    'da',
    'http://www.example.org',
    'http://www.example.org',
    'This is a citation text',
    'ABC',
    'The rights',
    false,
    'CLI TEST',
    'CLI TEST',
    '2020-02-22 09:54:10.223198',
    '2020-02-21 23:00:00.000000',
    null,
    '''test'':1 ''dataset'':2 ''vocabulary'':4 ''synchronizer'':5',
    '10.21373/gbif.2014.xsd123',
    'CC_BY_4_0',
    null,
    null,
    ARRAY['test-category', 'sample-category']
); 