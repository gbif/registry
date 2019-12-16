INSERT INTO public.occurrence_download (key, filter, status, download_link, notification_addresses, created_by, created,
                                        modified, size, total_records, send_notification, doi, format, license,
                                        erase_after)
VALUES ('ba40b279-7fef-43ab-a0c7-95d4ae2ffaf5', '{"type":"equals","key":"TAXON_KEY","value":"212"}', 'PREPARING',
        'testUrl', 'downloadtest@gbif.org', 'registry_user', '2019-12-13 13:13:39.067111', '2019-12-13 13:13:39.067111', 0, 0,
        true, '10.21373/dl.xsl50x', 'DWCA', 'UNSPECIFIED', '2020-06-13 13:13:38.855000');
INSERT INTO public.occurrence_download (key, filter, status, download_link, notification_addresses, created_by, created,
                                        modified, size, total_records, send_notification, doi, format, license,
                                        erase_after)
VALUES ('2393d6f0-cd3f-4395-94ea-0d3389fabeee', '{"type":"equals","key":"TAXON_KEY","value":"212"}', 'PREPARING',
        'testUrl', 'downloadtest@gbif.org', 'WS TEST', '2019-12-13 13:13:39.097718', '2019-12-13 13:13:39.097718', 0, 0,
        true, '10.21373/dl.isqkin', 'DWCA', 'UNSPECIFIED', '2020-06-13 13:13:39.083000');
INSERT INTO public.occurrence_download (key, filter, status, download_link, notification_addresses, created_by, created,
                                        modified, size, total_records, send_notification, doi, format, license,
                                        erase_after)
VALUES ('8b84c7b5-d1e8-4f86-8934-8b6c3298046d', '{"type":"equals","key":"TAXON_KEY","value":"212"}', 'PREPARING',
        'testUrl', 'downloadtest@gbif.org', 'WS TEST', '2019-12-13 13:13:39.107317', '2019-12-13 13:13:39.107317', 0, 0,
        true, '10.21373/dl.ntcbf6', 'DWCA', 'UNSPECIFIED', '2020-06-13 13:13:39.098000');
INSERT INTO public.occurrence_download (key, filter, status, download_link, notification_addresses, created_by, created,
                                        modified, size, total_records, send_notification, doi, format, license,
                                        erase_after)
VALUES ('c277534e-3576-4203-b26c-2ffbe18f61be', 'SELECT datasetKey FROM occurrence', 'PREPARING', 'testUrl',
        'downloadtest@gbif.org', 'WS TEST', '2019-12-13 13:13:39.117109', '2019-12-13 13:13:39.117109', 0, 0, true,
        '10.21373/dl.n6jcmp', 'SQL', 'UNSPECIFIED', '2020-06-13 13:13:39.108000');
INSERT INTO public.occurrence_download (key, filter, status, download_link, notification_addresses, created_by, created,
                                        modified, size, total_records, send_notification, doi, format, license,
                                        erase_after)
VALUES ('20c7c079-e91a-4cd9-9428-383dd008a47e', 'SELECT datasetKey FROM occurrence', 'PREPARING', 'testUrl',
        'downloadtest@gbif.org', 'registry_user', '2019-12-13 13:13:39.125894', '2019-12-13 13:13:39.125894', 0, 0, true,
        '10.21373/dl.kw7dtp', 'SQL', 'UNSPECIFIED', '2020-06-13 13:13:39.117000');
INSERT INTO public.occurrence_download (key, filter, status, download_link, notification_addresses, created_by, created,
                                        modified, size, total_records, send_notification, doi, format, license,
                                        erase_after)
VALUES ('af6093d6-4c57-4b1f-8e3d-ebd84692f8f7', 'SELECT datasetKey FROM occurrence', 'CANCELLED', 'testUrl',
        'downloadtest@gbif.org', 'registry_user', '2019-12-13 13:13:39.133348', '2019-12-13 13:13:39.133348', 0, 0, true,
        '10.21373/dl.wxa5kp', 'SQL', 'UNSPECIFIED', '2020-06-13 13:13:39.126000');
