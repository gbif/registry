--
-- Returns true if the input text is equal or exceeds the provided length following validation which includes: 
-- i. Trimming of whitespace 
-- *Note*: PostgreSQL's JDBC driver needs to parse the function in a single line
--
CREATE FUNCTION assert_min_length(input text, minlength integer) RETURNS boolean AS $$ DECLARE length integer; BEGIN length := char_length(trim(input)); IF (length IS NULL) OR (length >= minlength) THEN RETURN TRUE; ELSE RETURN FALSE; END IF; END; $$ LANGUAGE plpgsql; 
--
-- Returns true if the URL comforms with the HTTP protocol.  
--
CREATE FUNCTION assert_is_http(input text) RETURNS boolean AS $$ DECLARE length integer; BEGIN length := char_length(trim(input)); IF (length IS NULL) OR (position('http://' in trim(input)) = 0) OR (position('https://' in trim(input)) = 0) THEN RETURN TRUE; ELSE RETURN FALSE; END IF; END; $$ LANGUAGE plpgsql;

-- 
--  node
-- 

CREATE TYPE enum_node_type AS ENUM ('COUNTRY', 'ORGANIZATION');
CREATE TYPE enum_node_participation_status AS ENUM ('VOTING', 'ASSOCIATE');
CREATE TYPE enum_node_gbif_region AS ENUM ('AFRICA', 'ASIA', 'EUROPE', 'NORTH_AMERICA', 'OCEANIA', 'LATIN_AMERICA');
CREATE TYPE enum_node_continent AS ENUM ('AFRICA', 'ASIA', 'EUROPE', 'NORTH_AMERICA', 'OCEANIA', 'SOUTH_AMERICA', 'ANTARCTICA');
CREATE TABLE node
(
  key uuid NOT NULL PRIMARY KEY,
  type enum_node_type NOT NULL,
  participation_status enum_node_participation_status NOT NULL,
  gbif_region enum_node_gbif_region,
  continent enum_node_continent NOT NULL,
  title varchar(255) NOT NULL CHECK (assert_min_length(title, 2)),
  description text CHECK (assert_min_length(description, 10)),
  language char(2) NOT NULL CHECK (assert_min_length(language, 2)),
  email varchar(254) CHECK (assert_min_length(email, 5)),
  phone text CHECK (assert_min_length(phone, 5)),
  homepage text CHECK (assert_is_http(homepage)),
  logo_url text CHECK (assert_is_http(logo_url)),
  address text CHECK (assert_min_length(address, 1)),
  city text CHECK (assert_min_length(city, 1)),
  province text CHECK (assert_min_length(province, 1)),
  country char(2) CHECK (assert_min_length(country, 2)),
  postal_code text CHECK (assert_min_length(postal_code, 1)),
  created_by varchar(255) NOT NULL CHECK (assert_min_length(created_by, 3)),
  modified_by varchar(255) NOT NULL CHECK (assert_min_length(modified_by, 3)),
  created timestamp with time zone NOT NULL DEFAULT now(),
  modified timestamp with time zone NOT NULL DEFAULT now(),
  deleted timestamp with time zone
);

-- 
--  organization
-- 
CREATE TABLE organization
(
  key uuid NOT NULL PRIMARY KEY,
  endorsing_node_key uuid NOT NULL REFERENCES node(key),
  endorsement_approved boolean NOT NULL DEFAULT false,
  password varchar(255) CHECK (assert_min_length(password, 5)),
  title varchar(255) NOT NULL CHECK (assert_min_length(title, 2)),
  abbreviation varchar(10) CHECK (assert_min_length(abbreviation, 1)),
  description text CHECK (assert_min_length(description, 10)),
  language char(2) NOT NULL CHECK (assert_min_length(language, 2)),
  email varchar(254) CHECK (assert_min_length(email, 5)),
  phone text CHECK (assert_min_length(phone, 5)),
  homepage text CHECK (assert_is_http(homepage)),
  logo_url text CHECK (assert_is_http(logo_url)),
  address text CHECK (assert_min_length(address, 1)),
  city text CHECK (assert_min_length(city, 1)),
  province text CHECK (assert_min_length(province, 1)),
  country char(2) CHECK (assert_min_length(country, 2)),
  postal_code text CHECK (assert_min_length(postal_code, 1)),
  latitude NUMERIC(7,5) CHECK (latitude >= -90 AND latitude <= 90),
  longitude NUMERIC(8,5) CHECK (longitude >= -180 AND longitude <= 180),
  created_by varchar(255) NOT NULL CHECK (assert_min_length(created_by, 3)),
  modified_by varchar(255) NOT NULL CHECK (assert_min_length(modified_by, 3)),
  created timestamp with time zone NOT NULL DEFAULT now(),
  modified timestamp with time zone NOT NULL DEFAULT now(),
  deleted timestamp with time zone
);

