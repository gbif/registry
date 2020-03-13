-- create enum types
CREATE TYPE enum_collection_accession_status AS ENUM ('INSTITUTIONAL', 'PROJECT');
CREATE TYPE enum_institution_governance AS ENUM ('ACADEMIC_FEDERAL', 'ACADEMIC_FOR_PROFIT', 'ACADEMIC_LOCAL', 'ACADEMIC_NON_PROFIT', 'ACADEMIC_STATE', 'FEDERAL', 'FOR_PROFIT', 'LOCAL', 'NON_PROFIT', 'OTHER', 'STATE');
CREATE TYPE enum_institution_type AS ENUM ('BIOMEDICAL_RESEARCH_INSTITUTE', 'BOTANICAL_GARDEN', 'HERBARIUM', 'LIVING_ORGANISM_COLLECTION', 'MEDICAL_RESEARCH_INSTITUTE', 'MUSEUM', 'MUSEUM_HERBARIUM_PRIVATE_NON_PROFIT', 'OTHER_INSTITUTIONAL_TYPE', 'OTHER_TYPE_RESEARCH_INSTITUTION_BIOREPOSITORY', 'UNIVERSITY_COLLEGE', 'ZOO_AQUARIUM');


CREATE TABLE address (
	key serial NOT NULL PRIMARY KEY,
	address text CHECK (assert_min_length(address, 1)),
	city varchar CHECK (assert_min_length(city, 1)),
	province varchar CHECK (assert_min_length(province, 1)),
	postal_code varchar CHECK (assert_min_length(postal_code, 1)),
	country varchar CHECK (length(country) = 2),
	fulltext_search tsvector
);

CREATE INDEX address_fulltext_search_idx ON address USING gin(fulltext_search);

CREATE OR REPLACE FUNCTION address_change_trigger()
RETURNS TRIGGER AS
$addrchange$
    BEGIN
      NEW.fulltext_search :=
        TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.address,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.city,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.province,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.postal_code,''))) ||
        TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.country,'')));
      RETURN NEW;
    END;
$addrchange$
LANGUAGE plpgsql;

CREATE TRIGGER address_fulltext_update
  BEFORE INSERT OR UPDATE ON address
  FOR EACH ROW EXECUTE PROCEDURE address_change_trigger();

CREATE TABLE institution (
  key uuid NOT NULL PRIMARY KEY,
	code varchar NOT NULL CHECK (assert_min_length(code, 1)),
	name varchar NOT NULL CHECK (assert_min_length(name, 1)),
	description text CHECK (assert_min_length(description, 10)),
	type enum_institution_type,
	active bool,
	homepage varchar CHECK (assert_is_http(homepage)),
	catalog_url varchar CHECK (assert_is_http(catalog_url)),
	api_url varchar CHECK (assert_is_http(api_url)),
	institutional_governance enum_institution_governance,
	discipline varchar[],
	latitude NUMERIC(8,6) CHECK (latitude >= -90 AND latitude <= 90),
	longitude NUMERIC(9,6) CHECK (longitude >= -180 AND longitude <= 180),
	mailing_address_key int4 REFERENCES address(key),
	address_key int4 REFERENCES address(key),
	additional_names text[],
	founding_date date,
	geographic_description text CHECK (assert_min_length(geographic_description, 1)),
	taxonomic_description text CHECK (assert_min_length(taxonomic_description, 1)),
	number_specimens int4,
	index_herbariorum_record bool,
	logo_url varchar CHECK (assert_is_http(logo_url)),
	fulltext_search tsvector,
	cites_permit_number varchar CHECK (assert_min_length(cites_permit_number, 1)),
	created_by varchar NOT NULL CHECK (assert_min_length(created_by, 3)),
	modified_by varchar NOT NULL CHECK (assert_min_length(modified_by, 3)),
	created timestamptz NOT NULL DEFAULT now(),
	modified timestamptz NOT NULL DEFAULT now(),
	deleted timestamptz NULL
);

CREATE INDEX institution_fulltext_search_idx ON institution USING gin(fulltext_search);

