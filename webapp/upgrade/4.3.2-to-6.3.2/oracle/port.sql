-- Script for upgrading Oracle port database from Entando 4.3.2 to 6.3.2
-- Not a final target: continue with 6.3.2-to-7.0.0/oracle/port.sql to reach 7.0.0.
--
-- NOTE: the CLOB/XML statements marked below (contents.sync comparison via DBMS_LOB.COMPARE,
-- contents.published extraction via EXTRACTVALUE, and the cthread_config CLOB assembled with
-- TO_CLOB because the value exceeds the 4000-char literal limit) use Oracle-specific
-- constructs and should be verified before running.

-- ************* To 5.0.0 *************

ALTER TABLE pages_metadata_draft ADD (groupcode character varying(30));
ALTER TABLE pages_metadata_online ADD (groupcode character varying(30));
UPDATE pages_metadata_online o SET groupcode = (SELECT p.groupcode FROM pages p WHERE o.code = p.code);
UPDATE pages_metadata_draft d SET groupcode = (SELECT p.groupcode FROM pages p WHERE d.code = p.code);
ALTER TABLE pages DROP COLUMN groupcode;

INSERT INTO sysconfig (version, item, descr, config) VALUES ('production', 'jacms_resourceMetadataMapping', 'Mapping between resource Metadata and resource attribute fields', '<mapping>
    <field key="alt"></field>
    <field key="description"></field>
    <field key="legend"></field>
    <field key="title"></field>
</mapping>');

-- ************* To 5.3.2 *************

ALTER TABLE resources ADD (owner character varying(128));

ALTER TABLE contents ADD (sync smallint);
-- NOTE: CLOB columns cannot be compared with '='; DBMS_LOB.COMPARE returns 0 when equal
UPDATE contents SET sync = 1 WHERE DBMS_LOB.COMPARE(workxml, onlinexml) = 0;
UPDATE contents SET sync = 0 WHERE DBMS_LOB.COMPARE(workxml, onlinexml) <> 0 AND onlinexml IS NOT NULL;
UPDATE contents SET sync = 0 WHERE onlinexml IS NULL;

ALTER TABLE contents ADD (published character varying(20));
-- NOTE: extracts /root/lastModified from the online content XML (Oracle EXTRACTVALUE)
UPDATE contents SET published = EXTRACTVALUE(XMLType(onlinexml), '/*/lastModified') WHERE onlinexml IS NOT NULL;

CREATE INDEX contents_sync_idx ON contents (sync);

-- ************* To 6.2.0 *************

ALTER TABLE contents ADD (restriction character varying(40));
ALTER TABLE resources ADD (folderpath character varying(256));
ALTER TABLE widgetcatalog ADD (bundleid character varying(150));
ALTER TABLE widgetcatalog ADD (configui CLOB);

CREATE TABLE jpseo_friendlycode (
    friendlycode character varying(256) NOT NULL,
    pagecode character varying(30),
    contentid character varying(16),
    langcode character varying(2),
    CONSTRAINT jpseo_friendlycode_pkey PRIMARY KEY (friendlycode)
);

CREATE TABLE jpversioning_trashedresources (
    resid character varying(16) NOT NULL,
    restype character varying(30) NOT NULL,
    descr character varying(100) NOT NULL,
    maingroup character varying(20) NOT NULL,
    resxml CLOB NOT NULL,
    CONSTRAINT jpversioning_trashedresources_pkey PRIMARY KEY (resid)
);

CREATE TABLE jpversioning_versionedcontents (
    id integer NOT NULL,
    contentid character varying(16) NOT NULL,
    contenttype character varying(30) NOT NULL,
    descr character varying(255) NOT NULL,
    status character varying(12) NOT NULL,
    contentxml CLOB NOT NULL,
    versiondate timestamp NOT NULL,
    versioncode character varying(7) NOT NULL,
    onlineversion integer NOT NULL,
    approved smallint NOT NULL,
    username character varying(40),
    CONSTRAINT jpversioning_versionedcontents_pkey PRIMARY KEY (id),
    CONSTRAINT jpvers_contentvers_key UNIQUE (contentid, versioncode)
);

INSERT INTO sysconfig (version, item, descr, config) VALUES ('production', 'jpmail_config', 'Configurazione del servizio di invio eMail', '<?xml version="1.0" encoding="UTF-8"?>
<mailConfig>
	<senders>
		<sender code="CODE1">EMAIL1@EMAIL.COM</sender>
		<sender code="CODE2">EMAIL2@EMAIL.COM</sender>
	</senders>
	<smtp debug="false">
		<host>localhost</host>
		<port>25000</port>
		<user></user>
		<password></password>
		<security>std</security>
	</smtp>
</mailConfig>');

INSERT INTO sysconfig (version, item, descr, config) VALUES ('production', 'cthread_config', 'Configurazione thread pubblicazione/sospensione automatica',
  TO_CLOB('<contentThreadconfig sitecode="A">
<!--attiva o disattiva lo scheduler.
I valori da assegnare all attributo active possono essere true o false
 se il valore settato è true , lo scheduler è attivo
 se il valore settato è false , lo scheduler non è attivo
-->
<scheduler active="true"/>
<!--categoria globale a cui verranno associati i contenuti in fase di depubblicazione o sospensione del contenuto.
In corrispondenza della voce="code" va messo il codice della categoria;
è obbligatorio mettere questa categoria in quanto verrà utilizzata per lo spostamento dei contenuti 
nell archivio online in mancanza di categorie specifiche -->    
<globalcat code="archive"/> 
<!--contenuto e modello di contenuto sostitutivo globale, verrà utilizzato nel caso in cui non vi sia specificato 
l idContentReplace e il modelIdContentReplace nel tag contentType.
    nell attributo contentId = deve essere inserito l id del contenuto sostitutivo es. ART1226933
    nell attributo modelId = deve essere inserito l id del modello del contenuto sostitutivo   es. 12
   La definizione di questo contenuto sostitutivo globale è obbligatoria
-->   
<contentReplace contentId="ART1226933" modelId="12"/> 
<contentTypes>
<!--
startAttr: quest attributo deve essere valorizzato con il nome del campo in cui sarà specificata la data di pubblicazione.(obbligatorio)
endAttr: quest attributo deve essere valorizzato con il nome del campo in cui sarà specificata la data di sospensione.(obbligatorio)
idContentReplace: quest attributo deve essere valorizzato con il nome del campo in cui sarà specificato l id del contenuto da sostituire(opzionale)
modelIdContentReplace: quest attributo deve essere valorizzato con il nome del campo in cui sarà specificato il modelid del contenuto da sostituire(opzionale)
Nel caso in cui non vengano valorizzati idContentReplace e modelIdContentReplace, verrà utilizzato il contenuto sostitutivo globale definito precedentemente

Suspend: l attributo suspend è obbligatorio ed è necessario per definire l azione da svolgere(valori possono essere true o false)
- se è settato a true,  predispone la sospensione dei contenuti per la tipologia di contenuti in cui è settato
- se è settato a false, predispone lo  spostamento nell archivio online dei contenuti della tipologia di contenuto in cui è settato 
-->
        <contentType type="NOL" startAttr="Data_inizio" endAttr="Data_fine" idContentReplace="Id_contenuto_sost" modelIdContentReplace="Model_id" suspend="true">
                <!--categorie associate al tipo di contenuto e che verranno associate ai contenuti 
                in fase di depubblicazione o sospensione 
                nell attributo code deve essere specificato il codice della categoria-->
    <!--<category code="cnol_canale7" />
    <category code="cnol_canale3" />
    <category code="cnol_canale4" />
    <category code="cnol_canale5" />
    <category code="cnol_canale2" />
    <category code="cnol_canale1" />
    <category code="cnol_canale6" />
') ||
  TO_CLOB('    <category code="cnol_canale8" /> -->
        </contentType>
<contentType type="DRT" startAttr="Data_inizio" endAttr="Data_fine" suspend="true" >
         <!--categorie associate al tipo di contenuto e che verranno associate ai contenuti in fase di depubblicazione o sospensione 
                nell attributo code deve essere specificato il codice della categoria-->
    <!--<category code="pubb_drt" />
    <category code="cnol_canale8" /> -->
   </contentType>
  <contentType type="SNT" startAttr="Data_inizio" endAttr="Data_fine" suspend="true" >
        <!--non essendoci categorie associate, ai contenuti di questo tipo verrà associata la categoria globale -->
   </contentType>
</contentTypes>

<!-- i gruppi attualmente non vengono usati-->
<groups> 
          <group id="gruppoDiProvaThread" contentType="NOL" /> 
</groups>
  
<!-- utenti che riceveranno le comunicazioni sull andamento delle operazioni di pubblicazione, sospensione e spostamento -->
<users>
  <user username="admin" contentType="*" />
</users>


<!-- template della mail di comunicazione-->
<mail alsoHtml="false" senderCode="CODE1" mailAttrName="email" >    
<subject><![CDATA[Report pubblicazione/sospensione automatica]]></subject>     
 <htmlHeader><![CDATA[<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"> <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="it">  <head> </head>  <body><div style="padding:1.5em;font-family:Arial, sans serif; color: #333333;font-size:0.9em">Report  Pubblicazione/Sospensione automatica contenuti<br/><p>Contenuti pubblicati:</p>]]></htmlHeader> 
     <htmlFooter><![CDATA[<br /><br />Cordiali Saluti<br/></div></body></html>]]> </htmlFooter>       
<htmlSeparator> <![CDATA[ <br /> Contenuti sospesi: ]]> </htmlSeparator>     
 <textHeader><![CDATA[Report pubblicazione/sospensione automatica]]> </textHeader>  
<textFooter><![CDATA[   Cordiali Saluti.]]> </textFooter>      
<textSeparator><![CDATA[

Contenuti sospesi:
 ]]></textSeparator>  

<htmlSeparatorMove> <![CDATA[ <br /> Contenuti spostati in archivio online: ]]> </htmlSeparatorMove> 
 <textHeaderMove><![CDATA[Report pubblicazione/sospensione automatica]]> </textHeaderMove>  
<textFooterMove><![CDATA[   Cordiali Saluti.]]> </textFooterMove>  
<textSeparatorMove><![CDATA[

Contenuti spostati in archivio online:
 ]]></textSeparatorMove>   

</mail> </contentThreadconfig>'));

-- ************* To 6.3.2 *************

CREATE TABLE userpreferences
( username character varying(80) NOT NULL,
  wizard smallint NOT NULL,
  loadonpageselect smallint NOT NULL,
  translationwarning smallint NOT NULL,
  CONSTRAINT userpreferences_pkey PRIMARY KEY (username)
);

ALTER TABLE userpreferences ADD (defaultpageownergroup character varying(64));
ALTER TABLE userpreferences ADD (defaultpagejoingroups character varying(256));
ALTER TABLE userpreferences ADD (defaultcontentownergroup character varying(64));
ALTER TABLE userpreferences ADD (defaultcontentjoingroups character varying(256));
ALTER TABLE userpreferences ADD (defaultwidgetownergroup character varying(64));
ALTER TABLE userpreferences ADD (defaultwidgetjoingroups character varying(256));

ALTER TABLE resources ADD (correlationcode character varying(256));

ALTER TABLE widgetcatalog ADD (readonlypagewidgetconfig NUMBER(1) DEFAULT 0);
ALTER TABLE widgetcatalog ADD (widgetcategory character varying(80));
ALTER TABLE widgetcatalog ADD (icon character varying(80));
