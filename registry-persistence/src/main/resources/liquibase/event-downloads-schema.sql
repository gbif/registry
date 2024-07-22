--
--  event download
--
CREATE TABLE event_download (
  key varchar(255) NOT NULL,
  filter text NULL,
  status enum_downlad_status NOT NULL,
  download_link text NOT NULL,
  notification_addresses text NULL,
  created_by varchar(255) NOT NULL,
  created timestamptz NOT NULL DEFAULT now(),
  modified timestamptz NOT NULL DEFAULT now(),
  size int8 NULL,
  total_records int8 NULL,
  send_notification bool NOT NULL DEFAULT true,
  doi text NULL,
  format enum_download_format NOT NULL,
  license enum_dataset_license NOT NULL DEFAULT 'CC_BY_4_0'::enum_dataset_license,
  erase_after timestamptz NULL,
  erasure_notification timestamptz NULL,
  source text NULL,
  type download_type NULL DEFAULT 'EVENT'::download_type,
  verbatim_extensions _extension NULL DEFAULT ARRAY[]::extension[],
  CONSTRAINT event_download_created_by_check CHECK (assert_min_length((created_by)::text, 3)),
  CONSTRAINT event_download_pkey PRIMARY KEY (key),
  CONSTRAINT event_download_source_check CHECK (assert_min_length(source, 1))
);
CREATE INDEX event_download_created_idx ON event_download USING btree (created);
CREATE INDEX event_download_created_key_idx ON event_download USING btree (created, key) WHERE (type = 'EVENT'::download_type);
CREATE INDEX event_download_doi_idx ON event_download USING btree (doi);


--
--  dataset event download
--
CREATE TABLE dataset_event_download (
  download_key varchar(255) NOT NULL,
  dataset_key uuid NOT NULL,
  number_records int4 NOT NULL,
  dataset_title text NULL,
  dataset_doi text NULL,
  dataset_citation text NULL,
  CONSTRAINT dataset_event_download_pkey PRIMARY KEY (download_key, dataset_key),
  CONSTRAINT dataset_event_download_dataset_key_fkey FOREIGN KEY (dataset_key) REFERENCES dataset(key) ON DELETE CASCADE,
  CONSTRAINT dataset_event_download_download_key_fkey FOREIGN KEY (download_key) REFERENCES event_download(key) ON DELETE CASCADE
);
CREATE INDEX dataset_event_download_created_idx ON dataset_event_download USING btree (dataset_key, right((download_key)::text, 15) DESC, left((download_key)::text, 7) DESC);
CREATE INDEX dataset_event_download_dataset_key_idx ON dataset_event_download USING btree (dataset_key);
CREATE INDEX dataset_event_download_download_key_idx ON dataset_event_download USING btree (download_key);
