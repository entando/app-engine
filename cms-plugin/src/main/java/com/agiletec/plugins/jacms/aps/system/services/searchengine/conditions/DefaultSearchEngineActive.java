package com.agiletec.plugins.jacms.aps.system.services.searchengine.conditions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Conditional;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Conditional(DefaultSearchEngineActiveCondition.class)
public @interface DefaultSearchEngineActive {

    boolean value();
}
