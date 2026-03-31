<#assign wp=JspTaglibs["/aps-core"]>
<script nonce="<@wp.cspNonce />">
if (!window._jpwebdynamicformDateHandler) {
    window._jpwebdynamicformDateHandler = true;
    document.addEventListener('submit', function(e) {
        if (e.target.tagName === 'FORM') {
            e.target.querySelectorAll('input[data-jpwebdynamicform-date]').forEach(function(dateInput) {
                if (dateInput.value) {
                    var parts = dateInput.value.split('-');
                    if (parts.length === 3) {
                        dateInput.setAttribute('type', 'hidden');
                        dateInput.value = parts[2] + '/' + parts[1] + '/' + parts[0];
                    }
                }
            });
        }
    }, true);
}
</script>