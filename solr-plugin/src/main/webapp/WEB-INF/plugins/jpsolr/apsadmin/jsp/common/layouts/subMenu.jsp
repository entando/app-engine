<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wp" uri="/aps-core" %>
<wp:ifauthorized permission="superuser">
    <li class="list-group-item">
        <a href="<s:url action="config" namespace="/do/jpsolr" />">
            <span class="list-group-item-value"><s:text name="jpsolr.admin.config" /></span>
        </a>
    </li>
</wp:ifauthorized>
