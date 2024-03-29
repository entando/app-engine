<%@ taglib prefix="s" uri="/struts-tags" %>

<s:set var="masterCompositeAttributeTracer" value="#attributeTracer" />
<s:set var="masterCompositeAttribute" value="#attribute" />
<s:iterator value="#attribute.attributes" var="attribute">
<s:set var="attributeTracer" value="#masterCompositeAttributeTracer.getCompositeTracer(#masterCompositeAttribute)"></s:set>
<s:set var="parentAttribute" value="#masterCompositeAttribute"></s:set>
	<p>	
		<label for="<s:property value="%{#attributeTracer.getFormFieldName(#attribute)}" />"><s:property value="#attribute.name"/></label>
		<s:if test="#attribute.type == 'Text'">
			<s:include value="/WEB-INF/apsadmin/jsp/entity/view/textAttribute.jsp" />
		</s:if>
		<s:elseif test="#attribute.type == 'Monotext' || #attribute.type == 'Email'">
			<s:include value="/WEB-INF/apsadmin/jsp/entity/view/monotextAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'Hypertext'">
			<s:include value="/WEB-INF/apsadmin/jsp/entity/view/hypertextAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'Longtext'">
			<s:include value="/WEB-INF/apsadmin/jsp/entity/view/longtextAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'Enumerator'">
			<s:include value="/WEB-INF/apsadmin/jsp/entity/view/enumeratorAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'EnumeratorMap'">
			<s:include value="/WEB-INF/apsadmin/jsp/entity/view/enumeratorMapAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'Number'">
			<s:include value="/WEB-INF/apsadmin/jsp/entity/view/numberAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'Date'">
			<s:include value="/WEB-INF/apsadmin/jsp/entity/view/dateAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'Timestamp'">
			<s:include value="/WEB-INF/apsadmin/jsp/entity/view/timestampAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'Boolean'">
			<s:include value="/WEB-INF/apsadmin/jsp/entity/view/booleanAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'ThreeState'">
			<s:include value="/WEB-INF/apsadmin/jsp/entity/view/threeStateAttribute.jsp" />
		</s:elseif>
		<s:elseif test="#attribute.type == 'CheckBox'">
			<s:include value="/WEB-INF/apsadmin/jsp/entity/view/checkBoxAttribute.jsp" />
		</s:elseif>
	</p>
</s:iterator>
<s:set var="attributeTracer" value="#masterCompositeAttributeTracer" />
<s:set var="attribute" value="#masterCompositeAttribute" />
<s:set var="parentAttribute" value=""></s:set>