/*
* Inserts below are only needed when setting up a new registry-sandbox, depended on by:
*  - ipt.gbif.org
*  - ipt-uat.gbif.org
*  - registry-examples integration tests
*/

-- Add organization named "Test Organization #1"
INSERT INTO organization(key,endorsing_node_key,endorsement_approved,password,title,description,language,email,phone,homepage,logo_url,address,city,country,postal_code,latitude,longitude,created,modified,created_by,modified_by)
VALUES('0a16da09-7719-40de-8d4f-56a15ed52fb6'::uuid,'4ddd294f-02b7-4359-ac33-0806a9ca9c6b'::uuid,true::boolean,'password','Test Organisation #1','Test Organisation for IPT','EN','{kbraak@gbif.org}','{12345678}','{http://homepage.com}','http://logourl.com','{Universitetsparken 15}','Copenhagen','DK','2100','55.6761','55.6761',now(),now(),'Script', 'Script');

INSERT INTO contact(key,first_name,last_name,description,position,email,phone,organization,address,city,province,country,postal_code,created_by,modified_by,created,modified)
VALUES(21000,'Kyle','Braak','Programmer at GBIF','{Programmer}','{kbraak@gbif.org}','{12345678}','GBIF','{Universitetsparken 15}','Copenhagen','Zealand','DK','2100','Script','Script',now(),now());

INSERT INTO organization_contact(organization_key,contact_key,type,is_primary)
VALUES('0a16da09-7719-40de-8d4f-56a15ed52fb6'::uuid,21000,'ADMINISTRATIVE_POINT_OF_CONTACT'::enum_contact_type,true);

-- Give ws_client_demo permission to edit Test Organization #1
INSERT INTO editor_rights (username, key) values ('ws_client_demo', '0a16da09-7719-40de-8d4f-56a15ed52fb6');

-- Add installation to host ipt.gbif.org
INSERT INTO installation(key,organization_key,password,title,description,type,created,modified,created_by,modified_by)
VALUES('63e64bc2-d3de-43ee-bb52-cd65c759dd99'::uuid,'0a16da09-7719-40de-8d4f-56a15ed52fb6'::uuid,'password','Test IPT','Test IPT for IPT.GBIF.ORG','IPT_INSTALLATION',now(),now(),'Script', 'Script');

INSERT INTO contact(key,first_name,last_name,description,position,email,phone,organization,address,city,province,country,postal_code,created_by,modified_by,created,modified)
VALUES(21001,'Kyle','Braak','Programmer at GBIF','{Programmer}','{kbraak@gbif.org}','{12345678}','GBIF','{Universitetsparken 15}','Copenhagen','Zealand','DK','2100','Script','Script',now(),now());

INSERT INTO installation_contact(installation_key,contact_key,type,is_primary)
VALUES('63e64bc2-d3de-43ee-bb52-cd65c759dd99'::uuid,21001,'ADMINISTRATIVE_POINT_OF_CONTACT'::enum_contact_type,true);

-- Add installation to host ipt-uat.gbif.org
INSERT INTO installation(key,organization_key,password,title,description,type,created,modified,created_by,modified_by)
VALUES('8949a635-0eb0-4bf1-aaf9-9af97a0ea949'::uuid,'0a16da09-7719-40de-8d4f-56a15ed52fb6'::uuid,'password','Test IPT','Test IPT for IPT-UAT.GBIF.ORG','IPT_INSTALLATION',now(),now(),'Script', 'Script');

INSERT INTO contact(key,first_name,last_name,description,position,email,phone,organization,address,city,province,country,postal_code,created_by,modified_by,created,modified)
VALUES(21002,'Kyle','Braak','Programmer at GBIF','{Programmer}','{kbraak@gbif.org}','{12345678}','GBIF','{Universitetsparken 15}','Copenhagen','Zealand','DK','2100','Script','Script',now(),now());

INSERT INTO installation_contact(installation_key,contact_key,type,is_primary)
VALUES('8949a635-0eb0-4bf1-aaf9-9af97a0ea949'::uuid,21002,'ADMINISTRATIVE_POINT_OF_CONTACT'::enum_contact_type,true);

-- Add organization used exclusively for Jenkins
INSERT INTO organization(key,endorsing_node_key,endorsement_approved,password,title,description,language,email,phone,homepage,logo_url,address,city,country,postal_code,latitude,longitude,created,modified,created_by,modified_by)
VALUES('62922b92-69d1-4c4b-831c-b23d5412a124'::uuid,'4ddd294f-02b7-4359-ac33-0806a9ca9c6b'::uuid,true::boolean,'password','Test Organisation Jenkins','Test Organisation for IPT built by Jenkins','EN','{kbraak@gbif.org}','{12345678}','{http://homepage.com}','http://logourl.com','{Universitetsparken 15}','Copenhagen','DK','2100','55.6761','55.6761',now(),now(),'Script', 'Script');

INSERT INTO contact(key,first_name,last_name,description,position,email,phone,organization,address,city,province,country,postal_code,created_by,modified_by,created,modified)
VALUES(21001,'Kyle','Braak','Programmer at GBIF','{Programmer}','{kbraak@gbif.org}','{12345678}','GBIF','{Universitetsparken 15}','Copenhagen','Zealand','DK','2100','Script','Script',now(),now());

INSERT INTO organization_contact(organization_key,contact_key,type,is_primary)
VALUES('62922b92-69d1-4c4b-831c-b23d5412a124'::uuid,21001,'ADMINISTRATIVE_POINT_OF_CONTACT'::enum_contact_type,true);