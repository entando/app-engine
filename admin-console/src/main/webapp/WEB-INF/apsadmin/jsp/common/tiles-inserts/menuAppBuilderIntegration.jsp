<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wp" uri="/aps-core" %>
<%@ taglib prefix="wpsa" uri="/apsadmin-core" %>

<script>
    $(document).ready(function () {
        // matchHeight the contents of each .card-pf and then the .card-pf itself
        $(".row-cards-pf > [class*='col'] > .card-pf .card-pf-title").matchHeight();
        $(".row-cards-pf > [class*='col'] > .card-pf > .card-pf-body").matchHeight();
        $(".row-cards-pf > [class*='col'] > .card-pf > .card-pf-footer").matchHeight();
        $(".row-cards-pf > [class*='col'] > .card-pf").matchHeight();
        // Initialize the vertical navigation
        $().setupVerticalNavigation(true);
        $('[data-toggle=popover]').popovers();
//        $(".bootstrap-switch").bootstrapSwitch();
    });
</script>

<c:set var="current_languague" value="${not empty WW_TRANS_I18N_LOCALE ? WW_TRANS_I18N_LOCALE : pageContext.response.locale}" />

<script>
    // manage app-builder redux store to get the current language
    var reduxStore = JSON.parse(localStorage.getItem('redux'));
    var activeLocale = '<c:out value="${current_languague.language}" />'
    var reduxLocale = reduxStore && reduxStore.locale ? reduxStore.locale : activeLocale;

    // update redux store with activeLocale if `redux` doesn't exists in the localStorage
    if (!reduxStore) {
        updateLocalStorageWithLocale(activeLocale);
    }

    // refresh page with the new locale if necessary
    if (reduxLocale !== activeLocale) {
        var newUrl = window.location.href;
        var newParam = 'request_locale=' + reduxLocale;
        var paramIndex = newUrl.indexOf('request_locale=');
        if (paramIndex >= 0) {
            // if the param already exist, replace it with new value
            var toRemove = newUrl.slice(paramIndex, paramIndex+17);
            newUrl = newUrl.replace(toRemove, newParam);
            window.location.href = newUrl;
        } else {
            // else only update the url with new param
            var params = window.location.search;
            window.location.search += params.length ? '&' + newParam : newParam;
        }
    }

    function updateLocalStorageWithLocale(locale) {
        // get most updated redux store
        var store = JSON.parse(localStorage.getItem('redux')) || {};
        store.locale = locale;
        localStorage.setItem('redux', JSON.stringify(store));
    }
</script>

<wp:ifauthorized permission="superuser" var="isSuperUser" />
<s:set var="appBuilderBaseURL" ><wp:info key="systemParam" paramName="appBuilderBaseURL" /></s:set>
<s:set var="isHideBundlesMenuEntries" value="%{hideBundlesMenuEntries}" />

