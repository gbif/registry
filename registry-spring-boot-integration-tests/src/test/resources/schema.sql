DELETE
FROM public.editor_rights
WHERE 1 = 1;

DELETE
FROM public."user"
WHERE 1 = 1;

DELETE
FROM public.challenge_code
WHERE 1 = 1;

INSERT INTO public."user" (key, username, email, password, first_name, last_name, roles, settings, system_settings,
                           created, last_login, deleted, challenge_code_key)
VALUES (2, 'justuser', 'justuser@email.email', '$S$DSLeulP5GbaEzGpqDSJJVG8mFUisQP.Bmy/S15VVbG9aadZQ6KNp', 'John',
        'Doe', '{USER}', '', '', '2019-07-12 09:57:42.629508', null, null, null);

INSERT INTO public."user" (key, username, email, password, first_name, last_name, roles, settings, system_settings,
                           created, last_login, deleted, challenge_code_key)
VALUES (3, 'justadmin', 'justadmin@email.email', '$S$DSLeulP5GbaEzGpqDSJJVG8mFUisQP.Bmy/S15VVbG9aadZQ6KNp', 'Joe',
        'Doe', '{REGISTRY_ADMIN}', '', 'my.settings.key => 100_tacos=100$', '2019-07-12 10:02:03.778207', null, null, null);

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
VALUES (6, 'user_reset_password', 'user_reset_password@gbif.org', '$S$DhH0xHYrr2f/OSMJcRbD4Vg3tjceFQI798AEWrUofr8fCObUrmEC', 'Tim',
        'Robertson', '{USER}', 'country => dk, language => en', '', '2019-08-02 08:54:42.616587',
        '2019-08-02 08:54:42.667163', null, null);