INSERT INTO public."user" (key, username, email, password, first_name, last_name, roles, settings, system_settings,
                           created, last_login, deleted, challenge_code_key)
VALUES (4, 'user_13', 'user_13@gbif.org', '$S$DsO2Zyy5gdu98q5XBHbLMKVkMdayt/Y5lJLvjafCL42yUSTqF1Gh', 'Tim',
        'Robertson', '{USER}', 'country => dk, language => en', '', '2019-08-02 08:54:42.327579',
        '2019-08-02 08:54:42.582421', null, null);
INSERT INTO public."user" (key, username, email, password, first_name, last_name, roles, settings, system_settings,
                           created, last_login, deleted, challenge_code_key)
VALUES (5, 'user_12', 'user_12@gbif.org', '$S$DhH0xHYrr2f/OSMJcRbD4Vg3tjceFQI798AEWrUofr8fCObUrmEC', 'Tim',
        'Robertson', '{USER}', 'country => dk, language => en', '', '2019-08-02 08:54:42.616587',
        '2019-08-02 08:54:42.667163', null, null);
INSERT INTO public."user" (key, username, email, password, first_name, last_name, roles, settings, system_settings,
                           created, last_login, deleted, challenge_code_key)
VALUES (6, 'user_reset_password', 'user_reset_password@gbif.org',
        '$S$DhH0xHYrr2f/OSMJcRbD4Vg3tjceFQI798AEWrUofr8fCObUrmEC', 'Tim',
        'Robertson', '{USER}', 'country => dk, language => en', '', '2019-08-02 08:54:42.616587',
        '2019-08-02 08:54:42.667163', null, null);
INSERT INTO public.challenge_code (key, challenge_code, created)
VALUES (1, 'd4f26f30-c006-11e9-9cb5-2a2ae2dbcce4', '2019-08-02 08:54:42.616587');
INSERT INTO public."user" (key, username, email, password, first_name, last_name, roles, settings, system_settings,
                           created, last_login, deleted, challenge_code_key)
VALUES (7, 'user_update_password', 'user_update_password@gbif.org',
        '$S$DhH0xHYrr2f/OSMJcRbD4Vg3tjceFQI798AEWrUofr8fCObUrmEC', 'Tim',
        'Robertson', '{USER}', 'country => dk, language => en', '', '2019-08-02 08:54:42.616587',
        '2019-08-02 08:54:42.667163', null, 1);
