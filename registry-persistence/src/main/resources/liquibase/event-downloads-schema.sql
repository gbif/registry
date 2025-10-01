--
--  event download
--
CREATE TABLE event_download (
  key varchar(255) NOT NULL PRIMARY KEY,
  filter text,
  status enum_downlad_status NOT NULL,
  download_link text NOT NULL,
  notification_addresses text,
  created_by varchar(255) NOT NULL CHECK (assert_min_length(created_by, 3)),
  created timestamp with time zone NOT NULL DEFAULT now(),
  modified timestamp with time zone NOT NULL DEFAULT now(),
  size bigint,
  total_records bigint,
  send_notification BOOLEAN NOT NULL DEFAULT true,
  doi text NULL,
  format enum_download_format NOT NULL,
  license enum_dataset_license NOT NULL DEFAULT 'CC_BY_4_0'::enum_dataset_license,
  erase_after timestamp with time zone,
  erasure_notification timestamp with time zone,
  source text CHECK (assert_min_length(source, 1)),
  verbatim_extensions extension ARRAY DEFAULT array[]::extension[],
  description text,
  machine_description jsonb,
  checklist_key TEXT
);

CREATE INDEX ON event_download(key, created);
CREATE INDEX ON event_download (created);
CREATE INDEX event_download_doi_idx ON event_download (doi);

--
--  dataset event download
--
CREATE TABLE dataset_event_download (
  download_key varchar(255) NOT NULL REFERENCES event_download(key) ON DELETE CASCADE,
  dataset_key uuid NOT NULL REFERENCES dataset(key) ON DELETE CASCADE,
  number_records integer NOT NULL,
  dataset_title text,
  dataset_doi text,
  dataset_citation text,
  PRIMARY KEY (download_key,dataset_key)
);

CREATE INDEX ON dataset_event_download (dataset_key);
CREATE INDEX ON dataset_event_download (download_key);
CREATE INDEX dataset_event_download_created_idx ON dataset_event_download (dataset_key, right(download_key, 15) DESC, left(download_key, 7) DESC);
