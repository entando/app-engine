package com.agiletec.apsadmin.admin.reload;

import com.agiletec.aps.util.ApsWebApplicationUtils;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.slf4j.Logger;

import jakarta.servlet.http.HttpServletRequest;

public class ReloadConfigThread extends Thread {

    public static final String RELOAD_THREAD = "RELOAD THREAD";

    private final Logger log = EntLogFactory.getSanitizedLogger(ReloadConfigThread.class);
    private final HttpServletRequest request;

    public ReloadConfigThread(final HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public void run() {
        try {
            ApsWebApplicationUtils.executeSystemRefresh(request);
            log.info("ReloadConfigThread completed execution");
        } catch (InterruptedException e) {
            ApsWebApplicationUtils.getReloadInfo().put(RELOAD_THREAD, e.getMessage());
            log.error("Thread interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Throwable e) {
            ApsWebApplicationUtils.getReloadInfo().put(RELOAD_THREAD, e.getMessage());
            log.error("unexpected thread error", e);
            Thread.currentThread().interrupt();
        }
    }

}
