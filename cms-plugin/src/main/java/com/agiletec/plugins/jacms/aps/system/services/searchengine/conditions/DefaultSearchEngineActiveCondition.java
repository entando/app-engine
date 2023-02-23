package com.agiletec.plugins.jacms.aps.system.services.searchengine.conditions;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

public class DefaultSearchEngineActiveCondition implements Condition {

    private static final String SOLR_ACTIVE = "SOLR_ACTIVE";

    private final boolean envActive;

    public DefaultSearchEngineActiveCondition() {
        this(!Boolean.toString(true).equals(System.getenv(SOLR_ACTIVE)));
    }

    protected DefaultSearchEngineActiveCondition(boolean active) {
        this.envActive = active;
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        MultiValueMap<String, Object> attrs = metadata.getAllAnnotationAttributes(
                DefaultSearchEngineActive.class.getName());
        boolean active = false;
        if (attrs != null) {
            active = (boolean) attrs.getFirst("value");
        }
        return active == this.envActive;
    }

}
