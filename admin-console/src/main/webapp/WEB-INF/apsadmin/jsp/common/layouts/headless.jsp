<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="wp" uri="/aps-core" %>
<!DOCTYPE html>
<html lang="en-us">
    <head>
        <title>Entando - <s:set var="documentTitle"><tiles:getAsString name="title"/></s:set><s:property value="%{getText(#documentTitle)}" escapeHtml="false" /></title>
        <meta charset="UTF-8">
        <meta http-equiv="X-UA-Compatible" content="IE=Edge">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <jsp:include page="/WEB-INF/apsadmin/jsp/common/inc/header-include.jsp" />
        <tiles:insertAttribute name="extraResources"/>
        <style>
            body { margin: 0; padding: 16px; overflow: hidden; }
            /* hide Struts breadcrumbs and page title */
            .breadcrumb,
            h1.page-title-container,
            h1.page-title-container + hr,
            #main > .button-bar:first-child {
                display: none;
            }
        </style>
    </head>
    <body>
        <tiles:insertAttribute name="body"/>
        <script nonce="<wp:cspNonce />">
            (function() {
                // Inject entandoHeadless=true into all forms so it survives POST submissions
                document.querySelectorAll('form').forEach(function(form) {
                    if (!form.querySelector('input[name="entandoHeadless"]')) {
                        var input = document.createElement('input');
                        input.type = 'hidden';
                        input.name = 'entandoHeadless';
                        input.value = 'true';
                        form.appendChild(input);
                    }
                });
            })();
            (function() {
                function postHeight() {
                    window.parent.postMessage({
                        type: 'entando.legacyConfigResize',
                        height: document.body.scrollHeight
                    }, '*');
                }
                // on load
                postHeight();
                // on DOM changes (accordions, AJAX)
                new MutationObserver(postHeight).observe(document.body, {
                    childList: true, subtree: true, attributes: true
                });
                // fallback: poll for resize
                window.addEventListener('resize', postHeight);
            })();
        </script>
    </body>
</html>