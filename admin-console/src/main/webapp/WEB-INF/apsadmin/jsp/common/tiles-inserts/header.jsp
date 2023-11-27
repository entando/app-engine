<%@ taglib prefix="wp" uri="/aps-core" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix = "fn" uri = "http://java.sun.com/jsp/jstl/functions" %>

<script>
    var Entando = Entando || {};
    Entando.backoffice = Entando.backoffice || {};
    Entando.backoffice.stream = {};
    Entando.backoffice.stream.list = {};
    Entando.backoffice.stream.list.updateUrl = '<s:url action="update" namespace="/do/ActivityStream" />';
    Entando.backoffice.stream.list.loadMoreUrl = '<s:url action="viewMore" namespace="/do/ActivityStream" />';
    Entando.backoffice.stream.comments = {};
    Entando.backoffice.stream.comments.addUrl = '<s:url action="addComment" namespace="/do/ActivityStream" />';
    Entando.backoffice.stream.comments.deleteUrl = '<s:url action="removeComment" namespace="/do/ActivityStream" />';
</script>
<script>
    jQuery(function () {
        $('#activity-stream [data-toggle="tooltip"]').tooltip({trigger: 'hover'});
        <s:set var="appBuilderIntegrationEnabled" ><wp:info key="systemParam" paramName="appBuilderIntegrationEnabled" /></s:set>
		<s:if test="#appBuilderIntegrationEnabled == 'true'">
        // trigger updateLocale if language is changed
        $('#languageDropdownComponent a').click(function(e) {
            e.preventDefault();
            if (updateLocalStorageWithLocale) {
                updateLocalStorageWithLocale($(this).data('locale'));
            }
            window.location.href = $(this).attr('href');
        });
        </s:if>
    })
</script>


<s:set var="appBuilderIntegrationEnabled" ><wp:info key="systemParam" paramName="appBuilderIntegrationEnabled" /></s:set>
<s:set var="appBuilderBaseURL" ><wp:info key="systemParam" paramName="appBuilderBaseURL" /></s:set>
<s:set var="version" ><wp:info key="systemParam" paramName="version" /></s:set>
<c:set var = "appBuilderVersion"><c:out value="${version.replaceAll('.([^.]+)$', '')}"/></c:set>

<div class="navbar-header">
    <button type="button" class="navbar-toggle">
        <span class="sr-only">Toggle navigation</span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
    </button>    
    <s:if test="#appBuilderIntegrationEnabled == 'true'">
        <a href='<c:out value="${appBuilderBaseURL}"/>dashboard' class="navbar-brand">
            <img class="navbar-brand-icon logo-entando" src="<wp:resourceURL ignoreTenant="true" />administration/img/entando-logo-white.svg" alt="Entando <c:out value="${appBuilderVersion}" />" />
        </a>
    </s:if>
    <s:else>
        <a href="<s:url action="main" namespace="/do" />" class="navbar-brand">
            <img class="navbar-brand-icon logo-entando" src="<wp:resourceURL ignoreTenant="true" />administration/img/entando-logo-white.svg" alt="Entando <wp:info key="systemParam" paramName="version" />" />
        </a>
    </s:else>