-- 
--  installation
-- 
CREATE TYPE enum_installation_type AS ENUM ('IPT_INSTALLATION', 'DIGIR_INSTALLATION',
'TAPIR_INSTALLATION', 'BIOCASE_INSTALLATION', 'HTTP_INSTALLATION');
CREATE TABLE installation
(
  key uuid NOT NULL PRIMARY KEY,
  organization_key uuid NOT NULL REFERENCES organization(key),
  type enum_installation_type NOT NULL,
  title varchar(255) CHECK (assert_min_length(title, 2)),
  description text CHECK (assert_min_length(description, 10)),
  created_by varchar(255) NOT NULL CHECK (assert_min_length(created_by, 3)),
  modified_by varchar(255) NOT NULL CHECK (assert_min_length(modified_by, 3)),
  created timestamp with time zone NOT NULL DEFAULT now(),
  modified timestamp with time zone NOT NULL DEFAULT now(),
  deleted timestamp with time zone
);

-- 
--  dataset
-- 
CREATE TYPE enum_dataset_type AS ENUM ('OCCURRENCE', 'CHECKLIST', 'METADATA');
CREATE TYPE enum_dataset_subtype AS ENUM ('TAXONOMIC_AUTHORITY', 'NOMENCLATOR_AUTHORITY', 'INVENTORY_THEMATIC', 
'INVENTORY_REGIONAL', 'GLOBAL_SPECIES_DATASET', 'DERIVED_FROM_OCCURRENCE', 'SPECIMEN', 'OBSERVATION');
CREATE TABLE dataset
(
  key uuid NOT NULL PRIMARY KEY,
  parent_dataset_key uuid REFERENCES dataset(key),
  duplicate_of_dataset_key uuid REFERENCES dataset(key),
  installation_key uuid NOT NULL REFERENCES installation(key),
  owning_organization_key uuid NOT NULL REFERENCES organization(key),
  external boolean NOT NULL DEFAULT false,
  type enum_dataset_type NOT NULL,
  sub_type enum_dataset_subtype DEFAULT NULL,
  title varchar(255) NOT NULL CHECK (assert_min_length(title, 2)),
  alias varchar(50) CHECK (assert_min_length(alias, 2)),
  abbreviation varchar(50) CHECK (assert_min_length(abbreviation, 1)),
  description text CHECK (assert_min_length(description, 10)),
  language char(2) NOT NULL CHECK (assert_min_length(language, 2)),
  homepage text CHECK (assert_is_http(homepage)),
  logo_url text CHECK (assert_is_http(logo_url)),
  citation text CHECK (assert_min_length(citation, 10)),
  citation_identifier varchar(100) CHECK (assert_min_length(citation_identifier, 1)),
  rights text CHECK (assert_min_length(rights, 1)),
  locked_for_auto_update boolean NOT NULL DEFAULT false,
  created_by varchar(255) NOT NULL CHECK (assert_min_length(created_by, 3)),
  modified_by varchar(255) NOT NULL CHECK (assert_min_length(modified_by, 3)),
  created timestamp with time zone NOT NULL DEFAULT now(),
  modified timestamp with time zone NOT NULL DEFAULT now(),
  deleted timestamp with time zone
);

