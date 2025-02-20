<%@ taglib prefix="wp" uri="/aps-core" %>

<!--CSS inclusions-->
<link rel="stylesheet" href="<wp:resourceURL ignoreTenant="true" />administration/bootstrap/css/bootstrap.min.css" media="screen" />
<link rel="stylesheet" href="<wp:resourceURL ignoreTenant="true" />administration/patternfly/css/patternfly.css">
<link rel="stylesheet" href="<wp:resourceURL ignoreTenant="true" />administration/patternfly/css/patternfly-additions.css">
<link rel="stylesheet" href="<wp:resourceURL ignoreTenant="true" />administration/css/entando-admin-console-default-theme.css">
<link rel="stylesheet" type="text/css" href="<wp:resourceURL ignoreTenant="true" />administration/css/pages/settingsPage.css"/>



<!--JS inclusions-->
<script src="<wp:resourceURL ignoreTenant="true" />administration/js/jquery-3.4.1.min.js"></script>
<script src="<wp:resourceURL ignoreTenant="true" />administration/js/jquery-migrate-3.0.1.min.js"></script>
<% if( "true".equalsIgnoreCase(System.getenv().get("ACTIVITY_STREAM_NOTIFICATION_ENABLED")) ) { %>
    <script src="<wp:resourceURL ignoreTenant="true" />administration/js/entando-stream.js"></script>
<% } %>
<script src="<wp:resourceURL ignoreTenant="true" />administration/js/jquery.matchHeight-min.js"></script>
<script src="<wp:resourceURL ignoreTenant="true" />administration/patternfly/js/patternfly.js"></script>
<script src="<wp:resourceURL ignoreTenant="true" />administration/js/pages/settingsPage.js"></script>
<script src="<wp:resourceURL ignoreTenant="true" />administration/bootstrap/js/bootstrap.min.js"></script>
<script src="<wp:resourceURL ignoreTenant="true" />administration/js/bootstrap-switch.min.js"></script>
<script src="<wp:resourceURL ignoreTenant="true" />administration/js/bootstrap-datepicker/bootstrap-datepicker.js"></script>
