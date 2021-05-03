/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.entando.entando.plugins.jpsolr.aps.system.solr;

import com.agiletec.aps.system.common.IManager;
import com.agiletec.aps.system.common.entity.event.EntityTypesChangingEvent;
import com.agiletec.aps.system.common.entity.event.EntityTypesChangingObserver;
import com.agiletec.aps.system.common.entity.model.SmallEntityType;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.DateAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.NumberAttribute;
import com.agiletec.aps.system.common.searchengine.IndexableAttributeInterface;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.plugins.jacms.aps.system.services.content.event.PublicContentChangedObserver;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author E.Santoboni
 */
public class SearchEngineManager extends com.agiletec.plugins.jacms.aps.system.services.searchengine.SearchEngineManager 
        implements ISolrSearchEngineManager, PublicContentChangedObserver, EntityTypesChangingObserver {
    
    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(SearchEngineManager.class);
    
    @Autowired
    private ILangManager langManager;
    /*
    @Override
    public List<Map<String, Object>> getFields() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean addField(Map<String, Object> properties) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean updateField(Map<String, Object> properties) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean deleteField(String fieldName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    */
    @Override
    public boolean refreshCmsFields() {
        List<SmallEntityType> entityTypes = this.getContentManager().getSmallEntityTypes();
        List<String> checkedFields = new ArrayList<>();
        for (int i = 0; i < entityTypes.size(); i++) {
            if (i == 0) {
                this.checkField(checkedFields, SolrFields.SOLR_CONTENT_ID_FIELD_NAME, "string");
                this.checkField(checkedFields, SolrFields.SOLR_CONTENT_TYPE_FIELD_NAME, "text_general");
                this.checkField(checkedFields, SolrFields.SOLR_CONTENT_GROUP_FIELD_NAME, "text_general", true);
                this.checkField(checkedFields, SolrFields.SOLR_CONTENT_DESCRIPTION_FIELD_NAME, "text_gen_sort");
                this.checkField(checkedFields, SolrFields.SOLR_CONTENT_MAIN_GROUP_FIELD_NAME, "text_general");
                this.checkField(checkedFields, SolrFields.SOLR_CONTENT_CATEGORY_FIELD_NAME, "text_general", true);
            }
            SmallEntityType entityType = entityTypes.get(i);
            this.refreshEntityType(checkedFields, entityType.getCode());
        }
        return true;
    }
    
    protected void refreshEntityType(List<String> checkedFields, String entityTypeCode) {
        Content prototype = this.getContentManager().createContentType(entityTypeCode);
        Iterator<AttributeInterface> iterAttribute = prototype.getAttributeList().iterator();
        while (iterAttribute.hasNext()) {
            AttributeInterface currentAttribute = iterAttribute.next();
            Object value = currentAttribute.getValue();
            if (null == value) {
                continue;
            }
            List<Lang> langs = this.getLangManager().getLangs();
            for (int j = 0; j < langs.size(); j++) {
                Lang currentLang = (Lang) langs.get(j);
                this.checkAttribute(checkedFields, currentAttribute, currentLang);
            }
        }
    }

    private void checkAttribute(List<String> checkedFields, AttributeInterface attribute, Lang lang) {
        attribute.setRenderingLang(lang.getCode());
        if (attribute instanceof IndexableAttributeInterface
                || ((attribute instanceof DateAttribute || attribute instanceof NumberAttribute) && attribute.isSearchable())) {
            String type = null;
            if (attribute instanceof DateAttribute) {
                type = "pdates";
            } else if (attribute instanceof NumberAttribute) {
                type = "plongs";
            } else if (attribute instanceof BooleanAttribute) {
                type = "boolean";
            } else {
                type = "text_gen_sort";
            }
            String fieldName = lang.getCode().toLowerCase() + "_" + attribute.getName();
            fieldName = fieldName.replaceAll(":", "_");
            this.checkField(checkedFields, fieldName, type);
            if (null == attribute.getRoles()) {
                return;
            }
            for (int i = 0; i < attribute.getRoles().length; i++) {
                String roleFieldName = lang.getCode().toLowerCase() + "_" + attribute.getRoles()[i];
                roleFieldName = roleFieldName.replaceAll(":", "_");
                this.checkField(null, roleFieldName, type);
            }
        }
    }
    
    private void checkField(List<String> checkedFields, String fieldName, String type) {
        this.checkField(checkedFields, fieldName, type, false);
    }
    
    private void checkField(List<String> checkedFields, String fieldName, String type, boolean multiValue) {
        if (null != checkedFields && checkedFields.contains(fieldName)) {
            logger.warn("** field '" + fieldName + "' already checked **");
            return;
        }
        List<Map<String, Object>> fields = ((ISolrSearchEngineDAOFactory) this.getFactory()).getFields();
        Map<String, Object> currentField = fields.stream().filter(f -> f.get("name").equals(fieldName)).findFirst().orElse(null);
        Map<String, Object> newField = new HashMap<>();
        newField.put("name", fieldName);
        newField.put("type", type);
        newField.put("multiValued", multiValue);
        if (null == currentField) {
            ((ISolrSearchEngineDAOFactory) this.getFactory()).addField(newField);
        } else if (!type.equals(currentField.get("type")) || !Boolean.valueOf(multiValue).equals((Boolean) currentField.get("multiValued"))) {
            ((ISolrSearchEngineDAOFactory) this.getFactory()).replaceField(newField);
        }
        if (null != checkedFields) {
            checkedFields.add(fieldName);
        }
    }
    
    @Override
    public void deleteIndexedEntity(String entityId) throws EntException {
        try {
            this.getIndexerDao().delete(SolrFields.SOLR_CONTENT_ID_FIELD_NAME, entityId);
        } catch (EntException e) {
            logger.error("Error deleting content {} from index", entityId, e);
            throw e;
        }
    }
    
    @Override
    public void updateFromEntityTypesChanging(EntityTypesChangingEvent event) {
        super.updateFromEntityTypesChanging(event);
        if (((IManager) this.getContentManager()).getName().equals(event.getEntityManagerName())) {
            String typeCode = event.getNewEntityType().getTypeCode();
            this.refreshEntityType(new ArrayList<>(), typeCode);
        }
    }

    protected ILangManager getLangManager() {
        return langManager;
    }
    public void setLangManager(ILangManager langManager) {
        this.langManager = langManager;
    }
    
}
