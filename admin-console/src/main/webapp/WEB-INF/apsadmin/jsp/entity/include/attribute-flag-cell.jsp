<%@ taglib prefix="s" uri="/struts-tags" %>
<%--
    Renders one boolean-flag cell of an attribute list (Mandatory, Filter, ...).

    Shared by the top-level attribute list (include/attribute-list.jsp) and the Composite children
    list (attribute-type-entry-composite.jsp) so the two tables cannot drift apart again.

    Parameters, set by the caller with <s:set> right before <s:include>:
      #flagValue          boolean - the current value of the flag.
      #flagApplicable     boolean - false when the flag can never apply to this attribute; the cell
                                    then shows a neutral dash instead of an on/off box.
      #flagLabel          string  - i18n key of the column label, used to build the accessible name.
      #flagUnavailableKey string  - i18n key explaining why the flag is not applicable. Read only
                                    when #flagApplicable is false.
--%>
<s:set var="flagLabelText" value="%{getText(#flagLabel)}" />
<td class="text-center">
    <s:if test="!#flagApplicable">
        <s:set var="flagUnavailableText" value="%{null == #flagUnavailableKey ? '' : getText(#flagUnavailableKey)}" />
        <span class="text-muted" aria-hidden="true" title="<s:property value="#flagUnavailableText" />">&ndash;</span>
        <span class="sr-only"><s:property value="#flagLabelText" />: <s:property value="#flagUnavailableText" /></span>
    </s:if>
    <s:elseif test="#flagValue">
        <span class="icon fa fa-check-square-o" aria-hidden="true" title="<s:text name="label.yes" />"></span>
        <span class="sr-only"><s:property value="#flagLabelText" />: <s:text name="label.yes" /></span>
    </s:elseif>
    <s:else>
        <span class="icon fa fa-square-o" aria-hidden="true" title="<s:text name="label.no" />"></span>
        <span class="sr-only"><s:property value="#flagLabelText" />: <s:text name="label.no" /></span>
    </s:else>
</td>