-- 
--  network
-- 
CREATE TABLE network
(
  key uuid NOT NULL PRIMARY KEY,
  title varchar(255) NOT NULL CHECK (assert_min_length(title, 2)),
  description text CHECK (assert_min_length(description, 10)),
  language char(2) NOT NULL CHECK (assert_min_length(language, 2)),
  email varchar(254) CHECK (assert_min_length(email, 5)),
  phone text CHECK (assert_min_length(phone, 5)),
  homepage text CHECK (assert_is_http(homepage)),
  logo_url text CHECK (assert_is_http(logo_url)),
  address text CHECK (assert_min_length(address, 1)),
  city text CHECK (assert_min_length(city, 1)),
  province text CHECK (assert_min_length(province, 1)),
  country char(2) CHECK (assert_min_length(country, 2)),
  postal_code text CHECK (assert_min_length(postal_code, 1)),
  created_by varchar(255) NOT NULL CHECK (assert_min_length(created_by, 3)),
  modified_by varchar(255) NOT NULL CHECK (assert_min_length(modified_by, 3)),
  created timestamp with time zone NOT NULL DEFAULT now(),
  modified timestamp with time zone NOT NULL DEFAULT now(),
  deleted timestamp with time zone
);

-- 
--  tag
-- 
CREATE TABLE tag
(
  key serial NOT NULL PRIMARY KEY,
  value text NOT NULL CHECK (assert_min_length(value, 1)),
  created_by varchar(255) NOT NULL CHECK (assert_min_length(created_by, 3)),
  created timestamp with time zone NOT NULL DEFAULT now()
);

-- 
--  node_tag
-- 
CREATE TABLE node_tag
(
  node_key uuid NOT NULL REFERENCES node(key),
  tag_key integer NOT NULL UNIQUE REFERENCES tag(key) ON DELETE CASCADE,
  PRIMARY KEY (node_key, tag_key)
);

-- 
--  organization_tag
-- 
CREATE TABLE organization_tag
(
  organization_key uuid NOT NULL REFERENCES organization(key),
  tag_key integer NOT NULL UNIQUE REFERENCES tag(key) ON DELETE CASCADE,
  PRIMARY KEY (organization_key, tag_key)
);

-- 
--  installation_tag
-- 
CREATE TABLE installation_tag
(
  installation_key uuid NOT NULL REFERENCES installation(key),
  tag_key integer NOT NULL UNIQUE REFERENCES tag(key) ON DELETE CASCADE,
  PRIMARY KEY (installation_key, tag_key)
);

-- 
--  dataset_tag
-- 
CREATE TABLE dataset_tag
(
  dataset_key uuid NOT NULL REFERENCES dataset(key),
  tag_key integer NOT NULL UNIQUE REFERENCES tag(key) ON DELETE CASCADE,
  PRIMARY KEY (dataset_key, tag_key)
);

-- 
--  network_tag
-- 
CREATE TABLE network_tag
(
  network_key uuid NOT NULL REFERENCES network(key),
  tag_key integer NOT NULL UNIQUE REFERENCES tag(key) ON DELETE CASCADE,
  PRIMARY KEY (network_key, tag_key)
);

-- 
--  machine_tag
-- 
CREATE TABLE machine_tag
(
  key serial NOT NULL PRIMARY KEY,
  namespace varchar(255) NOT NULL CHECK (assert_min_length(value, 1)),
  name varchar(255) NOT NULL CHECK (assert_min_length(value, 1)),
  value varchar(255) NOT NULL CHECK (assert_min_length(value, 1)),
  created_by varchar(255) NOT NULL CHECK (assert_min_length(created_by, 3)),
  created timestamp with time zone NOT NULL DEFAULT now()
);

-- 
--  node_machine_tag
-- 
CREATE TABLE node_machine_tag
(
  node_key uuid NOT NULL REFERENCES node(key),
  machine_tag_key integer NOT NULL UNIQUE REFERENCES machine_tag(key)  ON DELETE CASCADE,
  PRIMARY KEY (node_key, machine_tag_key)
);

-- 
--  organization_machine_tag
-- 
CREATE TABLE organization_machine_tag
(
  organization_key uuid NOT NULL REFERENCES organization(key),
  machine_tag_key integer NOT NULL UNIQUE REFERENCES machine_tag(key) ON DELETE CASCADE,
  PRIMARY KEY (organization_key, machine_tag_key)
);

-- 
--  installation_machine_tag
-- 
CREATE TABLE installation_machine_tag
(
  installation_key uuid NOT NULL REFERENCES installation(key),
  machine_tag_key integer NOT NULL UNIQUE REFERENCES machine_tag(key) ON DELETE CASCADE,
  PRIMARY KEY (installation_key, machine_tag_key)
);

