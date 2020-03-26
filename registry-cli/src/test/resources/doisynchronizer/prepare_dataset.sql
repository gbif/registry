DELETE FROM public.network_contact;
DELETE FROM public.network_endpoint;
DELETE FROM public.network_comment;
DELETE FROM public.network_machine_tag;
DELETE FROM public.network_tag;
DELETE FROM public.network_identifier;
DELETE FROM public.network;

DELETE FROM public.metadata;
DELETE FROM public.dataset_contact;
DELETE FROM public.dataset_endpoint;
DELETE FROM public.dataset_comment;
DELETE FROM public.dataset_machine_tag;
DELETE FROM public.dataset_tag;
DELETE FROM public.dataset_identifier;
DELETE FROM public.dataset;

DELETE FROM public.installation_contact;
DELETE FROM public.installation_endpoint;
DELETE FROM public.installation_comment;
DELETE FROM public.installation_machine_tag;
DELETE FROM public.installation_tag;
DELETE FROM public.installation_identifier;
DELETE FROM public.installation;

DELETE FROM public.organization_contact;
DELETE FROM public.organization_endpoint;
DELETE FROM public.organization_comment;
DELETE FROM public.organization_machine_tag;
DELETE FROM public.organization_tag;
DELETE FROM public.organization_identifier;
DELETE FROM public.organization;

DELETE FROM public.node_endpoint;
DELETE FROM public.node_comment;
DELETE FROM public.node_machine_tag;
DELETE FROM public.node_tag;
DELETE FROM public.node_identifier;
DELETE FROM public.node;

DELETE FROM public.identifier;
DELETE FROM public.gbif_doi;


INSERT INTO public.node (key, gbif_region, continent, title, country, created_by, modified_by,
                         created, modified, deleted, fulltext_search, type, participation_status)
VALUES ('a49e75d9-7b07-4d01-9be8-6ab2133f42f9', 'EUROPE', 'EUROPE', 'The UK National Node', 'GB',
        'WS TEST', 'WS TEST', '2020-02-22 09:54:09.835039', '2020-02-22 09:54:09.835039', null,
        '''countri'':5 ''europ'':7,8 ''gb'':9 ''nation'':3 ''node'':4 ''uk'':2 ''vote'':6',
        'COUNTRY', 'VOTING');

INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title,
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

INSERT INTO public.installation (key, organization_key, type, title, description, created_by,
                                 modified_by, created, modified, deleted, fulltext_search, password,
                                 disabled)
VALUES ('1e9136f0-78fd-40cd-8b25-26c78a376d8d', 'ff593857-44c2-4011-be20-8403e8d0bd9a',
        'IPT_INSTALLATION', 'The BGBM BIOCASE INSTALLATION', 'The Berlin Botanical...', 'WS TEST',
        'WS TEST', '2020-02-22 09:54:10.094782', '2020-02-22 09:54:10.094782', null,
        '''berlin'':8 ''bgbm'':2 ''biocas'':3 ''botan'':9 ''instal'':4,6 ''ipt'':5', null, false);

INSERT INTO public.dataset (key, parent_dataset_key, duplicate_of_dataset_key, installation_key, publishing_organization_key, external, type, sub_type, title, alias, abbreviation, description, language, homepage, logo_url, citation, citation_identifier, rights, locked_for_auto_update, created_by, modified_by, created, modified, deleted, fulltext_search, doi, license, maintenance_update_frequency, version)
VALUES ('38f06820-08c5-42b2-94f6-47cc3e83a54a', null, null, '1e9136f0-78fd-40cd-8b25-26c78a376d8d', 'ff593857-44c2-4011-be20-8403e8d0bd9a', false, 'OCCURRENCE', null, 'DatasetSynchronizer test dataset', 'BGBM', 'BGBM', 'Test dataset', 'da', 'http://www.example.org', 'http://www.example.org', 'This is a citation text', 'ABC', 'The rights', false, 'CLI TEST', 'CLI TEST', '2020-02-22 09:54:10.223198', '2020-02-21 23:00:00.000000', null, '''255'':5 ''aladaglari'':44 ''berlin'':50 ''bgbm'':47,48 ''bolkar'':42 ''botan'':51 ''charact'':6,28,30 ''checklist'':46 ''citat'':56 ''daglari'':43 ''der'':39,41 ''exact'':33 ''german'':22 ''hochgebirgsregion'':40 ''languag'':23 ''long'':18 ''need'':2 ''pontaurus'':1 ''text'':57 ''titl'':10,19,34 ''turkei'':45 ''untersuchungen'':37 ''vegetationskundlich'':36 ''word'':24,26 ''www.example.org'':52', '10.21373/gbif.1584932725458', 'CC_BY_NC_4_0', null, null);

INSERT INTO public.gbif_doi (doi, type, target, status, metadata, created, modified) VALUES ('10.21373/gbif.1584932725458', 'DATASET', 'https://registry.gbif-dev.org/dataset/38f06820-08c5-42b2-94f6-47cc3e83a54a', 'REGISTERED', '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<resource xsi:schemaLocation="http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd" xmlns="http://datacite.org/schema/kernel-4" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <identifier identifierType="DOI">10.21373/gbif.1584932725458</identifier>
    <creators>
        <creator>
            <creatorName>Mike</creatorName>
        </creator>
    </creators>
    <titles>
        <title>Test DOI 10.21373/gbif.1584932725458 title</title>
    </titles>
    <publisher>Mike Podolskiy</publisher>
    <publicationYear>2020</publicationYear>
    <resourceType resourceTypeGeneral="Dataset"/>
</resource>
', '2020-03-23 03:05:25.693258', '2020-03-23 03:05:28.526516');

