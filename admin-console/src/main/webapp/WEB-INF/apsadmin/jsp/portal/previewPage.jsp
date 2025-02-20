<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wp" uri="/aps-core" %>

<s:set var="currentSize" value="screenSize" />
<html>
<head>
    <link rel="stylesheet" type="text/css" href="<wp:resourceURL ignoreTenant="true" />administration/patternfly/css/patternfly.css"/>
    <link rel="stylesheet" type="text/css" href="<wp:resourceURL ignoreTenant="true" />administration/css/pages/previewPage.css"/>
    <link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:ital,opsz,wght@0,14..32,100..900;1,14..32,100..900&display=swap" rel="stylesheet">

    <script src="https://ajax.googleapis.com/ajax/libs/jquery/2.2.4/jquery.min.js"></script>
    <script>
        var PROPERTY = {
            baseUrl: '<wp:info key="systemParam" paramName="applicationBaseURL" />',
            lang: '<s:property value="lang" />',
            token: '<s:property value="token" />',
            pageCode: '<s:property value="pageCode" />',
            previewWidth: '<s:property value="#currentSize.width" />',
            previewHeight: '<s:property value="#currentSize.width" />'
        };
    </script>
    <script src="<wp:resourceURL ignoreTenant="true" />administration/js/pages/previewPage.js"></script>
</head>
<body>
    <div class="main-container">
        <iframe id="previewFrame">
        </iframe>
    </div>
    <div class="bottom-bar">
        <label for="preview-mode-select"><s:text name="previewPage.previewMode" /></label>
        <select id="preview-mode-select" class="preview-mode-select">
            <option value="desktop"><s:text name="previewPage.option.desktop" /></option>
            <option value="tablet"><s:text name="previewPage.option.tablet" /></option>
            <option value="smartphone"><s:text name="previewPage.option.smartphone" /></option>
            <option value="custom"><s:text name="previewPage.option.custom" /></option>
        </select>

        <div class="custom-panel">
            <input class="custom-width" onkeypress="return event.charCode >= 48 && event.charCode <= 57"/>
            x
            <input class="custom-height" onkeypress="return event.charCode >= 48 && event.charCode <= 57"/>
            <button class="btn custom-size-btn"><s:text name="label.ok" /></button>
        </div>
        
        <label for="preview-mode-lang"><s:text name="previewPage.lang" /></label>
        <s:select  list="langs" id="preview-mode-lang" name="lang" listKey="code" listValue="descr"></s:select>

        <p><s:text name="previewPage.links.warning" /></p>
    </div>
</body>
</html>


