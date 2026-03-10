package com.agiletec.plugins.jpwebdynamicform.aps.internalservlet.message.helper;

import com.agiletec.aps.system.common.entity.model.AttributeFieldError;
import com.agiletec.aps.system.common.entity.model.AttributeTracer;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.services.i18n.II18nManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.apsadmin.system.BaseAction;
import com.agiletec.apsadmin.system.entity.EntityActionHelper;
import com.agiletec.apsadmin.system.entity.attribute.manager.AttributeManagerInterface;
import org.apache.struts2.ActionSupport;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging;

import java.util.List;
import java.util.Optional;

public class MessageActionHelper extends EntityActionHelper {

    private static final EntLogging.EntLogger _logger = EntLogging.EntLogFactory.getSanitizedLogger(MessageActionHelper.class);

    private II18nManager i18nManager;

    protected II18nManager getI18nManager() {
        return i18nManager;
    }

    public void setI18nManager(II18nManager i18nManager) {
        this.i18nManager = i18nManager;
    }

    @Override
    public void scanEntity(IApsEntity currentEntity, ActionSupport action) {
        try {
            String langCode = this.getCurrentLang(action);
            List<AttributeInterface> attributes = currentEntity.getAttributeList();
            for (int i = 0; i < attributes.size(); i++) {
                AttributeInterface entityAttribute = attributes.get(i);
                if (entityAttribute.isActive()) {
                    List<AttributeFieldError> errors = entityAttribute.validate(new AttributeTracer(), super.getLangManager(), this.getBeanFactory());
                    if (null != errors && !errors.isEmpty()) {
                        for (int j = 0; j < errors.size(); j++) {
                            AttributeFieldError attributeFieldError = errors.get(j);
                            AttributeTracer tracer = attributeFieldError.getTracer();
                            AttributeInterface attribute = attributeFieldError.getAttribute();
                            String messageAttributePositionPrefix = this.createErrorMessageAttributePositionPrefix(
                                    action, currentEntity, attribute, tracer, langCode);
                            AttributeManagerInterface attributeManager = this.getManager(attribute);
                            String errorMessage = attributeManager.getErrorMessage(attributeFieldError, action);
                            String formFieldName = tracer.getFormFieldName(attributeFieldError.getAttribute());
                            action.addFieldError(formFieldName, messageAttributePositionPrefix + " " + errorMessage);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            _logger.error("Error scanning Entity", t);
            action.addActionError(action.getText("Errors.genericError"));
        }
    }

    protected String createErrorMessageAttributePositionPrefix(
            ActionSupport action, IApsEntity currentEntity, AttributeInterface attribute,
            AttributeTracer tracer, String langCode) throws EntException {
        if (tracer.isMonoListElement()) {
            if (tracer.isCompositeElement()) {
                String[] args = {this.getAttributeName(currentEntity, tracer.getParentAttribute(), langCode),
                        String.valueOf(tracer.getListIndex() + 1),
                        this.getAttributeName(currentEntity, attribute, langCode)};
                return action.getText("EntityAttribute.compositeListAttributeElement.errorMessage.prefix", args);
            } else {
                String[] args = {this.getAttributeName(currentEntity, attribute, langCode),
                        String.valueOf(tracer.getListIndex() + 1)};
                return action.getText("EntityAttribute.monolistAttributeElement.errorMessage.prefix", args);
            }
        } else if (tracer.isCompositeElement()) {
            String[] args = {this.getAttributeName(currentEntity, tracer.getParentAttribute(), langCode),
                    this.getAttributeName(currentEntity, attribute, langCode)};
            return action.getText("EntityAttribute.compositeAttributeElement.errorMessage.prefix", args);
        } else if (tracer.isListElement()) {
            String[] args = {this.getAttributeName(currentEntity, attribute, langCode), tracer.getListLang().getDescr(),
                    String.valueOf(tracer.getListIndex() + 1)};
            return action.getText("EntityAttribute.listAttributeElement.errorMessage.prefix", args);
        } else {
            String[] args = {this.getAttributeName(currentEntity, attribute, langCode)};
            return action.getText("EntityAttribute.singleAttribute.errorMessage.prefix", args);
        }
    }

    protected String getAttributeName(IApsEntity currentEntity, AttributeInterface attribute, String langCode) throws EntException {
        String labelKey = "jpwebdynamicform_" + currentEntity.getTypeCode() + "_" + attribute.getName();
        return this.getI18nManager().getLabel(labelKey, langCode);
    }

    protected String getCurrentLang(ActionSupport action) {
        return Optional.ofNullable(action)
                .filter(a -> a instanceof BaseAction)
                .map(a -> ((BaseAction) a).getCurrentLang())
                .map(Lang::getCode)
                .orElseGet(() -> this.getLangManager().getDefaultLang().getCode());
    }

}
