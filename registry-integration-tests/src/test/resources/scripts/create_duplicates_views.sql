drop materialized view if exists institution_duplicates_mv;

create materialized view institution_duplicates_mv as
SELECT * FROM (
VALUES
('2ae51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,'AAA','Triple A University','DK','Copenhagen','DK','Copenhagen',
'f937a47f-5bb9-436b-9d1a-16cc93161cb6'::uuid,'BBB','Triple B University','DK','Copenhagen','DK','Copenhagen',
false,true,false,true,true,'2021-02-24 10:55:23.687'),
('2ae51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,'AAA','Triple A University','DK','Copenhagen','DK','Copenhagen',
'3937a47f-5bb9-436b-9d1a-16cc93161cb3'::uuid,'AAA','Another A institution','DE','Berlin',NULL,NULL,
true,false,false,false,false,'2021-02-24 10:55:23.687'),
('2ae51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,'AAA','Triple A University','DK','Copenhagen','DK','Copenhagen',
'4937a47f-5bb9-436b-9d1a-16cc93161cb3'::uuid,'AAA','Third',NULL,NULL,NULL,NULL,
true,false,false,false,false,'2021-02-24 10:55:23.687'),
('3937a47f-5bb9-436b-9d1a-16cc93161cb3'::uuid,'AAA','Another A institution','DE','Berlin',NULL,NULL,
'4937a47f-5bb9-436b-9d1a-16cc93161cb3'::uuid,'AAA','Third',NULL,NULL,NULL,NULL,
true,false,false,false,false,'2021-02-24 10:55:23.687'),
('7937a47f-5bb9-436b-9d1a-16cc93161cb7'::uuid,'CCC','The other institution','DK',NULL,NULL,NULL,
'aae51c20-a824-49ca-9c8e-1ebef68660ca'::uuid,'DDD','The other institution','DK','Copenhagen','DK','Copenhagen',
false,true,true,false,true,'2021-02-24 10:55:23.687'),
('7937a47f-5bb9-436b-9d1a-16cc93161cb7'::uuid,'CCC','The other institution','DK',NULL,NULL,NULL,
'1ae51c20-a824-49ca-9c8e-1ebef68660ca'::uuid,'FFF','The other institutionn',NULL,NULL,NULL,NULL,
false,true,false,false,false,'2021-02-24 10:55:23.687')
) AS t(key1, code1, name1, physical_country1, physical_city1, mailing_country1, mailing_city1,
key2, code2, name2, physical_country2, physical_city2, mailing_country2, mailing_city2,
code_match, fuzzy_name_match, name_match, city_match,country_match, generated_date);

drop materialized view if exists collection_duplicates_mv;

create materialized view collection_duplicates_mv as
SELECT * FROM (
VALUES
('2ae51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,'AAA','Triple A University','bae51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,'DK','Copenhagen','DK','Copenhagen',
'f937a47f-5bb9-436b-9d1a-16cc93161cb6'::uuid,'BBB','Triple B University','eee51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,'DK','Copenhagen','DK','Copenhagen',
false,true,false,true,true,false,'2021-02-24 10:55:23.687'),
('2ae51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,'AAA','Triple A University','bae51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,'DK','Copenhagen','DK','Copenhagen',
'3937a47f-5bb9-436b-9d1a-16cc93161cb3'::uuid,'AAA','Another A institution','bae51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,'DE','Berlin',NULL,NULL,
true,false,false,false,false,true,'2021-02-24 10:55:23.687'),
('2ae51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,'AAA','Triple A University','bae51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,'DK','Copenhagen','DK','Copenhagen',
'4937a47f-5bb9-436b-9d1a-16cc93161cb3'::uuid,'AAA','Third','eee51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,NULL,NULL,NULL,NULL,
true,false,false,false,false,false,'2021-02-24 10:55:23.687'),
('3937a47f-5bb9-436b-9d1a-16cc93161cb3'::uuid,'AAA','Another A institution','bae51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,'DE','Berlin',NULL,NULL,
'4937a47f-5bb9-436b-9d1a-16cc93161cb3'::uuid,'AAA','Third','eee51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,NULL,NULL,NULL,NULL,
true,false,false,false,false,false,'2021-02-24 10:55:23.687'),
('7937a47f-5bb9-436b-9d1a-16cc93161cb7'::uuid,'CCC','The other institution','dde51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,'DK',NULL,NULL,NULL,
'aae51c20-a824-49ca-9c8e-1ebef68660ca'::uuid,'DDD','The other institution','8ee51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,'DK','Copenhagen','DK','Copenhagen',
false,true,true,false,true,false,'2021-02-24 10:55:23.687'),
('7937a47f-5bb9-436b-9d1a-16cc93161cb7'::uuid,'CCC','The other institution','dde51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,'DK',NULL,NULL,NULL,
'1ae51c20-a824-49ca-9c8e-1ebef68660ca'::uuid,'FFF','The other institutionn','9ee51c20-a824-49ca-9c8e-1ebef68660c0'::uuid,NULL,NULL,NULL,NULL,
false,true,false,false,false,false,'2021-02-24 10:55:23.687')
) AS t(key1, code1, name1, institution_key1, physical_country1, physical_city1, mailing_country1, mailing_city1,
key2, code2, name2, institution_key2, physical_country2, physical_city2, mailing_country2, mailing_city2,
code_match, fuzzy_name_match, name_match, city_match, country_match, institution_key_match, generated_date);

