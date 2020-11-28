/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpsolr.aps.system.solr;

import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.IIndexerDAO;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ISearchEngineDAOFactory;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ISearcherDAO;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.springframework.beans.factory.annotation.Value;

/**
 * Classe factory degli elementi ad uso del SearchEngine.
 *
 * @author E.Santoboni
 */
public class SearchEngineDAOFactory implements ISearchEngineDAOFactory {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(SearchEngineDAOFactory.class);
    
    @Value("${SOLR_ADDRESS:http://localhost:8983}")
    private String solrAddress;
    
    /*
    private String indexerClassName;
    private String searcherClassName;

    private String indexDiskRootFolder;
    private String subDirectory;
    */
    private ConfigInterface configManager;
    private ILangManager langManager;
    private ICategoryManager categoryManager;

    @Override
    public void init() throws Exception {
        /*
        this.subDirectory = this.getConfigManager().getConfigItem(JacmsSystemConstants.CONFIG_ITEM_CONTENT_INDEX_SUB_DIR);
        if (this.subDirectory == null) {
            throw new EntException("Item configurazione assente: " + JacmsSystemConstants.CONFIG_ITEM_CONTENT_INDEX_SUB_DIR);
        }
        */
    }

    @Override
    public boolean checkCurrentSubfolder() throws EntException {
        /*
        String currentSubDir = this.getConfigManager().getConfigItem(JacmsSystemConstants.CONFIG_ITEM_CONTENT_INDEX_SUB_DIR);
        boolean check = currentSubDir.equals(this.subDirectory);
        if (!check) {
            logger.info("Actual subfolder {} - Current subfolder {}", this.subDirectory, currentSubDir);
        }
        return check;
        */
        return true;
    }

    @Override
    public IIndexerDAO getIndexer() throws EntException {
        IndexerDAO indexerDao = new IndexerDAO();
        indexerDao.setLangManager(this.getLangManager());
        indexerDao.setTreeNodeManager(this.getCategoryManager());
        indexerDao.setSolrAddress(this.solrAddress);
        return indexerDao;
    }

    @Override
    public ISearcherDAO getSearcher() throws EntException {
        SearcherDAO searcherDao = new SearcherDAO();
        searcherDao.setTreeNodeManager(this.getCategoryManager());
        searcherDao.setLangManager(this.getLangManager());
        searcherDao.setSolrAddress(this.solrAddress);
        return searcherDao;
    }

    @Override
    public IIndexerDAO getIndexer(String subDir) throws EntException {
        /*
        IIndexerDAO indexerDao = null;
        try {
            Class indexerClass = Class.forName(this.getIndexerClassName());
            indexerDao = (IIndexerDAO) indexerClass.newInstance();
            indexerDao.setLangManager(this.getLangManager());
            indexerDao.setTreeNodeManager(this.getCategoryManager());
            indexerDao.init(this.getDirectory(subDir));
        } catch (Throwable t) {
            logger.error("Error getting indexer", t);
            throw new EntException("Error creating new indexer", t);
        }
        return indexerDao;
        */
        return this.getIndexer();
    }

    @Override
    public ISearcherDAO getSearcher(String subDir) throws EntException {
        /*
        ISearcherDAO searcherDao = null;
        try {
            Class searcherClass = Class.forName(this.getSearcherClassName());
            searcherDao = (ISearcherDAO) searcherClass.newInstance();
            searcherDao.setTreeNodeManager(this.getCategoryManager());
            searcherDao.setLangManager(this.getLangManager());
        } catch (Throwable t) {
            logger.error("Error creating new searcher", t);
            throw new EntException("Error creating new searcher", t);
        }
        return searcherDao;
        */
        return this.getSearcher();
    }

    @Override
    public void updateSubDir(String newSubDirectory) throws EntException {
        // nothing to do
    }
/*
    private File getDirectory(String subDirectory) throws EntException {
        String dirName = this.getIndexDiskRootFolder();
        if (!dirName.endsWith("/")) {
            dirName += "/";
        }
        dirName += "cmscontents/" + subDirectory;
        logger.debug("Index Directory: {}", dirName);
        File dir = new File(dirName);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
            logger.debug("Index Directory created");
        }
        if (!dir.canRead() || !dir.canWrite()) {
            throw new EntException(dirName + " does not have r/w rights");
        }
        return dir;
    }
*/
    @Override
    public void deleteSubDirectory(String subDirectory) {
        // nothing to do
    }
/*
    public String getIndexerClassName() {
        return indexerClassName;
    }

    public void setIndexerClassName(String indexerClassName) {
        this.indexerClassName = indexerClassName;
    }

    public String getSearcherClassName() {
        return searcherClassName;
    }

    public void setSearcherClassName(String searcherClassName) {
        this.searcherClassName = searcherClassName;
    }

    protected String getIndexDiskRootFolder() {
        return indexDiskRootFolder;
    }

    public void setIndexDiskRootFolder(String indexDiskRootFolder) {
        this.indexDiskRootFolder = indexDiskRootFolder;
    }
*/
    protected ConfigInterface getConfigManager() {
        return configManager;
    }

    public void setConfigManager(ConfigInterface configService) {
        this.configManager = configService;
    }

    protected ILangManager getLangManager() {
        return langManager;
    }

    public void setLangManager(ILangManager langManager) {
        this.langManager = langManager;
    }

    protected ICategoryManager getCategoryManager() {
        return categoryManager;
    }

    public void setCategoryManager(ICategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }

}