CREATE OR REPLACE FUNCTION institution_change_trigger()
RETURNS TRIGGER AS
$instchange$
    BEGIN
      NEW.fulltext_search :=
        TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.code,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.name,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.description,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(CAST(NEW.type AS TEXT),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.homepage,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.catalog_url,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.api_url,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(CAST(NEW.institutional_governance AS TEXT),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(NEW.discipline, ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(NEW.additional_names, ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.geographic_description,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.taxonomic_description,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.logo_url,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.cites_permit_number,'')));
      RETURN NEW;
    END;
$instchange$
LANGUAGE plpgsql;

CREATE TRIGGER institution_fulltext_update
  BEFORE INSERT OR UPDATE ON institution
  FOR EACH ROW EXECUTE PROCEDURE institution_change_trigger();

CREATE TABLE collection (
  key uuid NOT NULL PRIMARY KEY,
	code varchar NOT NULL CHECK (assert_min_length(code, 1)),
	name varchar NOT NULL CHECK (assert_min_length(name, 1)),
	description text CHECK (assert_min_length(description, 5)),
	content_type varchar[],
	active bool,
	personal_collection bool DEFAULT false,
	doi varchar CHECK (assert_min_length(doi, 1)),
	homepage varchar CHECK (assert_is_http(homepage)),
	catalog_url varchar CHECK (assert_is_http(catalog_url)),
	api_url varchar CHECK (assert_is_http(api_url)),
	preservation_type varchar[],
	accession_status enum_collection_accession_status,
	institution_key uuid REFERENCES institution(key),
	mailing_address_key int4 REFERENCES address(key),
	address_key int4 REFERENCES address(key),
	fulltext_search tsvector,
	created_by varchar NOT NULL CHECK (assert_min_length(created_by, 3)),
	modified_by varchar NOT NULL CHECK (assert_min_length(modified_by, 3)),
	created timestamptz NOT NULL DEFAULT now(),
	modified timestamptz NOT NULL DEFAULT now(),
	deleted timestamptz NULL
);

CREATE INDEX collection_fulltext_search_idx ON collection USING gin(fulltext_search);

CREATE OR REPLACE FUNCTION collection_change_trigger()
RETURNS TRIGGER AS
$colchange$
    BEGIN
      NEW.fulltext_search :=
        TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.code,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.name,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.description,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(NEW.content_type, ' '),''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.doi,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.homepage,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.catalog_url,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.api_url,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(NEW.preservation_type, ' '),''))) ||
        TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(CAST(NEW.accession_status AS TEXT),'')));
      RETURN NEW;
    END;
$colchange$
LANGUAGE plpgsql;

CREATE TRIGGER collection_fulltext_update
  BEFORE INSERT OR UPDATE ON collection
  FOR EACH ROW EXECUTE PROCEDURE collection_change_trigger();

CREATE TABLE collection_person (
  key uuid NOT NULL PRIMARY KEY,
	first_name varchar NOT NULL CHECK (assert_min_length(first_name, 1)),
	last_name varchar CHECK (assert_min_length(last_name, 1)),
	position varchar CHECK (assert_min_length(position, 1)),
	area_responsibility varchar CHECK (assert_min_length(area_responsibility, 1)),
	research_pursuits text CHECK (assert_min_length(research_pursuits, 1)),
	phone varchar CHECK (assert_min_length(phone, 5)),
	fax varchar CHECK (assert_min_length(fax, 5)),
	email varchar CHECK (assert_min_length(email, 5)),
	mailing_address_key int4 REFERENCES address(key),
	primary_institution_key uuid REFERENCES institution(key),
	primary_collection_key uuid REFERENCES collection(key),
	fulltext_search tsvector,
	created_by varchar NOT NULL CHECK (assert_min_length(created_by, 3)),
	modified_by varchar NOT NULL CHECK (assert_min_length(modified_by, 3)),
	created timestamptz NOT NULL DEFAULT now(),
	modified timestamptz NOT NULL DEFAULT now(),
	deleted timestamptz NULL
);

CREATE INDEX colperson_fulltext_search_idx ON collection_person USING gin(fulltext_search);

CREATE OR REPLACE FUNCTION colperson_change_trigger()
RETURNS TRIGGER AS
$colpersonchange$
    BEGIN
      NEW.fulltext_search :=
        TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.first_name,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.last_name,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.position,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.area_responsibility,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.research_pursuits,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.phone,''))) ||
				TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.fax,''))) ||
        TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.email,'')));
      RETURN NEW;
    END;
$colpersonchange$
LANGUAGE plpgsql;

CREATE TRIGGER colperson_fulltext_update
  BEFORE INSERT OR UPDATE ON collection_person
  FOR EACH ROW EXECUTE PROCEDURE colperson_change_trigger();

CREATE TABLE collection_collection_person (
	collection_key uuid NOT NULL REFERENCES collection(key) ON DELETE CASCADE,
	collection_person_key uuid NOT NULL REFERENCES collection_person(key) ON DELETE CASCADE,
	PRIMARY KEY (collection_person_key, collection_key)
);

CREATE TABLE collection_identifier (
	collection_key uuid NOT NULL REFERENCES collection(key) ON DELETE CASCADE,
	identifier_key int4 NOT NULL REFERENCES identifier(key) ON DELETE CASCADE,
	PRIMARY KEY (identifier_key, collection_key)
);

CREATE TABLE collection_tag (
	collection_key uuid NOT NULL REFERENCES collection(key) ON DELETE CASCADE,
	tag_key int4 NOT NULL REFERENCES tag(key) ON DELETE CASCADE,
	PRIMARY KEY (collection_key, tag_key)
);

CREATE TABLE institution_collection_person (
	institution_key uuid NOT NULL REFERENCES institution(key) ON DELETE CASCADE,
	collection_person_key uuid NOT NULL REFERENCES collection_person(key) ON DELETE CASCADE,
	PRIMARY KEY (institution_key, collection_person_key)
);

CREATE TABLE institution_identifier (
	institution_key uuid NOT NULL REFERENCES institution(key) ON DELETE CASCADE,
	identifier_key int4 NOT NULL REFERENCES identifier(key) ON DELETE CASCADE,
	PRIMARY KEY (institution_key, identifier_key)
);

CREATE TABLE institution_tag (
	institution_key uuid NOT NULL REFERENCES institution(key) ON DELETE CASCADE,
	tag_key int4 NOT NULL REFERENCES tag(key) ON DELETE CASCADE,
	PRIMARY KEY (tag_key, institution_key)
);