<ul class="list-group">
    <li class="list-group-item secondary-nav-item-pf">
        <a href='<c:out value="${appBuilderBaseURL}"/>dashboard' class="no-chevron">
            <span class="icons-svg-new svg-dashboard" data-toggle="tooltip" title="<s:text name="menu.dashboard" />"><em class="dashboard"></em></span>
            <span class="list-group-item-value"><s:text name="menu.dashboard" /></span>
        </a>
    </li>
    <wp:ifauthorized permission="managePages" var="isManagePage" />
    <c:if test="${isManagePage || isSuperUser}">
        <!-- Page Designer -->
        <li class="list-group-item secondary-nav-item-pf" data-target="#page-designer-secondary">

            <a>
                <span class="icons-svg-new svg-pages" data-toggle="tooltip" title="<s:text name="menu.pageDesigner" />" ></span>
                <span class="list-group-item-value"><s:text name="menu.pageDesigner" /></span>
            </a>

            <div id="page-designer-secondary" class="nav-pf-secondary-nav">
                <div class="nav-item-pf-header">
                    <a class="secondary-collapse-toggle-pf" data-toggle="collapse-secondary-nav"></a>
                    <span><s:text name="menu.pageDesigner" /></span>
                </div>

                <!-- Page Designer Secondary -->
                <ul class="list-group">
                    <li class="list-group-item">
                        <a id="linkHome" href='<c:out value="${appBuilderBaseURL}"/>page'>
                            <span class="list-group-item-value"><s:text name="menu.pageDesigner.pageTree" /></span>
                        </a>
                    </li>
                    <li class="list-group-item">
                        <a id="" href='<c:out value="${appBuilderBaseURL}"/>page/configuration/homepage'>
                            <span class="list-group-item-value"><s:text name="menu.pageDesigner.pageConfiguration" /></span>
                        </a>
                    </li>
                    <c:if test="${isSuperUser}">
                        <li class="list-group-item">
                            <a href='<c:out value="${appBuilderBaseURL}"/>page-template'>
                                <span class="list-group-item-value"><s:text name="menu.UXPattern.pageModels" /></span>
                            </a>
                        </li>

                        <li class="list-group-item">
                            <a href='<c:out value="${appBuilderBaseURL}"/>page/settings'>
                                <span class="list-group-item-value"><s:text name="menu.pageDesigner.pageSettings" /></span>
                            </a>
                        </li>
                    </c:if>
                </ul>
                <!--Fine Page Designer Secondary-->
            </div>
        </li>
    </c:if>

    <!-- UX Patterns -->
    <c:if test="${isSuperUser}">
        <li class="list-group-item secondary-nav-item-pf" data-target="#ux-pattern-secondary">
            <a>
                <span class="icons-svg-new svg-components" data-toggle="tooltip" title="<s:text name="menu.UXPattern" />"></span>
                <span class="list-group-item-value"><s:text name="menu.UXPattern" /></span>
            </a>

            <div id="ux-pattern-secondary" class="nav-pf-secondary-nav">
                <div class="nav-item-pf-header">
                    <a class="secondary-collapse-toggle-pf" data-toggle="collapse-secondary-nav"></a>
                    <span><s:text name="menu.UXPattern" /></span>
                </div>

                <!-- UX Patterns Secondary -->
                <ul class="list-group">
                    <li class="list-group-item">
                        <a href='<c:out value="${appBuilderBaseURL}"/>widget'>
                            <span class="list-group-item-value"><s:text name="menu.UXPattern.widget" /></span>
                        </a>
                    </li>
                    <li class="list-group-item">
                        <a href='<c:out value="${appBuilderBaseURL}"/>fragment'>
                            <span class="list-group-item-value"><s:text name="menu.UXPattern.fragments" /></span>
                        </a>
                    </li>
                </ul>
                <!--Fine UX Patterns Secondary-->
            </div>
        </li>
    </c:if>

    <!-- APPS -->
    <li class="list-group-item secondary-nav-item-pf" data-target="#apps-secondary">
        <a>
            <span class="icons-svg-new svg-content" data-toggle="tooltip" title="<s:text name="menu.APPS" />"></span>
            <span class="list-group-item-value"><s:text name="menu.APPS" /></span>
        </a>
        <!--Integrations secondary-->
        <div id="apps-secondary" class="nav-pf-secondary-nav">
            <div class="nav-item-pf-header">
                <a class="secondary-collapse-toggle-pf" data-toggle="collapse-secondary-nav"></a>
                <span><s:text name="menu.APPS" /></span>
            </div>
            <wpsa:hookPoint key="core.menu.apps" objectName="hookPointElements_core_menu_apps">
                <s:iterator value="#hookPointElements_core_menu_apps" var="hookPointElement">
                    <wpsa:include value="%{#hookPointElement.filePath}"></wpsa:include>
                </s:iterator>
            </wpsa:hookPoint>
        </div>
    </li>

    <wp:ifauthorized permission="viewUsers" var="isViewUsers" />
    <wp:ifauthorized permission="editUsers" var="isEditUsers" />
    <wp:ifauthorized permission="editUserProfile" var="isEditProfiles" />
    <c:if test="${isSuperUser || isViewUsers || isEditUsers || isEditProfiles}">
        <!--  Users Settings -->
        <li class="list-group-item secondary-nav-item-pf" data-target="#user-settings-secondary">
            <a>
            <span class="icons-svg-new svg-users" data-toggle="tooltip" title="<s:text name="menu.userSettings" />"></span>
            <span class="list-group-item-value"><s:text name="menu.userSettings" /></span>
        </a>
            <div id="#user-settings-secondary" class="nav-pf-secondary-nav">
                <div class="nav-item-pf-header">
                    <a class="secondary-collapse-toggle-pf" data-toggle="collapse-secondary-nav"></a>
                    <span><s:text name="menu.userSettings"/></span>
                </div>

                <!-- Users Settings Secondary -->

                <ul class="list-group">
                    <li class="list-group-item">
                        <a href='<c:out value="${appBuilderBaseURL}"/>user'>
                            <span class="list-group-item-value"><s:text name="menu.usersSettings.users" /></span>
                        </a>
                    </li>

                    <c:if test="${isSuperUser}">
                        <li class="list-group-item">
                            <a href='<c:out value="${appBuilderBaseURL}"/>role'>
                                <span class="list-group-item-value"><s:text name="menu.usersSettings.roles" /></span>
                            </a>
                        </li>

                        <li class="list-group-item">
                            <a href='<c:out value="${appBuilderBaseURL}"/>group'>
                                <span class="list-group-item-value"><s:text name="menu.settings.groups" /></span>
                            </a>
                        </li>

                        <li class="list-group-item">
                            <a href='<c:out value="${appBuilderBaseURL}"/>profiletype'>
                                <span class="list-group-item-value"><s:text name="menu.usersSettings.profileTypes" /></span>
                            </a>
                        </li>

                        <li class="list-group-item">
                            <a href='<c:out value="${appBuilderBaseURL}"/>user/restrictions'>
                                <span class="list-group-item-value"><s:text name="menu.usersSettings.usersRestriction" /></span>
                            </a>
                        </li>
                    </c:if>
                </ul>
                <!--Fine Users Settings Secondary-->
            </div>
        </li>
    </c:if>

    <!-- ECR -->
    <wp:ifauthorized permission="enterECR" var="isEnterECR" />
    <c:if test="${(isSuperUser || isEnterECR) && !isHideBundlesMenuEntries}">
        <li class="list-group-item secondary-nav-item-pf">
            <a href='<c:out value="${appBuilderBaseURL}"/>component-repository' class="no-chevron">
                <span class="icons-svg-new svg-hub" data-toggle="tooltip" title="<s:text name="menu.ECR" />"></span>
                <span class="list-group-item-value"><s:text name="menu.ECR" /></span>
            </a>
        </li>
    </c:if>

    <!-- EPC -->
    <c:if test="${!isHideBundlesMenuEntries}">
        <li class="list-group-item secondary-nav-item-pf menu-epc">
            <a href='<c:out value="${appBuilderBaseURL}"/>' onclick="window.sessionStorage.setItem('menu_open', 'epc')" class="no-chevron">
                <span class="icons-svg-new svg-emp" data-toggle="tooltip" title="<s:text name="menu.EPCs" />"></span>
                <span class="list-group-item-value"><s:text name="menu.EPCs" /></span>
            </a>
        </li>
    </c:if>
