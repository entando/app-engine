<%@ taglib uri="/struts-tags" prefix="s"%>
<%@ taglib prefix="wpsf" uri="/apsadmin-form"%>
<%@ taglib uri="/apsadmin-core" prefix="wpsa"%>
<%@ taglib uri="/aps-core" prefix="wp"%>

<s:if test="null != entityType.attributeList && entityType.attributeList.size() > 0">

    <label class="col-sm-2 control-label" for="entityTypeCode">
        <s:text name="label.attributes" />
    </label>
    <div class="col-sm-10 table-in-col10">
        <div class="table-responsive overflow-visible">
            <table class="table table-striped table-bordered"
                   id="fagiano_entityTypesList">
                <tr>
                    <th class="table-w-25">
                <s:text name="label.code" />
                </th>
                <th class="table-w-15">
                <s:text name="label.type" />
                </th>
                <th class="table-w-25">
                <s:text name="name.roles" />
                </th>
                <th class="text-center" style="width: 59px;">
                <s:text name="Entity.attribute.flag.mandatory.full" />
                </th>
                <th class="text-center" style="width: 59px;">
                <s:text name="label.filter" />
                </th>
                <th class="text-center" style="width: 59px;">
                <s:text name="label.actions" />
                </th>
                </tr>

                <s:iterator value="#entityType.attributeList" var="attribute"
                            status="elementStatus">
                    <tr>
                        <td>
                    <s:property value="#attribute.name" />
                    </td>
                    <td><s:property value="#attribute.type" />
                    <s:if  test="#attribute.type == 'Monolist' || #attribute.type == 'List'">:
                        <s:property value="#attribute.nestedAttributeTypeCode" />
                    </s:if>
                    </td>

                    <s:if test="null == #attribute.roles || #attribute.length() == 0">
                        <td class="centerText">
                            <span title="<s:text name="label.none" />">&ndash;</span>
                        </td>
                    </s:if>
                    <s:else>
                        <td>
                            <ul class="noBullet">
                                <s:iterator value="#attribute.roles" var="attributeRoleName">
                                    <li>
                                        <span class="monospace">
                                            <s:property value="#attributeRoleName" /> &ndash;
                                        </span>
                                    <s:property value="%{getAttributeRole(#attributeRoleName).description}" />
                                    </li>
                                </s:iterator>
                            </ul>
                        </td>
                    </s:else>

                    <s:set var="flagValue" value="#attribute.required" />
                    <s:set var="flagApplicable" value="true" />
                    <s:set var="flagLabel" value="'Entity.attribute.flag.mandatory.full'" />
                    <s:include value="/WEB-INF/apsadmin/jsp/entity/include/attribute-flag-cell.jsp" />

                    <%-- Complex containers (Composite, List, Monolist) are never searchable
                         themselves - only their children can be - so they render as "not applicable"
                         rather than as an unchecked box. --%>
                    <s:set var="flagValue" value="#attribute.searchable" />
                    <s:set var="flagApplicable" value="%{#attribute.searchableOptionSupported || #attribute.searchable}" />
                    <s:set var="flagLabel" value="'label.filter'" />
                    <s:set var="flagUnavailableKey" value="'Entity.attribute.flag.searchable.notApplicable.type'" />
                    <s:include value="/WEB-INF/apsadmin/jsp/entity/include/attribute-flag-cell.jsp" />

                    <td class="text-center">
                        <div class="dropdown dropdown-kebab-pf">
                            <button class="btn btn-menu-right dropdown-toggle" type="button"
                                    data-toggle="dropdown" aria-haspopup="true"
                                    aria-expanded="true">
                                <span class="fa fa-ellipsis-v"></span>
                            </button>
                            <ul class="dropdown-menu dropdown-menu-right"
                                aria-labelledby="dropdownKebabRight">
                                <li>
                                <wpsa:actionParam action="editAttribute" var="actionName">
                                    <wpsa:actionSubParam name="attributeName" value="%{#attribute.name}" />
                                </wpsa:actionParam>
                                <wpsf:submit action="%{#actionName}" type="button"  title="%{getText('label.edit')}: %{#attribute.name}"
                                             cssClass="btn-no-button">
                                    <span class="sr-only">
                                        <s:text name="label.edit" />:
                                        <s:property value="#attribute.name" />
                                    </span>
                                    <s:text name="label.edit" />
                                </wpsf:submit>
                                </li>

                                <s:set var="elementIndex" value="#elementStatus.index" />
                                <s:include
                                    value="/WEB-INF/apsadmin/jsp/entity/include/attribute-operations-misc.jsp" />
                            </ul>
                        </div>
                    </td>
                    </tr>
                </s:iterator>

            </table>
        </div>
    </div>

</s:if>
