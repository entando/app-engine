<#-- 
removed iteration cycle (no list expected as it used to be in the original Struts2 template) added 'checked' 
-->
<#--
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
-->

<#-- added block for entando - start -->
<#assign itemKey = stack.findValue('top')/>
<#assign itemKeyStr = itemKey.toString() />
<#assign itemValue = stack.findString('top')/>
<#-- added block for entando - end -->

<input type="radio"<#rt/>
<#if attributes.name?has_content>
 name="${attributes.name?no_esc}"<#rt/>

<#-- modified block for entando -->
</#if> id="${attributes.id}"<#rt/>

<#-- added block for entando - start -->
<#if attributes.nameValue?exists>
 value="${attributes.nameValue}"<#rt/>
</#if>
<#-- added block for entando - end -->

<#if (attributes.disabled?? && attributes.disabled?is_string && attributes.disabled == "true") || (attributes.disabled?? && attributes.disabled?is_boolean && attributes.disabled)>
 disabled="disabled"<#rt/>
</#if>
<#if attributes.tabindex?has_content>
 tabindex="${attributes.tabindex}"<#rt/>
</#if>
<#if itemCssClass?has_content>
 class="${itemCssClass}"<#rt/>
<#else>
    <#if attributes.cssClass?has_content>
        class="${attributes.cssClass}"<#rt/>
    </#if>
</#if>
<#if itemCssStyle?has_content>
 style="${itemCssStyle}"<#rt/>
<#else>
    <#if attributes.cssStyle?has_content>
        style="${attributes.cssStyle}"<#rt/>
    </#if>
</#if>
<#if itemTitle?has_content>
 title="${itemTitle}"<#rt/>
<#else>
    <#if attributes.title?has_content>
 title="${attributes.title}"<#rt/>
    </#if>
</#if>
<#include "/${attributes.templateDir}/simple/css.ftl" />
<#include "/${attributes.templateDir}/simple/scripting-events.ftl" />
<#include "/${attributes.templateDir}/simple/common-attributes.ftl" />
<#global evaluate_dynamic_attributes = true/>
<#include "/${attributes.templateDir}/simple/dynamic-attributes.ftl" />

<#-- added block for entando - start -->
<#if attributes.checked?? >
 checked="checked"<#rt/>
</#if>
<#-- added block for entando - end -->

/><#rt/>

<#-- modified block for entando -->
<label for="${attributes.id}"<#include "/${attributes.templateDir}/simple/css.ftl"/>><#rt/></label>
