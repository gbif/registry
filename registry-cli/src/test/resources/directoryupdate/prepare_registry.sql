DELETE FROM public.identifier;
DELETE FROM public.node_identifier;
DELETE FROM public.node;

INSERT INTO public.node (key, gbif_region, continent, title, country, created_by, modified_by,
                         created, modified, deleted, fulltext_search, type, participation_status)
VALUES ('a49e75d9-7b07-4d01-9be8-6ab2133f42f9', 'EUROPE', 'EUROPE', 'The UK National Node', 'GB',
        'WS TEST', 'WS TEST', '2020-02-22 09:54:09.835039', '2020-02-22 09:54:09.835039', null,
        '''countri'':5 ''europ'':7,8 ''gb'':9 ''nation'':3 ''node'':4 ''uk'':2 ''vote'':6',
        'COUNTRY', 'VOTING');

INSERT INTO public.node (key, gbif_region, continent, title, country, created_by, modified_by,
                         created, modified, deleted, fulltext_search, type, participation_status)
VALUES ('0e655f55-8a9d-498f-9903-33058b78d006', 'AFRICA', 'AFRICA',
        'Wrong node name which must be updated by directory', 'TG',
        'CLI_TEST', 'CLI_TEST', '2020-03-20 00:24:07.409947',
        '2020-03-20 00:24:07.409947', null,
        '''africa'':5,6 ''countri'':3 ''gbif'':1 ''tg'':7 ''togo'':2 ''vote'':4', 'COUNTRY',
        'VOTING');

INSERT INTO public.identifier (key, type, identifier, created_by, created)
VALUES (-1, 'GBIF_PARTICIPANT', '244', 'directory-update-robot', '2020-03-20 00:24:07.416676');

INSERT INTO public.node_identifier (node_key, identifier_key) VALUES ('0e655f55-8a9d-498f-9903-33058b78d006', -1);
