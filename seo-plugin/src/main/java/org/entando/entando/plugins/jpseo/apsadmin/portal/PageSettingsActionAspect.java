/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.entando.entando.plugins.jpseo.apsadmin.portal;

import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.apsadmin.portal.PageSettingsAction;
import com.opensymphony.xwork2.ActionSupport;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.entando.entando.aps.system.services.storage.IStorageManager;
import org.entando.entando.plugins.jpseo.aps.system.JpseoSystemConstants;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

@Aspect
public class PageSettingsActionAspect {

    private static final EntLogger logger =  EntLogFactory.getSanitizedLogger(PageSettingsActionAspect.class);

    public static final String ROBOT_FILENAME = "robots.txt";

    public static final String PARAM_ROBOT_CONTENT_CODE = "robotContent";

    public static final String PARAM_ROBOT_ALTERNATIVE_PATH_CODE = "robotFilePath";

    public static final String SESSION_PARAM_ROBOT_ALTERNATIVE_PATH_CODE = "robotFilePath_sessionParam";

    public static final String SESSION_PARAM_ROBOT_ALTERNATIVE_PATH_CODE_ERROR = "robotFilePath_error_sessionParam";
    
    private IPageManager pageManager;

    private IStorageManager storageManager;

    @Before("execution(* com.agiletec.apsadmin.admin.BaseAdminAction.configSystemParams())")
    public void executeInitConfig(JoinPoint joinPoint) {
        if (!(joinPoint.getTarget() instanceof PageSettingsAction)) {
            return;
        }
        PageSettingsAction action = (PageSettingsAction) joinPoint.getTarget();
        try {
            HttpServletRequest request = ServletActionContext.getRequest();
            HttpSession session = request.getSession();
            String robotContent = "";
            String alternativePath = (String) session.getAttribute(SESSION_PARAM_ROBOT_ALTERNATIVE_PATH_CODE);
            if (StringUtils.isEmpty(alternativePath)) {
                alternativePath = this.getPageManager().getConfig(JpseoSystemConstants.ROBOT_ALTERNATIVE_PATH_PARAM_NAME);
            }
            if (StringUtils.isEmpty(alternativePath)) {
                if (this.getStorageManager().exists(ROBOT_FILENAME, false)) {
                    robotContent = this.getStorageManager().readFile(ROBOT_FILENAME, false);
                } else {
                    String message = "File '" + ROBOT_FILENAME + "' does not exists";
                    action.addFieldError(PARAM_ROBOT_CONTENT_CODE, message);
                }
            } else {
                String errorMessage = (String) session.getAttribute(SESSION_PARAM_ROBOT_ALTERNATIVE_PATH_CODE_ERROR);
                if (null != errorMessage) {
                    action.addFieldError(PARAM_ROBOT_ALTERNATIVE_PATH_CODE, errorMessage);
                    request.getSession().removeAttribute(PARAM_ROBOT_ALTERNATIVE_PATH_CODE);
                } else if (!PageSettingsUtils.isRightPath(alternativePath)) {
                    action.addFieldError(PARAM_ROBOT_ALTERNATIVE_PATH_CODE,
                            action.getText("jpseo.error.robotFilePath.invalid", new String[]{alternativePath}));
                } else if (this.checkPath(alternativePath, action)) {
                    robotContent = this.readFile(alternativePath, action);
                }
            }
            session.removeAttribute(SESSION_PARAM_ROBOT_ALTERNATIVE_PATH_CODE);
            session.removeAttribute(SESSION_PARAM_ROBOT_ALTERNATIVE_PATH_CODE_ERROR);
            if (null != robotContent) {
                request.setAttribute(PARAM_ROBOT_CONTENT_CODE, robotContent);
            }
            if (null != alternativePath) {
                request.setAttribute(PARAM_ROBOT_ALTERNATIVE_PATH_CODE, alternativePath);
            }
        } catch (Exception e) {
            logger.error("Error extracting " + ROBOT_FILENAME + " file content", e);
        }
    }

    private boolean checkPath(String alternativePath, ActionSupport action) {
        File file = new File(alternativePath);
        if (file.exists()) {
            if (!file.canRead() || !file.canWrite()) {
                action.addFieldError(PARAM_ROBOT_ALTERNATIVE_PATH_CODE,
                        action.getText("jpseo.error.robotFilePath.file.requiredAuth", new String[]{alternativePath}));
                return false;
            }
        } else {
            File directory = file.getParentFile();
            if (!directory.exists()) {
                action.addFieldError(PARAM_ROBOT_ALTERNATIVE_PATH_CODE,
                        action.getText("jpseo.error.robotFilePath.directory.notExists", new String[]{directory.getAbsolutePath()}));
                return false;
            } else if (!directory.canRead() || !directory.canWrite()) {
                action.addFieldError(PARAM_ROBOT_ALTERNATIVE_PATH_CODE,
                        action.getText("jpseo.error.robotFilePath.directory.requiredAuth", new String[]{directory.getAbsolutePath()}));
                return false;
            }
        }
        return true;
    }