-- 
--  dataset_machine_tag
-- 
CREATE TABLE dataset_machine_tag
(
  dataset_key uuid NOT NULL REFERENCES dataset(key),
  machine_tag_key integer NOT NULL UNIQUE REFERENCES machine_tag(key) ON DELETE CASCADE,
  PRIMARY KEY (dataset_key, machine_tag_key)
);

-- 
--  network_machine_tag
-- 
CREATE TABLE network_machine_tag
(
  network_key uuid NOT NULL REFERENCES network(key),
  machine_tag_key integer NOT NULL UNIQUE REFERENCES machine_tag(key) ON DELETE CASCADE,
  PRIMARY KEY (network_key, machine_tag_key)
);

-- 
--  contact
-- 
CREATE TYPE enum_contact_type AS ENUM ('TECHNICAL_POINT_OF_CONTACT', 'ADMINISTRATIVE_POINT_OF_CONTACT', 'POINT_OF_CONTACT', 
'ORIGINATOR', 'METADATA_AUTHOR', 'PRINCIPAL_INVESTIGATOR', 'AUTHOR', 'CONTENT_PROVIDER', 'CUSTODIAN_STEWARD',
'DISTRIBUTOR', 'EDITOR', 'OWNER', 'PROCESSOR', 'PUBLISHER', 'USER', 'PROGRAMMER', 'DATA_ADMINISTRATOR', 'SYSTEM_ADMINISTRATOR',
'HEAD_OF_DELEGATION','REGIONAL_NODE_REPRESENTATIVE','VICE_CHAIR','NODE_MANAGER','NODE_STAFF');
CREATE TABLE contact
(
  key serial NOT NULL PRIMARY KEY,
  name text CHECK (assert_min_length(name, 1)),
  description text CHECK (assert_min_length(description, 10)),
  position text CHECK (assert_min_length(position, 2)),
  email varchar(254) CHECK (assert_min_length(email, 5)),
  phone text CHECK (assert_min_length(phone, 5)),
  organization text CHECK (assert_min_length(organization, 2)), 
  address text CHECK (assert_min_length(address, 1)),
  city text CHECK (assert_min_length(city, 1)),
  province text CHECK (assert_min_length(province, 1)),
  country char(2) CHECK (assert_min_length(country, 2)),
  postal_code text CHECK (assert_min_length(postal_code, 1)),
  created_by varchar(255) NOT NULL CHECK (assert_min_length(created_by, 3)),
  modified_by varchar(255) NOT NULL CHECK (assert_min_length(modified_by, 3)),
  created timestamp with time zone NOT NULL DEFAULT now(),
  modified timestamp with time zone NOT NULL DEFAULT now()
);

-- 
--  node_contact
-- 
CREATE TABLE node_contact
(
  node_key uuid NOT NULL REFERENCES node(key),
  contact_key integer NOT NULL UNIQUE REFERENCES contact(key) ON DELETE CASCADE,
  type enum_contact_type NOT NULL,
  is_primary boolean NOT NULL,
  PRIMARY KEY (node_key, contact_key)
);
CREATE UNIQUE INDEX unique_node_contact ON node_contact(node_key, type) WHERE is_primary; 

-- 
--  organization_contact
-- 
CREATE TABLE organization_contact
(
  organization_key uuid NOT NULL REFERENCES organization(key),
  contact_key integer NOT NULL UNIQUE REFERENCES contact(key) ON DELETE CASCADE,
  type enum_contact_type  DEFAULT NULL,
  is_primary boolean NOT NULL,  
  PRIMARY KEY (organization_key, contact_key)
);
CREATE UNIQUE INDEX unique_organization_contact ON organization_contact(organization_key, type) WHERE is_primary; 

-- 
--  installation_contact
-- 
CREATE TABLE installation_contact
(
  installation_key uuid NOT NULL REFERENCES installation(key),
  contact_key integer NOT NULL UNIQUE REFERENCES contact(key) ON DELETE CASCADE,
  type enum_contact_type  DEFAULT NULL,
  is_primary boolean NOT NULL,  
  PRIMARY KEY (installation_key, contact_key)
);
CREATE UNIQUE INDEX unique_installation_contact ON installation_contact(installation_key, type) WHERE is_primary;

