/*
 * Copyright 2017-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.entando.entando.plugins.jpcontentscheduler.aps.system.services.content;

import com.agiletec.aps.system.ApsSystemUtils;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.AbstractService;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.common.entity.model.attribute.ITextAttribute;
import com.agiletec.aps.system.exception.ApsSystemException;
import com.agiletec.aps.system.services.authorization.IApsAuthority;
import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.baseconfig.SystemParamsUtils;
import com.agiletec.aps.system.services.keygenerator.IKeyGeneratorManager;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.cache.CmsCacheWrapperManager;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentSearcherDAO;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jpmail.aps.services.mail.IMailManager;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.entando.entando.aps.system.services.cache.ICacheInfoManager;
import org.entando.entando.aps.system.services.tenants.RefreshableBeanTenantAware;
import org.entando.entando.aps.system.services.userprofile.model.UserProfile;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.plugins.jpcontentscheduler.aps.system.services.ContentThreadConstants;
import org.entando.entando.plugins.jpcontentscheduler.aps.system.services.content.model.ContentState;
import org.entando.entando.plugins.jpcontentscheduler.aps.system.services.content.model.ContentSuspendMove;
import org.entando.entando.plugins.jpcontentscheduler.aps.system.services.content.model.ContentThreadConfig;
import org.entando.entando.plugins.jpcontentscheduler.aps.system.services.content.model.ContentTypeElem;
import org.entando.entando.plugins.jpcontentscheduler.aps.system.services.content.parse.ContentThreadConfigDOM;
import org.entando.entando.plugins.jpcontentscheduler.aps.system.services.content.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;

/**
 * Classe che implementa i servizi da necessari al thread di
 * pubblicazione/sospenzione automatica
 */
