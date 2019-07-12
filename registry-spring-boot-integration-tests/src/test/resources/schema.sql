DELETE FROM public.editor_rights WHERE 1=1;

DELETE FROM public."user" WHERE 1=1;

DELETE FROM public.challenge_code WHERE 1=1;

INSERT INTO public."user" (key, username, email, password, first_name, last_name, roles, settings, system_settings,
                           created, last_login, deleted, challenge_code_key)
VALUES (2, 'justuser', 'justuser@email.email', '$S$DSLeulP5GbaEzGpqDSJJVG8mFUisQP.Bmy/S15VVbG9aadZQ6KNp', 'John',
        'Doe', '{USER}', '', '', '2019-07-12 09:57:42.629508', null, null, null);

INSERT INTO public."user" (key, username, email, password, first_name, last_name, roles, settings, system_settings,
                           created, last_login, deleted, challenge_code_key)
VALUES (3, 'justadmin', 'justadmin@email.email', '$S$DSLeulP5GbaEzGpqDSJJVG8mFUisQP.Bmy/S15VVbG9aadZQ6KNp', 'Joe',
        'Doe', '{REGISTRY_ADMIN}', '', '', '2019-07-12 10:02:03.778207', null, null, null);