</div>
<nav class="collapse navbar-collapse">
    <ul class="nav navbar-nav navbar-right navbar-iconic">
        <li id="languageDropdown" class="dropdown">
            <a class="dropdown-toggle nav-item-iconic" id="languageDropdownMenu" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
                <c:set var="current_languague" value="${not empty WW_TRANS_I18N_LOCALE ? WW_TRANS_I18N_LOCALE : pageContext.response.locale}" />
                <c:out value="${fn:toUpperCase(current_languague.language)}" />
            </a>
            <ul id="languageDropdownComponent" class="dropdown-menu" aria-labelledby="languageDropdownMenu">
                <li><a data-locale="en" href="<s:url namespace="/" includeParams="all"><s:param name="request_locale">en</s:param></s:url>">EN</a></li>
                <li><a data-locale="it" href="<s:url namespace="/" includeParams="all"><s:param name="request_locale">it</s:param></s:url>">IT</a></li>
            </ul>
        </li>
        <li id="preview-portal" class="drawer-pf-trigger2 notifications dropdown">
            <a class="nav-item-iconic" target="#" href="<s:url value="/" />" title="<s:text name="note.goToPortal" /> ( <s:text name="note.sameWindow" /> )">
                <span class="icon fa fa-globe fa-fw"></span>&#32;
                <s:text name="note.goToPortal" />
            </a>
        </li>
        <% if( "true".equalsIgnoreCase(System.getenv().get("ACTIVITY_STREAM_NOTIFICATION_ENABLED")) ) { %>
        <s:if test="#appBuilderIntegrationEnabled == 'true'">
            <li></li>
        </s:if>
        <s:else>
            <li id="notification-ico" class="drawer-pf-trigger2 notifications dropdown">
                <a class="nav-item-iconic drawer-pf-trigger-icon">
                    <span class="fa fa-bell" title="Notifications"></span>
                </a>
            </li>
        </s:else>
        <% } %>
        <li id="infoHeader" class="dropdown">
            <a class="dropdown-toggle nav-item-iconic" id="infoDropdownMenu" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
                <span title="Info" class="fa pficon-info"></span>
                <span class="caret"></span>
            </a>
            <s:if test="#appBuilderIntegrationEnabled == 'true'">
                <ul class="dropdown-menu" aria-labelledby="infoDropdownMenu">
                    <li><a href='<c:out value="${appBuilderBaseURL}"/>about'><s:text name="about.title" /></a></li>
                    <li><a href='<c:out value="${appBuilderBaseURL}"/>license'><s:text name="license.title" /></a></li>
                </ul>
            </s:if>
            <s:else>
                <ul class="dropdown-menu" aria-labelledby="infoDropdownMenu">
                    <li><a href='<s:url namespace="/do/BaseAdmin" action="about"/>'><s:text name="about.title" /></a></li>
                    <li><a href='<s:url namespace="/do/BaseAdmin" action="license"/>'><s:text name="license.title" /></a></li>
                </ul>
            </s:else>
        </li>
        <li id="userDropdown" class="dropdown">
            <a class="dropdown-toggle nav-item-iconic" id="userDropdownMenu" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
                <span title="Username" class="fa fa-user"></span>
                <c:set var="current_username" value="${sessionScope.currentUser}" />
                <c:if test="${null != sessionScope.currentUser.profile}">
                    <c:set var="current_username" value="${sessionScope.currentUser.profile.displayName}" />
                </c:if>
                <c:out value="${current_username}" />
                <span class="caret"></span>
            </a>
            <ul class="dropdown-menu" aria-labelledby="userDropdownMenu">
                <c:if test="${sessionScope.currentUser.entandoUser}">
                    <li>
                        <a href='<s:if test="#appBuilderIntegrationEnabled == 'true'"><c:out value="${appBuilderBaseURL}"/>myProfile</s:if>
                           <s:else><s:url namespace="/do/CurrentUser" action="editProfile"/></s:else>'>
                            <i class="fa fa-user" aria-hidden="true"></i>
                            <s:text name="note.changeYourPassword" />
                        </a>
                    </li>
                </c:if>
                <li>
                    <a href="<s:url action="logout" namespace="/do" />">
                        <i class="fa fa-sign-out" aria-hidden="true"></i>&#32;
                        <s:text name="menu.exit" />
                    </a>
                </li>
            </ul>
            <div class="drawer-pf drawer-pf-custom hide drawer-pf-notifications-non-clickable">
                <div class="drawer-pf-title drawer-pf-title-right-menu">
                    <a id="toggle-stream" class="drawer-pf-toggle-expand"></a>
                    <span class="right-bar-title-pages"><s:text name="title.activityStream.notifications" /></span>
                    <span id="close-notifications" class=" close-button-menu-right pull-right"><i class="fa fa-times" ></i></span>
                </div>
                <div class="panel-group" id="notification-drawer-accordion">
                    <jsp:include page="/WEB-INF/apsadmin/jsp/common/template/mainBody.jsp" />
                </div> 
            </div>
        </li>
    </ul>
</nav>
<% if( "true".equalsIgnoreCase(System.getenv().get("ACTIVITY_STREAM_NOTIFICATION_ENABLED")) ) { %>
<script>
    $(document).ready(function () {
        // Show/Hide Notifications Drawer
        $('.notifications, #close-notifications').click(function () {
            var $drawer = $('.drawer-pf');

            $(this).toggleClass('open');
            if ($drawer.hasClass('hide')) {
                $drawer.removeClass('hide');
                $("#notification-ico").addClass("notification-active")
                setTimeout(function () {
                    if (window.dispatchEvent) {
                        window.dispatchEvent(new Event('resize'));
                    }
                    // Special case for IE
                    if ($(document).fireEvent) {
                        $(document).fireEvent('onresize');
                    }
                }, 100);
            } else {
                $drawer.addClass('hide');
                $("#notification-ico").removeClass("notification-active")
            }
        });
        $('#toggle-stream').click(function () {
            var $drawer = $('.drawer-pf');
            var $drawerNotifications = $drawer.find('.drawer-pf-notification');

            if ($drawer.hasClass('drawer-pf-expanded')) {
                $drawer.removeClass('drawer-pf-expanded');
                $drawerNotifications.removeClass('expanded-notification');
            } else {
                $drawer.addClass('drawer-pf-expanded');
                $drawerNotifications.addClass('expanded-notification');
            }
        });

        // Mark All Read
        $('.panel-collapse').each(function (index, panel) {
            var $panel = $(panel);
            $panel.on('click', '.drawer-pf-action .btn', function () {
                $panel.find('.unread').removeClass('unread');
                $(panel.parentElement).find('.panel-counter').text('0 New Events');
            });
        });

        $('#notification-drawer-accordion').initCollapseHeights('.panel-body');
    });

</script>
<% } %>