public class ContentSchedulerManager extends AbstractService implements IContentSchedulerManager,
        RefreshableBeanTenantAware {

    private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(ContentSchedulerManager.class);
    private static final long serialVersionUID = 6880576602469119814L;

    private IContentSearcherDAO _workContentSearcherDAO;

    private ConfigInterface configManager;
    private ContentThreadConfig config;
    private IAuthorizationManager _authorizationManager;
    private IUserManager _userManager;
    private IContentSchedulerDAO _contentSchedulerDAO;

    private IMailManager _mailManager;
    private transient IKeyGeneratorManager keyGeneratorManager;
    
    private transient ICacheInfoManager cacheInfoManager;

    @Override
    public void init() throws Exception {
        this.loadConfigs();
        _logger.info("'{}': initialized", this.getClass().getName());
    }

    @Override
    public void initTenantAware() throws Exception {
        this.loadConfigs();
    }

    /**
     * Carico la configurazione impostata nella tabella sysconfig di japsport
     * (parametro 'cthread_config')
     */
    private void loadConfigs() throws EntException {
        try {
            String xml = this.getConfigManager().getConfigItem(ContentThreadConstants.CONTENTTHREAD_CONFIG_ITEM);
            if (xml == null) {
                throw new EntException(ContentThreadConstants.READ_CONFIG_ERROR + ContentThreadConstants.CONTENTTHREAD_CONFIG_ITEM);
            }
            ContentThreadConfigDOM configDOM = new ContentThreadConfigDOM();
            this.setConfig(configDOM.extractConfig(xml));
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "loadConfigs");
            throw new EntException(ContentThreadConstants.INIT_ERROR, t);
        }
    }

    @Override
    public void updateConfig(ContentThreadConfig config) throws EntException {
        try {
            String xml = new ContentThreadConfigDOM().createConfigXml(config);
            this.getConfigManager().updateConfigItem(ContentThreadConstants.CONTENTTHREAD_CONFIG_ITEM, xml);
            this.setConfig(config);
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "loadConfigs");
            throw new EntException("error updating configuration", t);
        }

    }

    /**
     * Restituisce tutti i contenuti che hanno un attributo con nome key
     * Data_inizio e valore la data corrente
     *
     * @throws EntException
     */
    @Override
    public List<String> getContentIdToPublish() throws EntException {
        List<String> ans = new ArrayList<String>();
        try {
            for (Iterator<ContentTypeElem> i = this.getConfig().getTypesList().iterator(); i.hasNext();) {
                ContentTypeElem elem = i.next();
                String filterKey = elem.getStartDateAttr();
                if (null != filterKey) {
                    Calendar cal = Calendar.getInstance();
                    // cal.setTime(new Date());
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    Date start = cal.getTime();
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    Date end = cal.getTime();
                    EntitySearchFilter[] filters = new EntitySearchFilter[]{new EntitySearchFilter(filterKey, true, start, end)};
                    List<String> retrieved = this.getWorkContentSearcherDAO().searchId(elem.getContentType(), filters);
                    ans.addAll(retrieved);
                }
            }
        } catch (Throwable t) {
            _logger.error(ContentThreadConstants.GET_CONTENTIDS_ERROR, t);
            throw new EntException(ContentThreadConstants.GET_CONTENTIDS_ERROR, t);
        }
        return ans;
    }

    /**
     * Restituisce tutti i contenuti che hanno un attributo con nome key
     * Data_fine e valore la data corrente
     *
     * @throws EntException
     */
    @Override
    public List<ContentSuspendMove> getContentAttrDataFine() throws EntException {
        List<ContentSuspendMove> ans = new ArrayList<>();
        try {
            for (Iterator<ContentTypeElem> i = this.config.getTypesList().iterator(); i.hasNext();) {
                ContentTypeElem elem = i.next();
                String filterKey = elem.getEndDateAttro();
                if (null != filterKey) {
                    Date actualDate = new Date();
                    // Data fine compresa nella settimana
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(actualDate);
                    cal.add(Calendar.DATE, -7);
                    Date beginDate = cal.getTime();
                    EntitySearchFilter blobFilter = new EntitySearchFilter(IContentManager.CONTENT_ONLINE_FILTER_KEY, false, null, false);
                    EntitySearchFilter[] filters = new EntitySearchFilter[]{new EntitySearchFilter(filterKey, true, beginDate, new Date()), blobFilter};
                    List<String> retrieved = this.getWorkContentSearcherDAO().searchId(elem.getContentType(), filters);
                    ContentSuspendMove contSuspMov = new ContentSuspendMove(elem.getSuspend(),
                            retrieved,
                            elem.getContentType(),
                            this.getConfig().getGlobalCat(),
                            elem.getIdsCategories(),
                            elem.getIdContentReplace(),
                            elem.getModelIdContentReplace());
                    contSuspMov.setContentIdReplace(this.getConfig().getContentIdRepl());
                    contSuspMov.setContentModelReplace(this.getConfig().getContentModelRepl());
                    ans.add(contSuspMov);
                }
            }
        } catch (Throwable t) {
            _logger.error(ContentThreadConstants.GET_CONTENTIDS_ERROR, t);
            throw new EntException(ContentThreadConstants.GET_CONTENTIDS_ERROR, t);
        }
        return ans;
    }

    /**
     * Metodo per spedire le mail come impostato nella configurazione
     *
     * @param publishedContents
     * @param suspendedContents
     * @throws EntException
     */
    @Override
    public void sendMailWithResults(List<ContentState> publishedContents, List<ContentState> suspendedContents, List<ContentState> movedContents, Date startJobDate,
            Date endJobDate) throws EntException {
        // TODO send to groups
        // sendToGroups(publishedContents, suspendedContents);
        sendToUsers(publishedContents, suspendedContents, movedContents, startJobDate, endJobDate);
    }

    /**
     * Spedisco agli utenti specificati nella configurazione
     *
     * @param publishedContents
     * @param suspendedContents
     * @param startJobDate
     * @param endJobDate
     * @throws EntException
     */
    private void sendToUsers(List<ContentState> publishedContents, List<ContentState> suspendedContents, List<ContentState> moveContents, Date startJobDate, Date endJobDate)
            throws EntException {
        Map<String, List<String>> mapUsers = this.getConfig().getUsersContentType();
        Set<String> keys = mapUsers.keySet();
        for (String key : keys) {
            List<String> typesList = mapUsers.get(key);
            List<ContentState> contentPList = contentOfTypes(publishedContents, typesList);
            List<ContentState> contentSList = contentOfTypes(suspendedContents, typesList);
            List<ContentState> contentMList = contentOfTypes(moveContents, typesList);
            if ((contentPList != null && contentPList.size() > 0) || (contentSList != null && contentSList.size() > 0) || (contentMList != null && contentMList.size() > 0)) {
                UserDetails user = this.getUserManager().getUser(key);
                if (user == null) {
                    _logger.error(ContentThreadConstants.USER_IS_NULL + "{}", key);
                    continue;
                } else {
                    UserProfile profile = (UserProfile) user.getProfile();
                    if (null != profile) {
                        ITextAttribute mailAttribute = (ITextAttribute) profile.getAttributeByRole(SystemConstants.USER_PROFILE_ATTRIBUTE_ROLE_MAIL);
                        String[] email = new String[1];
                        if (null != mailAttribute && mailAttribute.getText().trim().length() > 0) {
                            email[0] = mailAttribute.getText();
                            String simpleText = Utils.prepareMailText(contentPList, contentSList, contentMList, this.getConfig(), startJobDate, endJobDate);
                            boolean issent = false;
                            if (this.getConfig().isAlsoHtml()) {
                                String applBaseUrl = this.getConfigManager().getParam(SystemConstants.PAR_APPL_BASE_URL);
                                String htmlText = Utils.prepareMailHtml(contentPList, contentSList, contentMList, this.getConfig(), startJobDate, endJobDate, applBaseUrl);
                                issent = this.getMailManager().sendMixedMail(simpleText, htmlText, config.getSubject(), null, email, null, null, config.getSenderCode());
                            } else {
                                issent = this.getMailManager().sendMail(simpleText, config.getSubject(), email, null, null, config.getSenderCode());
                            }
                            if (issent) {
                                _logger.info(ContentThreadConstants.MAIL_SENT + "{}", key);
                            } else {
                                _logger.error(ContentThreadConstants.SEND_ERROR + "{}", key);
                            }
                        }
                    } else {
                        _logger.error(ContentThreadConstants.PROFILE_IS_NULL + "{}", key);
                    }
                }
            }
        }
    }

    /**
     * Dato un array di contenuti, restituisce tutti quelli di tipo specificato
     * nell'array types. Serve per sapere quali contenuti un gruppo o un utente
     * deve sapere la pubblicazione o sospensione
     *
     * @param contentList
     * @param types
     * @return
     */
    private List<ContentState> contentOfTypes(List<ContentState> contentList, List<String> types) {
        List<ContentState> ans = new ArrayList<>();
        for (Iterator<ContentState> itContent = contentList.iterator(); itContent.hasNext();) {
            ContentState currElem = itContent.next();
            String currType = currElem.getType();
            if ((types != null && types.size() == 1 && types.get(0).equals(ContentThreadConstants.ALL_TYPES))) {
                ans.add(currElem);
            } else {
                for (Iterator<String> itTypes = types.iterator(); itTypes.hasNext();) {
                    if (itTypes.next().equals(currType)) {
                        ans.add(currElem);
                    }
                }
            }
        }
        return ans;
    }

    /**
     * NON USATO metodo ausiliario a getAuthorizedUsers
     *
     * @param groupName
     * @return
     */
    public IApsAuthority getApsAuthority(String groupName) {
        IApsAuthority authority = null;
        // this.getAuthorizatorManager().getAuthority(groupName);
        return authority;
    }

    /**
     * Restituisce la lista degli utenti associati ad un determinato gruppo
     *
     * @param groupName
     * @return
     */
    public List<UserDetails> getAuthorizedUsers(String groupName) {
        // IApsAuthority auth = this.getApsAuthority(groupName);
        List<UserDetails> users = null;
        try {
            List<String> usernames = this.getAuthorizationManager().getUsersByGroup(groupName, false);
            if (usernames.size() > 0) {
                users = new ArrayList<>();
                for (int i = 0; i < usernames.size(); i++) {
                    UserDetails u = this.getUserManager().getUser(usernames.get(i));
                    users.add(u);
                }
            }

            return users; // this.getAuthorizatorManager().getUsersByAuthority(auth);
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "getUserAuthorizated");
            throw new RuntimeException("Errore in ricerca utenti autorizzati", t);
        }
    }

    /**
     * Return the desired system parameter
     *
     * @param paramName
     * @return
     */
    @Override
    public String getSystemParam(String paramName) {
        String param = "";
        try {
            String xmlParams = this.getConfigManager().getConfigItem(SystemConstants.CONFIG_ITEM_PARAMS);
            Map<String, String> systemParams = SystemParamsUtils.getParams(xmlParams);
            param = systemParams.get(paramName);
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "error getting the system parameter " + paramName);
        }
        return param;
    }

    protected IContentSchedulerDAO getContentSchedulerDAO() {
        return _contentSchedulerDAO;
    }

    public void setContentSchedulerDAO(IContentSchedulerDAO contentSchedulerDAO) {
        this._contentSchedulerDAO = contentSchedulerDAO;
    }

    public IUserManager getUserManager() {
        return _userManager;
    }

    public void setUserManager(IUserManager userManager) {
        this._userManager = userManager;
    }

    protected IContentSearcherDAO getWorkContentSearcherDAO() {
        return _workContentSearcherDAO;
    }

    public void setWorkContentSearcherDAO(IContentSearcherDAO workContentSearcherDAO) {
        this._workContentSearcherDAO = workContentSearcherDAO;
    }

    public IMailManager getMailManager() {
        return _mailManager;
    }

    public void setMailManager(IMailManager mailManager) {
        this._mailManager = mailManager;
    }

    public IKeyGeneratorManager getKeyGeneratorManager() {
        return this.keyGeneratorManager;
    }

    public void setKeyGeneratorManager(IKeyGeneratorManager keyGeneratorManager) {
        this.keyGeneratorManager = keyGeneratorManager;
    }

    @Override
    public ContentThreadConfig getConfig() {
        return config;
    }

    public void setConfig(ContentThreadConfig config) {
        this.config = config;
    }

    public ConfigInterface getConfigManager() {
        return configManager;
    }

    public void setConfigManager(ConfigInterface configManager) {
        this.configManager = configManager;
    }

    public IAuthorizationManager getAuthorizationManager() {
        return _authorizationManager;
    }

    public void setAuthorizationManager(IAuthorizationManager authorizationManager) {
        this._authorizationManager = authorizationManager;
    }

    /**
     * Only for ONE SHOT Procedure
     *
     * @param content
     * @param updateDate
     * @throws EntException
     */
    @Override
    public void moveOnLineContent(Content content, boolean updateDate, boolean updateLastModified) throws EntException {
        try {
            if (updateLastModified) {
                content.setLastModified(new Date());
            }
            if (updateDate) {
                content.incrementVersion(false);
            }
            if (null == content.getId()) {
                int key = keyGeneratorManager.getUniqueKeyCurrentValue();
                String id = content.getTypeCode() + key;
                content.setId(id);
                this.getContentSchedulerDAO().addEntity(content);
            } else {
                this.getContentSchedulerDAO().updateContent(content, updateDate);
                this.getContentSchedulerDAO().publishContent(content);
            }
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "moveOnLineContent");
            throw new EntException("Error while move content on line", t);
        }
    }

    /**
     * Unpublish a content, preventing it from being displayed in the portal.
     * Obviously the content itself is not deleted.
     *
     * @param content the content to unpublish.
     * @param updateLastModified .
     * @throws EntException in case of error
     */
    @Override
    @CacheEvict(value = ICacheInfoManager.DEFAULT_CACHE_NAME, key = "'" + JacmsSystemConstants.CONTENT_CACHE_PREFIX + "'.concat(#content.id)", condition = "#content.id != null")
    public void removeOnLineContent(Content content, boolean updateLastModified) throws EntException {
        try {
            if (updateLastModified) {
                content.setLastModified(new Date());
            }
            content.incrementVersion(false);
            if (null != content.getStatus() && content.getStatus().equals(Content.STATUS_PUBLIC)) {
                content.setStatus(Content.STATUS_READY);
            }
            this.getContentSchedulerDAO().unpublishOnLineContent(content);
            // this.notifyPublicContentChanging(content,
            // PublicContentChangedEvent.REMOVE_OPERATION_CODE);
            this.flushGroups(content.getId(), content.getTypeCode());
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "removeOnLineContent");
            throw new EntException("Error while removing onLine content", t);
        }
    }

    private void flushGroups(String contentId, String typeCode) {
        String[] groups = (null != typeCode) ? CmsCacheWrapperManager.getContentCacheGroupsToEvict(contentId, typeCode) : CmsCacheWrapperManager.getContentCacheGroupsToEvict(contentId);
        for (int i = 0; i < groups.length; i++) {
            String groupCode = groups[i];
            this.getCacheInfoManager().flushGroup(ICacheInfoManager.DEFAULT_CACHE_NAME, groupCode);
        }
    }

    protected ICacheInfoManager getCacheInfoManager() {
        return this.cacheInfoManager;
    }
    @Autowired
    public void setCacheInfoManager(ICacheInfoManager cacheInfoManager) {
        this.cacheInfoManager = cacheInfoManager;
    }

}