    protected String readFile(String path, PageSettingsAction action) {
        try {
            File file = new File(path);
            if (file.exists()) {
                return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            } else {
                String message = "File '" + path + "' does not exists";
                action.addFieldError(PARAM_ROBOT_CONTENT_CODE, message);
            }
        } catch (Throwable t) {
            logger.error("Error reading File with path {}", path, t);
            String message = "Error reading File with path " + path + " - " + t.getMessage();
            action.addFieldError(PARAM_ROBOT_CONTENT_CODE, message);
        }
        return "";
    }

    @Before("execution(* com.agiletec.apsadmin.admin.AbstractParameterizableManagerSettingsAction.updateSystemParams())")
    public void executeUpdateSystemParamsForAjax(JoinPoint joinPoint) {
        this.executeUpdateSystemParams(joinPoint);
    }

    @Before("execution(* com.agiletec.apsadmin.portal.PageSettingsAction.updateSystemParamsForAjax())")
    public void executeUpdateSystemParams(JoinPoint joinPoint) {
        HttpServletRequest request = ServletActionContext.getRequest();
        PageSettingsAction action = (PageSettingsAction) joinPoint.getTarget();
        try {
            String robotContent = request.getParameter(PARAM_ROBOT_CONTENT_CODE);
            String alternativePath = request.getParameter(PARAM_ROBOT_ALTERNATIVE_PATH_CODE);
            InputStream is = (!StringUtils.isBlank(robotContent))
                    ? new ByteArrayInputStream(robotContent.getBytes("UTF-8")) : null;
            if (StringUtils.isBlank(alternativePath)) {
                //default PATH
                if (null != is) {
                    this.getStorageManager().saveFile(ROBOT_FILENAME, false, is);
                } else {
                    this.getStorageManager().deleteFile(ROBOT_FILENAME, false);
                }
            } else if (!PageSettingsUtils.isRightPath(alternativePath)) {
                action.addFieldError(PARAM_ROBOT_ALTERNATIVE_PATH_CODE,
                        action.getText("jpseo.error.robotFilePath.invalid", new String[]{alternativePath}));
            } else if (this.checkPath(alternativePath, action)) {
                if (null != is) {
                    // SONAR-FALSE-POSITIVE:
                    // alternativePath is actually checked by PageSettingsUtils.isRightPath
                    this.saveFile(alternativePath, is, action); //NOSONAR
                } else {
                    File file = new File(alternativePath);
                    try {
                        if (file.exists()) {
                            // SONAR-FALSE-POSITIVE:
                            // alternativePath is actually checked by PageSettingsUtils.isRightPath
                            file.delete();  //NOSONAR
                        }
                    } catch (Exception e) {
                        logger.error("error deleting file {}", alternativePath);
                    }
                }
            }
            Map<String, String> newParams = new HashMap<>();
            newParams.put(JpseoSystemConstants.ROBOT_ALTERNATIVE_PATH_PARAM_NAME, "");
            if (!StringUtils.isEmpty(alternativePath)) {
                newParams.put(JpseoSystemConstants.ROBOT_ALTERNATIVE_PATH_PARAM_NAME, alternativePath);
            } else {
                newParams.put(JpseoSystemConstants.ROBOT_ALTERNATIVE_PATH_PARAM_NAME, "");
            }
            this.getPageManager().updateParams(newParams);
        } catch (Throwable t) {
            logger.error("error updating page settings for seo", t);
            action.addActionError("error updating page settings for seo");
        }
    }

    private void saveFile(String filePath, InputStream is, PageSettingsAction action) throws IOException {
        FileOutputStream outStream = null;
        try {
            byte[] buffer = new byte[1024];
            int length = -1;
            outStream = new FileOutputStream(filePath);
            while ((length = is.read(buffer)) != -1) {
                outStream.write(buffer, 0, length);
                outStream.flush();
            }
        } catch (IOException t) {
            String message = "Error saving File '" + filePath + "' - " + t.getMessage();
            action.addFieldError(PARAM_ROBOT_ALTERNATIVE_PATH_CODE, message);
            logger.error("Error on saving file", t);
        } finally {
            if (null != outStream) {
                outStream.close();
            }
            if (null != is) {
                is.close();
            }
        }
    }

    protected IPageManager getPageManager() {
        return pageManager;
    }
    public void setPageManager(IPageManager pageManager) {
        this.pageManager = pageManager;
    }

    protected IStorageManager getStorageManager() {
        return storageManager;
    }
    public void setStorageManager(IStorageManager storageManager) {
        this.storageManager = storageManager;
    }

}
