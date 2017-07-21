

## Notes about local development

### Setting up a local copy of the production registry

Dump the DB
```
$ pg_dump -F c -U registry -h XXX.gbif.org -O -T dataset_occurrence_download -T occurrence_download -T metadata prod_b_registry > /tmp/reg_live.bin'
```

Set up a new local DB
```
$ /Applications/Postgres.app/Contents/Versions/9.3/bin/psql -p5432 -c "drop database registry"
$ /Applications/Postgres.app/Contents/Versions/9.3/bin/psql -p5432
  create database registry;
  \connect registry
  create role registry;                                                                                                                                                                                                                                 
  GRANT ALL ON DATABASE registry TO registry;   

  CREATE EXTENSION unaccent;                                                                                                                                                                                                                            
  CREATE EXTENSION hstore; 

  CREATE FUNCTION assert_min_length(input text, minlength integer) RETURNS boolean AS $$ DECLARE length integer; BEGIN length := char_length(trim(input)); IF (length IS NULL) OR (length >= minlength) THEN RETURN TRUE; ELSE RETURN FALSE; END IF; END; $$ LANGUAGE plpgsql; 
  CREATE FUNCTION assert_is_http(input text) RETURNS boolean AS $$ DECLARE length integer; BEGIN length := char_length(trim(input)); IF (length IS NULL) OR (position('http://' in trim(input)) = 0) OR (position('https://' in trim(input)) = 0) THEN RETURN TRUE; ELSE RETURN FALSE; END IF; END; $$ LANGUAGE plpgsql;

```

Load in the dump
```
$ pg_restore -U registry -d registry /tmp/reg_live.bin  
```

Add the necessary metadata table
```
$ /Applications/Postgres.app/Contents/Versions/9.3/bin/psql -p5432
  CREATE TABLE metadata
  (
    key serial NOT NULL,
    dataset_key uuid NOT NULL,
    type enum_metadata_type NOT NULL,
    content bytea NOT NULL,
    created_by character varying(255) NOT NULL,
    modified_by character varying(255) NOT NULL,
    created timestamp with time zone NOT NULL DEFAULT now(),
    modified timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT metadata_pkey PRIMARY KEY (key),
    CONSTRAINT metadata_dataset_key_fkey FOREIGN KEY (dataset_key)
      REFERENCES dataset (key) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT metadata_created_by_check CHECK (assert_min_length(created_by::text, 3)),
    CONSTRAINT metadata_modified_by_check CHECK (assert_min_length(modified_by::text, 3))
  )
  WITH (
    OIDS=FALSE
  );
  ALTER TABLE metadata OWNER TO registry;
  GRANT ALL ON TABLE metadata TO registry;
  CREATE INDEX metadata_dataset_key_idx
    ON metadata
    USING btree
    (dataset_key);  
  \q
```

Add a user:
```
delete from public.user where key=1;
insert into public.user(key, username, email, password, first_name, last_name, settings, roles, created)
values (1, 'trobertson', 'trobertson@gbif.org', 'TODO', 'Tim', 'Robertson', 
  'country=>DK'::hstore, '{"USER", "REGISTRY_ADMIN", "ADMIN", "EDITOR"}', now());
```



