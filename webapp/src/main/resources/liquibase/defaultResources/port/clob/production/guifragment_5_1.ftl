<#assign wp=JspTaglibs["/aps-core"]>

<@wp.info key="langs" var="langsVar" />
<@wp.info key="currentLang" var="currentLangVar" />

<#include "entando_ootb_carbon_include" >

<#assign langstrs = [] />
<#list langsVar as curLangVar>
  <#assign langurl><@wp.url lang="${curLangVar.code}" paramRepeat=true /></#assign>
  <#assign langstr = ["{\"code\": \"" + curLangVar.code + "\", \"descr\": \"" + curLangVar.descr + "\", \"url\": \""+ langurl +"\"}"]  />
  <#assign langstrs = langstrs + langstr />
</#list>
<#assign lang_json_string = langstrs?join(", ") />

<choose-language-widget
  currentLang="${currentLangVar}"
  languages="[${lang_json_string?html}]"
></choose-language-widget>