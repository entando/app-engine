package org.entando.entando.plugins.jpsolr.apsadmin.tags;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.TagSupport;
import org.entando.entando.aps.system.services.searchengine.SolrEnvironmentVariables;

/**
 * Includes the content only if Solr is active.
 */
public class IfSolrActiveTag extends TagSupport {

    @Override
    public int doStartTag() throws JspException {
        if (SolrEnvironmentVariables.active()) {
            return EVAL_BODY_INCLUDE;
        }
        return SKIP_BODY;
    }
}
