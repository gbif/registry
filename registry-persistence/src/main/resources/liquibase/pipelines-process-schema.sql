CREATE TABLE pipeline_process (
                                  key bigserial NOT NULL PRIMARY KEY,
                                  dataset_key uuid NOT NULL REFERENCES dataset (key) ON DELETE CASCADE,
                                  attempt integer NOT NULL,
                                  created timestamp with time zone NOT NULL DEFAULT now(),
                                  created_by text NOT NULL,
                                  UNIQUE(dataset_key, attempt)
);

CREATE TYPE pipeline_step_status AS ENUM ('RUNNING', 'FAILED', 'COMPLETED');

CREATE TYPE pipeline_step_type AS ENUM ('DWCA_TO_VERBATIM', 'XML_TO_VERBATIM', 'ABCD_TO_VERBATIM', 'VERBATIM_TO_INTERPRETED', 'INTERPRETED_TO_INDEX', 'HDFS_VIEW');

CREATE TYPE pipeline_step_runner AS ENUM ('STANDALONE', 'DISTRIBUTED', 'UNKNOWN');

CREATE TABLE pipeline_step (
                               key bigserial NOT NULL PRIMARY KEY,
                               type pipeline_step_type NOT NULL,
                               runner pipeline_step_runner,
                               started timestamp with time zone NOT NULL DEFAULT now(),
                               finished timestamp with time zone,
                               state pipeline_step_status NOT NULL,
                               message text,
                               metrics hstore,
                               rerun_reason text,
                               created_by text NOT NULL,
                               modified timestamp with time zone,
                               modified_by text,
                               pipeline_process_key integer NOT NULL REFERENCES pipeline_process (key) ON DELETE CASCADE
);
