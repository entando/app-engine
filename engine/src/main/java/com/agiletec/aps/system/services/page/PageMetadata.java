/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package com.agiletec.aps.system.services.page;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.aps.system.services.pagemodel.PageModel;
import com.agiletec.aps.util.ApsProperties;
import java.io.Serializable;

/**
 * This is the representation of a portal page metadata
 *
 * @author E.Mezzano, spuddu
 */
public class PageMetadata implements Cloneable, Serializable {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(PageMetadata.class);

    private String group;

    private ApsProperties titles = new ApsProperties();

    private Set<String> extraGroups;

    private String modelCode;

    private boolean showable = false;

    private boolean useExtraTitles = false;

    private String mimeType;

    private String charset;

    private Date updatedAt;

    @Override
    public PageMetadata clone() {
        PageMetadata copy = null;
        try {
            copy = this.getClass().newInstance();
            copy.setGroup(this.getGroup());
            ApsProperties titles = new ApsProperties();
            titles.putAll(this.getTitles());
            copy.setTitles(titles);
            Set<String> extraGroups = this.getExtraGroups();
            if (extraGroups != null) {
                copy.setExtraGroups(new TreeSet<>(extraGroups));
            }
            copy.setModelCode(this.getModelCode());
            copy.setShowable(this.isShowable());
            copy.setUseExtraTitles(this.isUseExtraTitles());
            copy.setMimeType(this.getMimeType());
            copy.setCharset(this.getCharset());
            copy.setUpdatedAt(this.getUpdatedAt());
        } catch (Throwable t) {
            logger.error("Error cloning {}" + this.getClass(), t);
            throw new RuntimeException("Error cloning " + this.getClass(), t);
        }
        return copy;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public ApsProperties getTitles() {
        return titles;
    }

    /**
     * Set the titles of the node.
     *
     * @param titles A set of properties with the titles, where the keys are the
     * codes of language.
     */
    public void setTitles(ApsProperties titles) {
        this.titles = titles;
    }

    public void setTitle(String langCode, String title) {
        this.getTitles().setProperty(langCode, title);
    }

    public String getTitle(String langCode) {
        return this.getTitles().getProperty(langCode);
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public void addExtraGroup(String groupName) {
        if (null == this.getExtraGroups()) {
            this.setExtraGroups(new HashSet<>());
        }
        this.getExtraGroups().add(groupName);
    }

    public void removeExtraGroup(String groupName) {
        if (null == this.getExtraGroups()) {
            return;
        }
        this.getExtraGroups().remove(groupName);
    }

    public void setExtraGroups(Set<String> extraGroups) {
        this.extraGroups = extraGroups;

    }

    public Set<String> getExtraGroups() {
        return extraGroups;
    }

    /**
     * WARING: this method is reserved to the page manager service only. This
     * returns a boolean values indicating whether the page is displayed in the
     * menus or similar.
     *
     * @return true if the page must be shown in the menu, false otherwise.
     */
    public boolean isShowable() {
        return showable;
    }

    /**
     * WARING: this method is reserved to the page manager service only. Toggle
     * the visibility of the current page in the menu or similar.
     *
     * @param showable a boolean which toggles the visibility on when true, off
     * otherwise.
     */
    public void setShowable(boolean showable) {
        this.showable = showable;
    }

    public boolean isUseExtraTitles() {
        return useExtraTitles;
    }

    public void setUseExtraTitles(boolean useExtraTitles) {
        this.useExtraTitles = useExtraTitles;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "PageMetadata";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        result = prime * result + ((charset == null) ? 0 : charset.hashCode());
        result = prime * result + ((extraGroups == null) ? 0 : extraGroups.hashCode());
        result = prime * result + ((mimeType == null) ? 0 : mimeType.hashCode());
        result = prime * result + ((modelCode == null) ? 0 : modelCode.hashCode());
        result = prime * result + (showable ? 1231 : 1237);
        result = prime * result + ((titles == null) ? 0 : titles.hashCode());
        result = prime * result + ((updatedAt == null) ? 0 : updatedAt.hashCode());
        result = prime * result + (useExtraTitles ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        boolean equalConf = this.hasEqualConfiguration(obj);
        if (equalConf) {
            PageMetadata other = (PageMetadata) obj;
            if (updatedAt == null) {
                if (other.updatedAt != null) {
                    return false;
                }
            } else if (!updatedAt.equals(other.updatedAt)) {
                return false;
            }
        }
        return equalConf;
    }

    /**
     * all but lastUpdate
     *
     * @param obj
     * @return
     */
    public boolean hasEqualConfiguration(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!this.getClass().getName().equals(obj.getClass().getName())) {
            return false;
        }
        PageMetadata other = (PageMetadata) obj;
        if (this.charset == null) {
            if (other.charset != null) {
                return false;
            }
        } else if (!charset.equals(other.charset)) {
            return false;
        }
        if (extraGroups == null) {
            if (other.extraGroups != null) {
                return false;
            }
        } else if (!extraGroups.equals(other.extraGroups)) {
            return false;
        }
        if (mimeType == null) {
            if (other.mimeType != null) {
                return false;
            }
        } else if (!mimeType.equals(other.mimeType)) {
            return false;
        }
        if (group == null) {
            if (other.group != null) {
                return false;
            }
        } else if (!group.equals(other.group)) {
            return false;
        }
        if (modelCode == null) {
            if (other.modelCode != null) {
                return false;
            }
        } else if (!modelCode.equals(other.modelCode)) {
            return false;
        }
        if (showable != other.showable) {
            return false;
        }
        if (titles == null) {
            if (other.titles != null) {
                return false;
            }
        } else if (!titles.equals(other.titles)) {
            return false;
        }
        if (useExtraTitles != other.useExtraTitles) {
            return false;
        }
        return true;
    }

}
