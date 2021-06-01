INSERT INTO widgetcatalog (code, titles, parameters, plugincode, parenttypecode, defaultconfig, locked) VALUES ('jpsolr_facetResults', '<?xml version="1.0" encoding="UTF-8"?>
<properties>
<property key="en">Facets Search Result</property>
<property key="it">Risultati Ricerca Faccette</property>
</properties>', '<config>
	<parameter name="contentTypesFilter">Content Type (optional)</parameter>
	<action name="facetNavResultConfig"/>
</config>', 'jpsolr', NULL, NULL, 1);

INSERT INTO widgetcatalog (code, titles, parameters, plugincode, parenttypecode, defaultconfig, locked) VALUES ('jpsolr_facetTree', '<?xml version="1.0" encoding="UTF-8"?>
<properties>
<property key="en">Facets Tree</property>
<property key="it">Albero delle faccette</property>
</properties>', '<config>
	<parameter name="facetRootNodes">Facet Category Root</parameter>
	<parameter name="contentTypesFilter">Content Type (optional)</parameter>
	<action name="facetNavTreeConfig"/>
</config>', 'jpsolr', NULL, NULL, 1);