-- 
--  dataset_contact
-- 
CREATE TABLE dataset_contact
(
  dataset_key uuid NOT NULL REFERENCES dataset(key),
  contact_key integer NOT NULL UNIQUE REFERENCES contact(key) ON DELETE CASCADE,
  type enum_contact_type  DEFAULT NULL,
  is_primary boolean NOT NULL,  
  PRIMARY KEY (dataset_key, contact_key)
);
CREATE UNIQUE INDEX unique_dataset_contact ON dataset_contact(dataset_key, type) WHERE is_primary;

-- 
--  network_contact
-- 
CREATE TABLE network_contact
(
  network_key uuid NOT NULL REFERENCES network(key),
  contact_key integer NOT NULL UNIQUE REFERENCES contact(key) ON DELETE CASCADE,
  type enum_contact_type  DEFAULT NULL,
  is_primary boolean NOT NULL,  
  PRIMARY KEY (network_key, contact_key)
);
CREATE UNIQUE INDEX unique_network_contact ON network_contact(network_key, type) WHERE is_primary;

-- 
--  endpoint
-- 
CREATE TYPE enum_endpoint_type AS ENUM ('EML', 'FEED', 'WFS', 'WMS', 'TCS_RDF', 'TCS_XML', 'DWC_ARCHIVE', 'DIGIR', 'DIGIR_MANIS',
'TAPIR', 'BIOCASE', 'OAI_PMH', 'OTHER');
CREATE TABLE endpoint
(
  key serial NOT NULL PRIMARY KEY,
  type enum_endpoint_type NOT NULL,
  url text CHECK (assert_min_length(url, 10)),
  description text CHECK (assert_min_length(description, 10)),
  created_by varchar(255) NOT NULL CHECK (assert_min_length(created_by, 3)),
  modified_by varchar(255) NOT NULL CHECK (assert_min_length(modified_by, 3)),
  created timestamp with time zone NOT NULL DEFAULT now(),
  modified timestamp with time zone NOT NULL DEFAULT now()
);

--
--	node_endpoint
--
CREATE TABLE node_endpoint
(
	node_key uuid NOT NULL REFERENCES node(key),
	endpoint_key integer NOT NULL UNIQUE REFERENCES endpoint(key) ON DELETE CASCADE,
  	PRIMARY KEY (node_key, endpoint_key)
);

-- 
--  organization_endpoint
-- 
CREATE TABLE organization_endpoint
(
  organization_key uuid NOT NULL REFERENCES organization(key),
  endpoint_key integer NOT NULL UNIQUE REFERENCES endpoint(key) ON DELETE CASCADE,
  PRIMARY KEY (organization_key, endpoint_key)
);

-- 
--  installation_endpoint
-- 
CREATE TABLE installation_endpoint
(
  installation_key uuid NOT NULL REFERENCES installation(key),
  endpoint_key integer NOT NULL UNIQUE REFERENCES endpoint(key) ON DELETE CASCADE,
  PRIMARY KEY (installation_key, endpoint_key)
);

-- 
--  dataset_endpoint
-- 
CREATE TABLE dataset_endpoint
(
  dataset_key uuid NOT NULL REFERENCES dataset(key),
  endpoint_key integer NOT NULL UNIQUE REFERENCES endpoint(key) ON DELETE CASCADE,
  PRIMARY KEY (dataset_key, endpoint_key)
);

-- 
--  network_endpoint
-- 
CREATE TABLE network_endpoint
(
  network_key uuid NOT NULL REFERENCES network(key),
  endpoint_key integer NOT NULL UNIQUE REFERENCES endpoint(key) ON DELETE CASCADE,
  PRIMARY KEY (network_key, endpoint_key)
);

-- 
--  identifier
-- 
CREATE TYPE enum_identifier_type AS ENUM ('SOURCE_ID', 'URL', 'LSID', 'HANDLER', 'DOI', 'UUID', 
'FTP', 'URI', 'UNKNOWN', 'GBIF_PORTAL', 'GBIF_NODE', 'GBIF_PARTICIPANT');
CREATE TABLE identifier
(
  key serial NOT NULL PRIMARY KEY,
  type enum_identifier_type NOT NULL,
  identifier text NOT NULL CHECK (assert_min_length(identifier, 1)),
  created_by varchar(255) NOT NULL CHECK (assert_min_length(created_by, 3)),
  created timestamp with time zone NOT NULL DEFAULT now()
);

-- 
--  organization_identifier
-- 
CREATE TABLE organization_identifier
(
  organization_key uuid NOT NULL REFERENCES organization(key),
  identifier_key integer NOT NULL UNIQUE REFERENCES identifier(key) ON DELETE CASCADE,
  PRIMARY KEY (organization_key, identifier_key)
);

