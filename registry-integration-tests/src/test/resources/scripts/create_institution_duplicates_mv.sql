drop materialized view if exists institution_duplicate_mv;

create materialized view institution_duplicate_mv as
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
) AS t(key1, code1, name1, physicalCountry1, physicalCity1, mailingCountry1, mailingCity1,
key2, code2, name2, physicalCountry2, physicalCity2, mailingCountry2, mailingCity2,
code_match, fuzzy_name_match, name_match, city_match,country_match, generated_date);
