-- Script for upgrading Oracle serv database from Entando 4.3.2 to 6.3.2
-- Not a final target: continue with 6.3.2-to-7.0.0/oracle/serv.sql to reach 7.0.0.

-- ************* To 5.0.0 *************
-- Nothing to do

-- ************* To 5.3.2 *************
-- Nothing to do

-- ************* To 6.2.0 *************

ALTER TABLE authusers MODIFY username VARCHAR2(80);
ALTER TABLE authusergrouprole MODIFY username VARCHAR2(80);
ALTER TABLE authuserprofileattrroles MODIFY username VARCHAR2(80);
ALTER TABLE authusershortcuts MODIFY username VARCHAR2(80);

-- ************* To 6.3.2 *************
-- Nothing to do