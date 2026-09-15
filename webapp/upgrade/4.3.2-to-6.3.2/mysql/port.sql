-- Script for upgrading MySQL port database from Entando 4.3.2 to 6.3.2
-- Not a final target: continue with 6.3.2-to-7.0.0/mysql/port.sql to reach 7.0.0.
--
-- NOTE: the CLOB/XML statements marked below (contents.sync comparison, contents.published
-- extraction) use MySQL-specific functions and should be verified before running.

-- ************* To 5.0.0 *************

ALTER TABLE pages_metadata_draft ADD COLUMN groupcode VARCHAR(30);
ALTER TABLE pages_metadata_online ADD COLUMN groupcode VARCHAR(30);
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

ALTER TABLE resources ADD COLUMN owner VARCHAR(128);

ALTER TABLE contents ADD COLUMN sync SMALLINT;
UPDATE contents SET sync = 1 WHERE workxml = onlinexml;
UPDATE contents SET sync = 0 WHERE workxml <> onlinexml AND onlinexml IS NOT NULL;
UPDATE contents SET sync = 0 WHERE onlinexml IS NULL;

ALTER TABLE contents ADD COLUMN published VARCHAR(20);
-- NOTE: extracts /root/lastModified from the online content XML (MySQL ExtractValue)
UPDATE contents SET published = ExtractValue(onlinexml, '/*/lastModified') WHERE onlinexml IS NOT NULL;

CREATE INDEX contents_sync_idx ON contents (sync);

-- ************* To 6.2.0 *************

ALTER TABLE contents ADD COLUMN restriction VARCHAR(40);

ALTER TABLE resources ADD COLUMN folderpath VARCHAR(256);

ALTER TABLE widgetcatalog ADD COLUMN bundleid VARCHAR(150);
ALTER TABLE widgetcatalog ADD COLUMN configui TEXT;

CREATE TABLE jpseo_friendlycode (
    friendlycode VARCHAR(256) NOT NULL,
    pagecode VARCHAR(30),
    contentid VARCHAR(16),
    langcode VARCHAR(2),
    CONSTRAINT jpseo_friendlycode_pkey PRIMARY KEY (friendlycode)
);

CREATE TABLE jpversioning_trashedresources (
    resid VARCHAR(16) NOT NULL,
    restype VARCHAR(30) NOT NULL,
    descr VARCHAR(100) NOT NULL,
    maingroup VARCHAR(20) NOT NULL,
    resxml LONGTEXT NOT NULL,
    CONSTRAINT jpversioning_trashedresources_pkey PRIMARY KEY (resid)
);

CREATE TABLE jpversioning_versionedcontents (
    id INT NOT NULL,
    contentid VARCHAR(16) NOT NULL,
    contenttype VARCHAR(30) NOT NULL,
    descr VARCHAR(255) NOT NULL,
    status VARCHAR(12) NOT NULL,
    contentxml LONGTEXT NOT NULL,
    versiondate DATETIME NOT NULL,
    versioncode VARCHAR(7) NOT NULL,
    onlineversion INT NOT NULL,
    approved SMALLINT NOT NULL,
    username VARCHAR(40),
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

INSERT INTO sysconfig (version, item, descr, config) VALUES ('production', 'cthread_config', 'Configurazione thread pubblicazione/sospensione automatica', '<contentThreadconfig sitecode="A">
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
    <category code="cnol_canale8" /> -->
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

</mail> </contentThreadconfig>');

-- ************* To 6.3.2 *************

CREATE TABLE userpreferences
(
    username VARCHAR(80) NOT NULL,
    wizard SMALLINT NOT NULL,
    loadonpageselect SMALLINT NOT NULL,
    translationwarning SMALLINT NOT NULL,
    defaultpageownergroup VARCHAR(64),
    defaultpagejoingroups VARCHAR(256),
    defaultcontentownergroup VARCHAR(64),
    defaultcontentjoingroups VARCHAR(256),
    defaultwidgetownergroup VARCHAR(64),
    defaultwidgetjoingroups VARCHAR(256),
    CONSTRAINT userpreferences_pkey PRIMARY KEY (username)
);

ALTER TABLE resources ADD COLUMN correlationcode VARCHAR(256);

ALTER TABLE widgetcatalog ADD COLUMN readonlypagewidgetconfig SMALLINT;
ALTER TABLE widgetcatalog ADD COLUMN widgetcategory VARCHAR(80);
ALTER TABLE widgetcatalog ADD COLUMN icon VARCHAR(80);
