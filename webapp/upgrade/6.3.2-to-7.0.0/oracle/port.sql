-- Script for upgrading Oracle port database from Entando 6.3.2 to 7.0.0

-- core

DELETE FROM sysconfig WHERE item = 'dataTypeDefinitions';
DELETE FROM sysconfig WHERE item = 'dataobjectsubdir';

ALTER TABLE guifragment MODIFY locked NULL;
ALTER TABLE widgetcatalog MODIFY locked NULL;
ALTER TABLE pages MODIFY pos NULL;
ALTER TABLE localstrings MODIFY stringvalue NULL;

CREATE TABLE DATABASECHANGELOG (
                                   ID VARCHAR2(255) NOT NULL,
                                   AUTHOR VARCHAR2(255) NOT NULL,
                                   FILENAME VARCHAR2(255) NOT NULL,
                                   DATEEXECUTED TIMESTAMP NOT NULL,
                                   ORDEREXECUTED NUMBER(10) NOT NULL,
                                   EXECTYPE VARCHAR2(10) NOT NULL,
                                   MD5SUM VARCHAR2(35) DEFAULT NULL,
                                   DESCRIPTION VARCHAR2(255) DEFAULT NULL,
                                   COMMENTS VARCHAR2(255) DEFAULT NULL,
                                   TAG VARCHAR2(255) DEFAULT NULL,
                                   LIQUIBASE VARCHAR2(20) DEFAULT NULL,
                                   CONTEXTS VARCHAR2(255) DEFAULT NULL,
                                   LABELS VARCHAR2(255) DEFAULT NULL,
                                   DEPLOYMENT_ID VARCHAR2(10) DEFAULT NULL
);

CREATE TABLE DATABASECHANGELOGLOCK (
                                       ID NUMBER(10) NOT NULL,
                                       LOCKED NUMBER(1) NOT NULL,
                                       LOCKGRANTED TIMESTAMP DEFAULT NULL,
                                       LOCKEDBY VARCHAR2(255) DEFAULT NULL,
                                       PRIMARY KEY (ID)
);

