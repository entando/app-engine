<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="wp" uri="/aps-core" %>

<s:include value="/WEB-INF/apsadmin/jsp/admin/inc/reload-menu-intro.jsp" />

<div class="text-center">
    <wp:ifauthorized permission="superuser">
        <i class="fa fa-question esclamation-big" aria-hidden="true"></i>
        <p class="esclamation-underline"><s:text name="menu.reload.verify"/></p>
        <p>
            <s:text name="label.reload.message.check"/>
        </p>
        <div class="btn button-fixed-width">
            <a id="link-reload-action" href="<s:url namespace="/do/BaseAdmin" action="reloadStatus" />" class="btn-primary button-fixed-width">
                <s:text name="label.reload.check" />
            </a>
        </div>
        <div>
            <h3 id="show-reload-progress">
                <s:text name="label.reload.progress"/>:&nbsp;
                <b><s:property value="%{getReloadProgress()}"/>%</b>
            </h3>
        </div>

    </wp:ifauthorized>
</div>

<script>
  $(document).ready(function () {
    let reloadUrl = "<s:url namespace='/do/BaseAdmin' action='reloadStatusJson' />";

    function refreshProgress() {
      $.getJSON(reloadUrl, function (data) {
        let progress = parseInt(data, 10);

        if (!isNaN(progress)) {
          if (progress === -1) {
            $('#show-reload-progress b').text('100%');
            window.location.href = $('#link-reload-action').attr('href');
          } else {
            $('#show-reload-progress b').text(progress + '%');
          }
        } else {
          console.warn('NaN, leaving...');
          window.location.href = $('#link-reload-action').attr('href');
        }
      }).fail(function (jqxhr, textStatus, error) {
        console.error('AJAX error:', textStatus, error);
      });
    }

    refreshProgress();
    setInterval(refreshProgress, 2000);
  });
</script>
