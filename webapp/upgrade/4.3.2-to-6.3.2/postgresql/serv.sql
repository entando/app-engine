-- Script for upgrading PostgreSQL serv database from Entando 4.3.2 to 6.3.2
-- Not a final target: continue with 6.3.2-to-7.0.0/postgresql/serv.sql to reach 7.0.0.

-- ************* To 5.0.0 *************
-- Nothing to do

-- ************* To 5.3.2 *************
-- Nothing to do

-- ************* To 6.2.0 *************

ALTER TABLE authusers ALTER COLUMN username TYPE character varying(80);
ALTER TABLE authusergrouprole ALTER COLUMN username TYPE character varying(80);
ALTER TABLE authuserprofileattrroles ALTER COLUMN username TYPE character varying(80);
ALTER TABLE authusershortcuts ALTER COLUMN username TYPE character varying(80);

ALTER TABLE actionlogrecords ALTER COLUMN actionname TYPE character varying(250);

ALTER TABLE api_oauth_tokens ALTER COLUMN localuser TYPE character varying(255);

ALTER TABLE actionloglikerecords ALTER COLUMN id SET DEFAULT nextval('actionloglikerecords_id_seq'::regclass);
ALTER TABLE actionlogrelations ALTER COLUMN id SET DEFAULT nextval('actionlogrelations_id_seq'::regclass);

ALTER TABLE authrolepermissions ALTER COLUMN id SET DEFAULT nextval('authrolepermissions_id_seq'::regclass);
ALTER TABLE authusergrouprole ALTER COLUMN id SET DEFAULT nextval('authusergrouprole_id_seq'::regclass);
ALTER TABLE authuserprofileattrroles ALTER COLUMN id SET DEFAULT nextval('authuserprofileattrroles_id_seq'::regclass);
ALTER TABLE authuserprofilesearch ALTER COLUMN id SET DEFAULT nextval('authuserprofilesearch_id_seq'::regclass);

-- ************* To 6.3.2 *************
-- Nothing to do
