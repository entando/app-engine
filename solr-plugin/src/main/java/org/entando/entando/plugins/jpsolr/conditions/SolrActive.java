package org.entando.entando.plugins.jpsolr.conditions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Conditional;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Conditional(SolrActiveCondition.class)
public @interface SolrActive {

    boolean value();
}