-- 
--  dataset_identifier
-- 
CREATE TABLE dataset_identifier
(
  dataset_key uuid NOT NULL REFERENCES dataset(key),
  identifier_key integer NOT NULL UNIQUE REFERENCES identifier(key) ON DELETE CASCADE,
  PRIMARY KEY (dataset_key, identifier_key)
);

--
--  node_identifier
--
CREATE TABLE node_identifier
(
  node_key uuid NOT NULL REFERENCES node(key),
  identifier_key integer NOT NULL UNIQUE REFERENCES identifier(key) ON DELETE CASCADE,
  PRIMARY KEY (node_key, identifier_key)
);

--
--  comment
-- 
CREATE TABLE comment
(
  key serial NOT NULL PRIMARY KEY,
  content text NOT NULL CHECK (assert_min_length(content, 1)),
  created_by varchar(255) NOT NULL CHECK (assert_min_length(created_by, 3)),
  modified_by varchar(255) NOT NULL CHECK (assert_min_length(modified_by, 3)),
  created timestamp with time zone NOT NULL DEFAULT now(),
  modified timestamp with time zone NOT NULL DEFAULT now()
);

-- 
--  node_comment
-- 
CREATE TABLE node_comment
(
  node_key uuid NOT NULL REFERENCES node(key),
  comment_key integer NOT NULL UNIQUE REFERENCES comment(key) ON DELETE CASCADE,
  PRIMARY KEY (node_key, comment_key)
);

-- 
--  organization_comment
-- 
CREATE TABLE organization_comment
(
  organization_key uuid NOT NULL REFERENCES organization(key),
  comment_key integer NOT NULL UNIQUE REFERENCES comment(key) ON DELETE CASCADE,
  PRIMARY KEY (organization_key, comment_key)
);

-- 
--  installation_comment
-- 
CREATE TABLE installation_comment
(
  installation_key uuid NOT NULL REFERENCES installation(key),
  comment_key integer NOT NULL UNIQUE REFERENCES comment(key) ON DELETE CASCADE,
  PRIMARY KEY (installation_key, comment_key)
);

-- 
--  dataset_comment
-- 
CREATE TABLE dataset_comment
(
  dataset_key uuid NOT NULL REFERENCES dataset(key),
  comment_key integer NOT NULL UNIQUE REFERENCES comment(key) ON DELETE CASCADE,
  PRIMARY KEY (dataset_key, comment_key)
);

-- 
--  network_comment
-- 
CREATE TABLE network_comment
(
  network_key uuid NOT NULL REFERENCES network(key),
  comment_key integer NOT NULL UNIQUE REFERENCES comment(key) ON DELETE CASCADE,
  PRIMARY KEY (network_key, comment_key)
);

-- 
--  endpoint_machine_tag
-- 
CREATE TABLE endpoint_machine_tag
(
  endpoint_key integer NOT NULL REFERENCES endpoint(key),
  machine_tag_key integer NOT NULL UNIQUE REFERENCES machine_tag(key) ON DELETE CASCADE,
  PRIMARY KEY (endpoint_key, machine_tag_key)
);

-- 
--  dataset_network
-- 
CREATE TABLE dataset_network
(
  dataset_key uuid NOT NULL REFERENCES dataset(key),
  network_key uuid NOT NULL REFERENCES network(key),
  PRIMARY KEY (dataset_key, network_key)
);

-- 
--  metadata
-- 
CREATE TYPE enum_metadata_type AS ENUM ('EML');
CREATE TABLE metadata
(
  key serial NOT NULL PRIMARY KEY,
  dataset_key uuid NOT NULL UNIQUE REFERENCES dataset(key),
  type enum_metadata_type NOT NULL,
  version varchar(50) NOT NULL CHECK (assert_min_length(version, 1)),
  content text NOT NULL CHECK (assert_min_length(content, 1)),
  created_by varchar(255) NOT NULL CHECK (assert_min_length(created_by, 3)),
  modified_by varchar(255) NOT NULL CHECK (assert_min_length(modified_by, 3)),
  created timestamp with time zone NOT NULL DEFAULT now(),
  modified timestamp with time zone NOT NULL DEFAULT now()
);
