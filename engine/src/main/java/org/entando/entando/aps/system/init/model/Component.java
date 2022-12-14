/*
 * Copyright 2015-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.aps.system.init.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Element;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

public class Component {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(Component.class);

    private String code;
    private String description;
    private String artifactId;
    private String artifactGroupId;
    private String artifactVersion;
    private List<String> dependencies;

    private Map<String, List<String>> tableNames;

    private Map<String, ComponentEnvironment> environments;

    private Map<String, String> liquibaseChangeSets;

    public Component(Element rootElement, Map<String, String> postProcessClasses) throws Throwable {
        try {
            code = rootElement.getChildText("code");
            description = rootElement.getChildText("description");
            Element artifactElement = rootElement.getChild("artifact");
            if (null != artifactElement) {
                artifactId = artifactElement.getChildText("artifactId");
                artifactGroupId = artifactElement.getChildText("groupId");
                artifactVersion = artifactElement.getChildText("version");
            }
            Element dependenciesElement = rootElement.getChild("dependencies");
            if (null != dependenciesElement) {
                List<Element> dependenciesElementd = dependenciesElement.getChildren("code");
                for (int i = 0; i < dependenciesElementd.size(); i++) {
                    Element element = dependenciesElementd.get(i);
                    this.addDependency(element.getText());
                }
            }
            Element installationElement = rootElement.getChild("installation");
            if (null != installationElement) {
                Element tableMappingElement = installationElement.getChild("tableMapping");
                this.extractTableMapping(tableMappingElement);
                List<Element> enviromentElements = installationElement.getChildren("environment");
                if (enviromentElements.size() > 0) {
                    environments = new HashMap<>();
                }
                for (int i = 0; i < enviromentElements.size(); i++) {
                    Element environmentElement = enviromentElements.get(i);
                    ComponentEnvironment environment
                            = new ComponentEnvironmentImpl(environmentElement, postProcessClasses);
                    environments.put(environment.getCode(), environment);
                }
                Element liquibaseElement = installationElement.getChild("liquibase");
                if (null != liquibaseElement) {
                    this.setLiquibaseChangeSets(new HashMap<>());
                    List<Element> changeSetElements = liquibaseElement.getChildren("changeSet");
                    for (int i = 0; i < changeSetElements.size(); i++) {
                        Element changeSetElement = changeSetElements.get(i);
                        String dataSource = changeSetElement.getAttributeValue("datasource");
                        String changeSet = changeSetElement.getAttributeValue("changeLogFile");
                        this.getLiquibaseChangeSets().put(dataSource, changeSet);
                    }

                }
            }
        } catch (Throwable t) {
            logger.error("Error loading component", t);
        }
    }

    private void extractTableMapping(Element tableMappingElement) {
        if (null != tableMappingElement) {
            this.setTableNames(new HashMap<>());
            List<Element> datasourceElements = tableMappingElement.getChildren("datasource");
            for (int i = 0; i < datasourceElements.size(); i++) {
                Element datasourceElement = datasourceElements.get(i);
                String datasourceName = datasourceElement.getAttributeValue("name");
                List<String> tableMapping = new ArrayList<>();
                List<Element> tableClasses = datasourceElement.getChildren("class");
                for (int j = 0; j < tableClasses.size(); j++) {
                    tableMapping.add(tableClasses.get(j).getText());
                }
                List<String> datasourceTableNames = new ArrayList<>();
                List<Element> tableElements = datasourceElement.getChildren("table");
                for (int j = 0; j < tableElements.size(); j++) {
                    datasourceTableNames.add(tableElements.get(j).getText());
                }
                if (!datasourceTableNames.isEmpty()) {
                    this.getTableNames().put(datasourceName, datasourceTableNames);
                }
            }
        }
    }

    public String getCode() {
        return code;
    }

    protected void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    protected void setDescription(String description) {
        this.description = description;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getArtifactGroupId() {
        return artifactGroupId;
    }

    public void setArtifactGroupId(String artifactGroupId) {
        this.artifactGroupId = artifactGroupId;
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    protected void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    protected void addDependency(String dependency) {
        if (null == dependency || dependency.trim().length() == 0) {
            return;
        }
        if (null == this.getDependencies()) {
            this.setDependencies(new ArrayList<>());
        }
        if (!this.getDependencies().contains(dependency)) {
            this.getDependencies().add(dependency);
        }
    }

    public Map<String, List<String>> getTableNames() {
        return tableNames;
    }

    protected void setTableNames(Map<String, List<String>> tableNames) {
        this.tableNames = tableNames;
    }

    public Map<String, ComponentEnvironment> getEnvironments() {
        return environments;
    }

    protected void setEnvironments(Map<String, ComponentEnvironment> environments) {
        this.environments = environments;
    }

    public Map<String, String> getLiquibaseChangeSets() {
        return liquibaseChangeSets;
    }
    protected void setLiquibaseChangeSets(Map<String, String> liquibaseChangeSets) {
        this.liquibaseChangeSets = liquibaseChangeSets;
    }

}
