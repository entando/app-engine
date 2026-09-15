-- Script for upgrading MySQL serv database from Entando 4.3.2 to 6.3.2
-- Not a final target: continue with 6.3.2-to-7.0.0/mysql/serv.sql to reach 7.0.0.

-- ************* To 5.0.0 *************
-- Nothing to do

-- ************* To 5.3.2 *************
-- Nothing to do

-- ************* To 6.2.0 *************

ALTER TABLE authusers MODIFY COLUMN username VARCHAR(80);
ALTER TABLE authusergrouprole MODIFY COLUMN username VARCHAR(80);

ALTER TABLE authuserprofileattrroles DROP FOREIGN KEY authuserprofileattrroles_fkey;
ALTER TABLE authuserprofilesearch DROP FOREIGN KEY authuserprofilesearch_fkey;

ALTER TABLE authuserprofiles MODIFY COLUMN username VARCHAR(80);
ALTER TABLE authuserprofileattrroles MODIFY COLUMN username VARCHAR(80);
ALTER TABLE authuserprofilesearch MODIFY COLUMN username VARCHAR(80);

ALTER TABLE authuserprofileattrroles ADD CONSTRAINT authuserprofileattrroles_fkey FOREIGN KEY (username) REFERENCES authuserprofiles(username);
ALTER TABLE authuserprofilesearch ADD CONSTRAINT authuserprofilesearch_fkey FOREIGN KEY (username) REFERENCES authuserprofiles(username);

ALTER TABLE authusershortcuts MODIFY COLUMN username VARCHAR(80);

ALTER TABLE actionlogrecords MODIFY COLUMN username VARCHAR(80);
ALTER TABLE actionlogrecords MODIFY COLUMN actionname VARCHAR(250);

-- ************* To 6.3.2 *************
-- Nothing to do