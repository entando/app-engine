<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="/aps-core" prefix="wp" %>

<s:if test="%{recaptchaType == 'v3'}">
    <script nonce="<wp:cspNonce />" src="https://www.google.com/recaptcha/api.js?render=<wp:info key="systemParam" paramName="jpwebdynamicform_recaptcha_publickey" />"></script>
    <input type="hidden" id="recaptchaToken" name="recaptchaToken"></input>
    <script nonce="<wp:cspNonce />">
        function handleCaptchaToken(form, tokenField){
            grecaptcha.ready(function() {
                grecaptcha.execute('<wp:info key="systemParam" paramName="jpwebdynamicform_recaptcha_publickey" />', {action: 'submit'}).then(function(token) {
                    document.getElementById(tokenField).value = token;
                    document.getElementById(form).submit();
                });
            });
        }
        document.getElementById('jpwebdynamicform_form').addEventListener('submit', function(e) {
            e.preventDefault();
            handleCaptchaToken('jpwebdynamicform_form', 'recaptchaToken');
        });
    </script>
</s:if>
<s:else>
    <script nonce="<wp:cspNonce />" src="https://www.google.com/recaptcha/api.js" async defer></script>
    <div class="g-recaptcha mb-5" data-sitekey="<wp:info key="systemParam" paramName="jpwebdynamicform_recaptcha_publickey" />"></div>
</s:else>
