<%-- //TODO: composite --%>
<%@ taglib prefix="s" uri="/struts-tags" %>

<s:set var="masterCompositeAttributeTracer" value="#attributeTracer" />
<s:set var="masterCompositeAttribute" value="#attribute" />
<s:iterator value="#attribute.attributes" var="attribute">
<s:set var="attributeTracer" value="#masterCompositeAttributeTracer.getCompositeTracer(#masterCompositeAttribute)"></s:set>
<s:set var="parentAttribute" value="#masterCompositeAttribute"></s:set>
	<p>
		<label for="<s:property value="%{#attributeTracer.getFormFieldName(#attribute)}" />" class="basic-mint-label"><s:property value="#attribute.name"/><s:include value="/WEB-INF/apsadmin/jsp/entity/modules/include/attributeInfo.jsp" />:</label>
		<s:if test="#attribute.type == 'Boolean' || #attribute.type == 'ThreeState'">
			</p>
		</s:if>

		<s:if test="#attribute.type == 'Text'">
			<br /><s:include value="/WEB-INF/apsadmin/jsp/entity/modules/textAttribute.jsp" />
		</s:if>
		<s:elseif test="#attribute.type == 'Monotext'">
			<br /><s:include value="/WEB-INF/apsadmin/jsp/entity/modules/monotextAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'Hypertext'">
			<br /><s:include value="/WEB-INF/apsadmin/jsp/entity/modules/hypertextAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'Longtext'">
			<br /><s:include value="/WEB-INF/apsadmin/jsp/entity/modules/longtextAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'Enumerator'">
			<br /><s:include value="/WEB-INF/apsadmin/jsp/entity/modules/enumeratorAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'EnumeratorMap'">
			<br /><s:include value="/WEB-INF/apsadmin/jsp/entity/modules/enumeratorMapAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'Number'">
			<br /><s:include value="/WEB-INF/apsadmin/jsp/entity/modules/numberAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'Date'">
			<br /><s:include value="/WEB-INF/apsadmin/jsp/entity/modules/dateAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'Timestamp'">
			<br /><s:include value="/WEB-INF/apsadmin/jsp/entity/modules/timestampAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'Boolean'">
			<s:include value="/WEB-INF/apsadmin/jsp/entity/modules/booleanAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'ThreeState'">
			<s:include value="/WEB-INF/apsadmin/jsp/entity/modules/threeStateAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'CheckBox'">
			<br /><s:include value="/WEB-INF/apsadmin/jsp/entity/modules/checkBoxAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'Email'">
			<br /><s:include value="/WEB-INF/apsadmin/jsp/entity/modules/monotextAttribute.jsp" />
		</s:elseif>

	<s:if test="#attribute.type != 'Boolean' && #attribute.type != 'ThreeState'">
		</p>
	</s:if>

</s:iterator>
<s:set var="attributeTracer" value="#masterCompositeAttributeTracer" />
<s:set var="attribute" value="#masterCompositeAttribute" />
<s:set var="parentAttribute" value=""></s:set>