Before (problematic):
<#if parameters.disabled!false>

After (compatible with Struts2 6.x):
<#if (parameters.disabled?? && parameters.disabled?is_string && parameters.disabled == "true") || (parameters.disabled?? && parameters.disabled?is_boolean && parameters.disabled)>


in .flt     parameters. to -> attributes.