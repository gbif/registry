INSERT INTO public.address (key, address, city, province, postal_code, country)
VALUES (1, 'Universitetsparken 15', 'Copenhagen', 'Capital', '2100', 'DK');
INSERT INTO public.address (key, address, city, province, postal_code, country)
VALUES (2, 'Roskildevej 32', 'Frederiksberg', 'Capital', '2000', 'DK');

INSERT INTO public.institution (key, code, name, description, type, active, homepage, catalog_url, api_url,
                                institutional_governance, discipline, latitude, longitude, mailing_address_key,
                                address_key, additional_names, founding_date, geographic_description,
                                taxonomic_description, number_specimens, index_herbariorum_record, logo_url,
                                fulltext_search, cites_permit_number, created_by, modified_by, created, modified,
                                deleted)
VALUES ('b40143bf-c810-4e67-b998-2936cef72bb3', 'II', 'First institution', 'dummy description', null, false,
        'http://dummy', null, null, null, null, null, null, null, 1, null, null, null, null, 0, false, null,
        '''descript'':5 ''dummi'':4,6 ''ii'':1 ''institut'':2 ''name'':3', null, 'WS TEST', 'WS TEST',
        '2020-01-02 15:12:03.897224', '2020-01-02 15:12:03.897224', null);
INSERT INTO public.institution (key, code, name, description, type, active, homepage, catalog_url, api_url,
                                institutional_governance, discipline, latitude, longitude, mailing_address_key,
                                address_key, additional_names, founding_date, geographic_description,
                                taxonomic_description, number_specimens, index_herbariorum_record, logo_url,
                                fulltext_search, cites_permit_number, created_by, modified_by, created, modified,
                                deleted)
VALUES ('4c7db8e8-23ac-4392-824f-82e17dd19cc8', 'II2', 'Second institution', 'dummy description', null, false,
        'http://dummy', null, null, null, null, null, null, null, 2, null, null, null, null, 0, false, null,
        '''descript'':5 ''dummi'':4,6 ''ii2'':1 ''institut'':2 ''name2'':3', null, 'WS TEST', 'WS TEST',
        '2020-01-02 15:12:03.950265', '2020-01-02 15:12:03.950265', null);
INSERT INTO public.institution (key, code, name, description, type, active, homepage, catalog_url, api_url,
                                institutional_governance, discipline, latitude, longitude, mailing_address_key,
                                address_key, additional_names, founding_date, geographic_description,
                                taxonomic_description, number_specimens, index_herbariorum_record, logo_url,
                                fulltext_search, cites_permit_number, created_by, modified_by, created, modified,
                                deleted)
VALUES ('0082dba6-9669-414e-925e-183d7a136554', 'code', 'deleted', 'dummy description', null, false, 'http://dummy',
        null,
        null, null, null, null, null, null, null, null, null, null, null, 0, false, null,
        '''code'':1 ''descript'':4 ''dummi'':3,5 ''name'':2', null, 'WS TEST', 'WS TEST', '2020-01-03 09:00:55.630539',
        '2020-01-03 09:00:55.630539', '2020-01-03 09:00:55.692831');


INSERT INTO public.collection_person (key, first_name, last_name, position, area_responsibility, research_pursuits,
                                      phone, fax, email, mailing_address_key, primary_institution_key,
                                      primary_collection_key, fulltext_search, created_by, modified_by, created,
                                      modified, deleted)
VALUES ('9ed62e2a-9288-4516-ad3d-2ffc5e019cb7', 'John', 'Doe', null, null, null, null, null, null, null, null,
        null, '''doe'':2 ''john'':1', 'WS TEST', 'WS TEST', '2020-01-03 09:52:48.569000',
        '2020-01-03 09:52:48.569000', null);
INSERT INTO public.collection_person (key, first_name, last_name, position, area_responsibility, research_pursuits,
                                      phone, fax, email, mailing_address_key, primary_institution_key,
                                      primary_collection_key, fulltext_search, created_by, modified_by, created,
                                      modified, deleted)
VALUES ('e5f16af3-6fb7-4cb0-826b-733d1a4a5e36', 'Joseph', 'Doe', null, null, null, null, null, null, null, null,
        null, '''doe'':2 ''joseph'':1', 'WS TEST', 'WS TEST', '2020-01-03 09:52:48.593020',
        '2020-01-03 09:52:48.593020', null);


INSERT INTO public.institution_collection_person (institution_key, collection_person_key)
VALUES ('b40143bf-c810-4e67-b998-2936cef72bb3', '9ed62e2a-9288-4516-ad3d-2ffc5e019cb7');
INSERT INTO public.institution_collection_person (institution_key, collection_person_key)
VALUES ('4c7db8e8-23ac-4392-824f-82e17dd19cc8', 'e5f16af3-6fb7-4cb0-826b-733d1a4a5e36');
