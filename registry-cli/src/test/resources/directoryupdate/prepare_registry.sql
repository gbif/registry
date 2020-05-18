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

DELETE FROM public.contact;
DELETE FROM public.comment;
DELETE FROM public.tag;
DELETE FROM public.machine_tag;
DELETE FROM public.endpoint;
DELETE FROM public.identifier;

DELETE FROM public.gbif_doi;

DELETE FROM public.editor_rights;


INSERT INTO public.node (key, gbif_region, continent, title, country, created_by, modified_by,
                         created, modified, deleted, fulltext_search, type, participation_status)
VALUES ('a49e75d9-7b07-4d01-9be8-6ab2133f42f9', 'EUROPE', 'EUROPE', 'The UK National Node', 'GB',
        'WS TEST', 'WS TEST', '2020-02-22 09:54:09.835039', '2020-02-22 09:54:09.835039', null,
        '''countri'':5 ''europ'':7,8 ''gb'':9 ''nation'':3 ''node'':4 ''uk'':2 ''vote'':6',
        'COUNTRY', 'VOTING');

INSERT INTO public.node (key, gbif_region, continent, title, country, created_by, modified_by,
                         created, modified, deleted, fulltext_search, type, participation_status)
VALUES ('c9659a3e-07e9-4fcb-83c6-de8b9009a02e', 'AFRICA', 'AFRICA',
        'Wrong node name which must be updated by directory', 'TG',
        'CLI_TEST', 'CLI_TEST', '2020-03-20 00:24:07.409947',
        '2020-03-20 00:24:07.409947', null,
        '''africa'':5,6 ''countri'':3 ''gbif'':1 ''tg'':7 ''togo'':2 ''vote'':4', 'COUNTRY',
        'VOTING');

INSERT INTO public.identifier (key, type, identifier, created_by, created)
VALUES (-1, 'GBIF_PARTICIPANT', '244', 'directory-update-robot', '2020-03-20 00:24:07.416676');

INSERT INTO public.node_identifier (node_key, identifier_key) VALUES ('c9659a3e-07e9-4fcb-83c6-de8b9009a02e', -1);