INSERT INTO DATABASECHANGELOG (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES
    ('00000000000001_schemaPort','entando','liquibase/port/00000000000001_schemaPort.xml',TO_TIMESTAMP('2022-03-03 12:12:32.446304', 'YYYY-MM-DD HH24:MI:SS.FF6'),1,'EXECUTED','8:ca0f8957e59d0a535f286d630767e814','createTable tableName=sysconfig; createTable tableName=categories; createTable tableName=localstrings; createTable tableName=pagemodels; createTable tableName=pages; createTable tableName=pages_metadata_online; addForeignKeyConstraint baseTableNam...','',NULL,'4.4.3',NULL,NULL,'6305951931');
INSERT INTO DATABASECHANGELOG (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES
    ('00000000000001_dataPort_production','entando','liquibase/port/00000000000001_dataPort_production.xml',TO_TIMESTAMP('2022-03-03 12:12:32.72679', 'YYYY-MM-DD HH24:MI:SS.FF5'),2,'EXECUTED','8:f1d2f81737cddd29dfa883fdc990faa0','insert tableName=categories; insert tableName=localstrings; insert tableName=localstrings; insert tableName=localstrings; insert tableName=localstrings; insert tableName=localstrings; insert tableName=localstrings; insert tableName=localstrings; i...','',NULL,'4.4.3','production',NULL,'6305951931');
INSERT INTO DATABASECHANGELOG (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES
    ('00000000000001_de_defaultResources_dataPort_production','entando','liquibase/defaultResources/port/00000000000001_dataPort_production.xml',TO_TIMESTAMP('2022-03-03 12:12:35.532485', 'YYYY-MM-DD HH24:MI:SS.FF6'),6,'EXECUTED','8:7de85f47914acfe5b22c8c7db3228f09','insert tableName=widgetcatalog; insert tableName=guifragment; insert tableName=localstrings; insert tableName=localstrings; insert tableName=localstrings; insert tableName=localstrings; insert tableName=localstrings; insert tableName=localstrings;...','',NULL,'4.4.3','production',NULL,'6305955238');

-- jacms

ALTER TABLE contentattributeroles MODIFY (attrname character varying(30));
ALTER TABLE contentattributeroles MODIFY (rolename character varying(50));
ALTER TABLE contentsearch MODIFY (attrname character varying(30));
ALTER TABLE workcontentattributeroles MODIFY (attrname character varying(30));
ALTER TABLE workcontentattributeroles MODIFY (rolename character varying(30));
ALTER TABLE workcontentsearch MODIFY (attrname character varying(30));
ALTER TABLE resourcerelations MODIFY (refcategory character varying(30));

INSERT INTO DATABASECHANGELOG (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES
    ('00000000000001_jacms_schemaPort','entando','liquibase/jacms/port/00000000000001_schemaPort.xml',TO_TIMESTAMP('2022-03-03 12:12:34.162904', 'YYYY-MM-DD HH24:MI:SS.FF6'),3,'EXECUTED','8:4b138e1a635c359489a8b4dbeae439cb','createTable tableName=contentmodels; createTable tableName=contents; createIndex indexName=contents_contenttype_idx, tableName=contents; createIndex indexName=contents_lastmodified_idx, tableName=contents; createIndex indexName=contents_maingroup_...','',NULL,'4.4.3',NULL,NULL,'6305953882');
INSERT INTO DATABASECHANGELOG (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES
    ('00000000000001_jacms_dataPort_production','entando','liquibase/jacms/port/00000000000001_dataPort_production.xml',TO_TIMESTAMP('2022-03-03 12:12:34.262709', 'YYYY-MM-DD HH24:MI:SS.FF6'),4,'EXECUTED','8:563907e57e3ac8a1e6e8f1dc4a7467d0','insert tableName=sysconfig; insert tableName=sysconfig; insert tableName=sysconfig; insert tableName=sysconfig; insert tableName=localstrings; insert tableName=localstrings; insert tableName=localstrings; insert tableName=localstrings; insert tabl...','',NULL,'4.4.3','production',NULL,'6305953882');

-- jpversioning

ALTER TABLE jpversioning_versionedcontents MODIFY (descr character varying(256));

INSERT INTO DATABASECHANGELOG (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES
    ('00000000000001_jpversioning_schemaPort','entando','liquibase/jpversioning/port/00000000000001_schemaPort.xml',TO_TIMESTAMP('2022-03-03 12:12:35.877767', 'YYYY-MM-DD HH24:MI:SS.FF6'),7,'EXECUTED','8:fa3f7bbe1e84964a52b5d49609df135b','createTable tableName=jpversioning_trashedresources; createTable tableName=jpversioning_versionedcontents; addUniqueConstraint constraintName=jpvers_contentvers_key, tableName=jpversioning_versionedcontents','',NULL,'4.4.3',NULL,NULL,'6305955828');

-- jpmail

INSERT INTO DATABASECHANGELOG (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES
    ('00000000000001_jpmail_dataPort_production','entando','liquibase/jpmail/port/00000000000001_dataPort_production.xml',TO_TIMESTAMP('2022-03-03 12:12:34.621258', 'YYYY-MM-DD HH24:MI:SS.FF6'),5,'EXECUTED','8:a7e88f5e191c357dd3e503963b0730ba','insert tableName=sysconfig','',NULL,'4.4.3','production',NULL,'6305954611');

-- jpseo

INSERT INTO DATABASECHANGELOG (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES
    ('00000000000001_jpseo_schemaPort','entando','liquibase/jpseo/port/00000000000001_schemaPort.xml',TO_TIMESTAMP('2022-03-03 12:12:36.252658', 'YYYY-MM-DD HH24:MI:SS.FF6'),8,'EXECUTED','8:0cac575831787759faf345608323ed1f','createTable tableName=jpseo_friendlycode','',NULL,'4.4.3',NULL,NULL,'6305956213');
INSERT INTO DATABASECHANGELOG (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES
    ('00000000000001_jpseo_dataPort_production','entando','liquibase/jpseo/port/00000000000001_dataPort_production.xml',TO_TIMESTAMP('2022-03-03 12:12:36.280257', 'YYYY-MM-DD HH24:MI:SS.FF6'),9,'EXECUTED','8:283a24fe0ff8c95c4806a1fc9ecc5fa9','insert tableName=widgetcatalog; insert tableName=guifragment; insert tableName=guifragment','',NULL,'4.4.3','production',NULL,'6305956213');

-- jpcontentscheduler

INSERT INTO DATABASECHANGELOG (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id)
VALUES('00000000000001_jpcontentscheduler_dataPort_production', 'entando', 'liquibase/jpcontentscheduler/port/00000000000001_dataPort_production.xml', TO_TIMESTAMP('2022-03-03 15:44:33.948', 'YYYY-MM-DD HH24:MI:SS.FF3'), 8, 'EXECUTED', '8:ea617c252a9fd7ef7158ce075325969b', 'insert tableName=sysconfig', '', NULL, '4.4.3', 'production', NULL, '6318673902');

-- content templates (7.x compatibility)
-- Add a CSP nonce to inline <script> tags and migrate Velocity $velocityCount to $foreach.count

-- UPDATE pagemodels
-- SET templategui = REPLACE(templategui, '<script', '<script nonce="<@wp.cspNonce />"')
-- WHERE templategui LIKE '%<script%' AND templategui NOT LIKE '%nonce=%';
--
-- UPDATE guifragment
-- SET gui = REPLACE(gui, '<script', '<script nonce="<@wp.cspNonce />"')
-- WHERE gui LIKE '%<script%' AND gui NOT LIKE '%nonce=%';
--
-- UPDATE guifragment
-- SET defaultgui = REPLACE(defaultgui, '<script', '<script nonce="<@wp.cspNonce />"')
-- WHERE defaultgui LIKE '%<script%' AND defaultgui NOT LIKE '%nonce=%';
--
-- UPDATE pagemodels
-- SET templategui = '<#assign wp=JspTaglibs["/aps-core"]>' || CHR(10) || templategui
-- WHERE templategui LIKE '%cspNonce%' AND templategui NOT LIKE '%/aps-core%';
--
-- UPDATE guifragment
-- SET gui = '<#assign wp=JspTaglibs["/aps-core"]>' || CHR(10) || gui
-- WHERE gui LIKE '%cspNonce%' AND gui NOT LIKE '%/aps-core%';
--
-- UPDATE guifragment
-- SET defaultgui = '<#assign wp=JspTaglibs["/aps-core"]>' || CHR(10) || defaultgui
-- WHERE defaultgui LIKE '%cspNonce%' AND defaultgui NOT LIKE '%/aps-core%';
--
-- UPDATE contentmodels
-- SET model = REPLACE(model, '<script', '<script nonce="$content.nonce"')
-- WHERE model LIKE '%<script%' AND model NOT LIKE '%nonce=%';
--
-- UPDATE contentmodels
-- SET model = REPLACE(model, '$velocityCount', '$foreach.count')
-- WHERE model LIKE '%$velocityCount%';