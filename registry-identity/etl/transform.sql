--
-- Creates 3 tables:
-- 1. The active user accounts in a the desired format (tmp_active_accounts)
-- 2. A table containing SQL statements suitable for usage against PG to update previously createdd user accounts (tmp_export1)
-- 3. A table containing SQL statements suitable for usage against PG to insert new user accounts (tmp_export2)
-- Note that accounts are not deleted in MySQL so no propagation of deletions is scripted for.

--
-- A temporary table of the active accounts and information we wish to export
--
DROP TABLE IF EXISTS tmp_active_accounts;
CREATE TABLE tmp_active_accounts AS
SELECT
  u.uid AS id,
  u.name AS username,
  u.pass AS password,
  REPLACE(u.mail, '\'', '\'\'') AS email,
  REPLACE(fn.field_firstname_value, '\'', '\'\'') AS first_name,
  REPLACE(ln.field_lastname_value, '\'', '\'\'') AS last_name,
  CONCAT("'country=>", iso.field_iso2_value, "'::hstore") AS settings,
  CONCAT('{"USER"', COALESCE(r.roles, '') ,'}') AS roles
FROM
  users u
    JOIN field_data_field_firstname fn ON fn.entity_id=u.uid AND fn.entity_type='user' AND fn.delta=0
    JOIN field_data_field_lastname ln ON ln.entity_id=u.uid AND ln.entity_type='user' AND ln.delta=0
    JOIN field_data_field_country_mono c ON c.entity_id=u.uid AND c.entity_type='user' AND c.delta=0
    JOIN field_data_field_iso2 iso ON iso.entity_id=c.field_country_mono_tid AND iso.entity_type='taxonomy_term' AND iso.delta=0
    LEFT JOIN (
      SELECT
        u.name AS username,
        CONCAT(',',GROUP_CONCAT(CONCAT('"', r.name, '"') SEPARATOR ',')) AS roles
      FROM
        users u
          JOIN users_roles ur ON ur.uid = u.uid
          JOIN role r ON r.rid = ur.rid
      WHERE
        r.name IN ('REGISTRY_ADMIN', 'REGISTRY_EDITOR')
      GROUP BY u.name
    ) r ON r.username = u.name
WHERE
  u.access <> 0 AND u.login <> 0 AND u.status = 1;

--
-- The following hacks create tables that give PG 9.3 compliant SQL to simulate UPSERTS.
-- Operations are idempotent, but not thread safe.
--

DROP TABLE IF EXISTS tmp_export1;
CREATE TABLE tmp_export1 AS
SELECT CONCAT(
  "UPDATE public.user SET ",
    " username='", username, "',",
    " password='", password, "',",
    " email='", email, "',",
    " first_name='", first_name, "',",
    " last_name='", last_name, "',",
    " settings=", settings, ",",
    " roles='", roles, "' ",
  "WHERE key=", id, ";"
) as updates
FROM tmp_active_accounts;

DROP TABLE IF EXISTS tmp_export2;
CREATE TABLE tmp_export2 AS
SELECT CONCAT(
  "INSERT INTO public.user(key, username, email, first_name, last_name, password, settings, roles) ",
    "SELECT ", id, ",",
    "'", username, "',",
    "'", email, "',",
    "'", first_name, "',",
    "'", last_name, "',",
    "'", password, "',",
    settings, ",",
    "'", roles, "' ",
  "WHERE NOT EXISTS (SELECT 1 FROM public.user WHERE key=", id, ");"
) as inserts
FROM tmp_active_accounts;
