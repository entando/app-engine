package org.entando.entando.apsadmin.system;

import java.util.Collections;
import java.util.List;

public class RequestLoggingInterceptorConfig {

    private List<Exclusion> exclusions = Collections.emptyList();

    public boolean isExcluded(String requestUri, String parameterName) {
        for (Exclusion e : exclusions) {
            if (e.matches(requestUri, parameterName)) {
                return true;
            }
        }
        return false;
    }

    public List<Exclusion> getExclusions() {
        return exclusions;
    }

    public void setExclusions(List<Exclusion> exclusions) {
        this.exclusions = exclusions;
    }

    public static class Exclusion {

        private String actionPath;       // null/empty = applies to all actions
        private List<String> parameters; // required: one or more parameter names

        public boolean matchesAction(String requestUri) {
            return actionPath == null || actionPath.isEmpty() || requestUri.endsWith(actionPath);
        }

        public boolean matches(String requestUri, String param) {
            if (parameters == null || parameters.isEmpty() || !parameters.contains(param)) {
                return false;
            }
            return matchesAction(requestUri);
        }

        public String getActionPath() {
            return actionPath;
        }

        public void setActionPath(String actionPath) {
            this.actionPath = actionPath;
        }

        public List<String> getParameters() {
            return parameters;
        }

        public void setParameters(List<String> parameters) {
            this.parameters = parameters;
        }
    }
}