</ul>

<c:if test="${isSuperUser}">
    <ul class="list-group">

        <li class="list-group-item secondary-nav-item-pf" data-target="#settings-secondary">
            <a>
                <span class="icons-svg-new svg-setting" data-toggle="tooltip" title="<s:text name="menu.settings" />"></span>
                <span class="list-group-item-value"><s:text name="menu.settings" /></span>
            </a>

            <div id="#settings-secondary" class="nav-pf-secondary-nav">
                <div class="nav-item-pf-header">
                    <a class="secondary-collapse-toggle-pf" data-toggle="collapse-secondary-nav"></a>
                    <span><s:text name="menu.settings" /></span>
                </div>

                <!-- Settings Secondary -->

                <ul class="list-group">
                    <li class="list-group-item">
                        <a href='<c:out value="${appBuilderBaseURL}"/>database'>
                            <span class="list-group-item-value"><s:text name="menu.settings.database" /></span>
                        </a>
                    </li>

                    <li class="list-group-item">
                        <a href='<c:out value="${appBuilderBaseURL}"/>file-browser'>
                            <span class="list-group-item-value"><s:text name="menu.settings.fileBrowser" /></span>
                        </a>
                    </li>

                    <li class="list-group-item">
                        <a href='<c:out value="${appBuilderBaseURL}"/>labels-languages'>
                            <span class="list-group-item-value"><s:text name="menu.settings.labelsLanguages" /></span>
                        </a>
                    </li>

                    <li class="list-group-item">
                        <a href='<c:out value="${appBuilderBaseURL}"/>email-config'>
                            <span class="list-group-item-value"><s:text name="menu.settings.email" /></span>
                        </a>
                    </li>

                    <li class="list-group-item">
                        <a href='<c:out value="${appBuilderBaseURL}"/>reloadConfiguration'>
                            <span class="list-group-item-value"><s:text name="menu.settings.reloadConfigurations" /></span>
                        </a>
                    </li>
                </ul>
                <!--Fine Users Settings Secondary-->
            </div>

        </li>
    </ul>
</c:if>
