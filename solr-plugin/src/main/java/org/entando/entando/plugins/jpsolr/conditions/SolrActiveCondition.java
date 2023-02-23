package org.entando.entando.plugins.jpsolr.conditions;

import org.entando.entando.plugins.jpsolr.SolrEnvironmentVariables;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

public class SolrActiveCondition implements Condition {

    private final boolean envActive;

    public SolrActiveCondition() {
        this(SolrEnvironmentVariables.active());
    }

    protected SolrActiveCondition(boolean active) {
        this.envActive = active;
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        MultiValueMap<String, Object> attrs = metadata.getAllAnnotationAttributes(SolrActiveCondition.class.getName());
        boolean active = false;
        if (attrs != null) {
            active = (boolean) attrs.getFirst("value");
        }
        return active == this.envActive;
    }
}
