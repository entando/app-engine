<#assign s=JspTaglibs["/struts-tags"]>
<#assign wp=JspTaglibs["/aps-core"]>
<#assign wpsf=JspTaglibs["/apsadmin-form"]>

<@s.set var="titleKey">jpwebdynamicform_TITLE_<@s.property value="typeCode"/></@s.set>
<@s.set var="subtitleKey">jpwebdynamicform_SUBTITLE_<@s.property value="typeCode"/></@s.set>
<@s.set var="typeCodeKey" value="typeCode" />
<@s.set var="myCurrentPage"><@wp.currentPage param="code"/></@s.set>
<h2 class="title-divider"><span><@wp.i18n key="${titleKey}" /></span>
    <small><@wp.i18n key="${subtitleKey}" /></small></h2>

<form action="<@wp.action path="/ExtStr2/do/jpwebdynamicform/Message/User/captchaConfirm.action"/>" method="post" id="formContact" >
    <@s.if test="hasFieldErrors()">
        <div class="alert alert-error">
            <ul>
                <@s.iterator value="fieldErrors">
                    <@s.iterator value="value">
                        <li><@s.property escape="false" /></li>
                    </@s.iterator>
                </@s.iterator>
            </ul>
        </div>
    </@s.if>
    <@s.if test="hasActionErrors()">
        <div class="alert alert-error">
            <ul>
                <@s.iterator value="actionErrors">
                    <li><@s.property escape="false" /></li>
                </@s.iterator>
            </ul>
        </div>
    </@s.if>
    <@s.if test="recaptchaAfterEnabled">
        <@wp.fragment code="jpwebdynamicform_is_captchaInclude" escapeXml=false />
    </@s.if>
    <p>
        <@s.set var="labelSubmit"><@wp.i18n key="jpwebdynamicform_INVIA" /></@s.set>
        <@wpsf.submit useTabindexAutoIncrement=true value="%{#labelSubmit}" cssClass="btn btn-inverse"/>
    </p>
</form>