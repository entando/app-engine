/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package com.agiletec.aps.system.services.category.cache;

import com.agiletec.aps.system.common.AbstractCacheWrapper;
import com.agiletec.aps.system.services.category.Category;
import com.agiletec.aps.system.services.category.ICategoryDAO;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.util.ApsProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.apache.commons.lang3.ArrayUtils;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.springframework.cache.Cache;

/**
 * @author E.Santoboni
 */
public class CategoryManagerCacheWrapper extends AbstractCacheWrapper implements ICategoryManagerCacheWrapper {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(CategoryManagerCacheWrapper.class);

    @Override
    public void release() {
        Cache cache = this.getCache();
        List<String> codes = (List<String>) this.get(cache, CATEGORY_CODES_CACHE_NAME, List.class);
        if (null != codes) {
            for (String code : codes) {
                cache.evict(CATEGORY_CACHE_NAME_PREFIX + code);
            }
            cache.evict(CATEGORY_CODES_CACHE_NAME);
        }
        cache.evict(CATEGORY_STATUS_CACHE_NAME);
        cache.evict(CATEGORY_ROOT_CACHE_NAME);
    }

    @Override
    public void initCache(ICategoryDAO categoryDAO, ILangManager langManager) throws EntException {
        List<Category> categories = null;
        try {
            categories = categoryDAO.loadCategories(langManager);
            if (categories.isEmpty()) {
                Category root = this.createRoot(langManager);
                categoryDAO.addCategory(root);
                categories.add(root);
            }
            Cache cache = this.getCache();
            this.initCache(cache, categories);
        } catch (Throwable t) {
            logger.error("Error loading the category tree", t);
            throw new EntException("Error loading the category tree.", t);
        }
    }

    protected Category createRoot(ILangManager langManager) {
        Category root = new Category();
        root.setCode("home");
        root.setParentCode("home");
        List<Lang> langs = langManager.getLangs();
        ApsProperties titles = new ApsProperties();
        for (Lang lang : langs) {
            titles.setProperty(lang.getCode(), "Home");
        }
        root.setTitles(titles);
        return root;
    }

    private void initCache(Cache cache, List<Category> categories) throws EntException {
        Category root = null;
        Map<String, Category> categoryMap = new HashMap<>();
        for (Category cat : categories) {
            categoryMap.put(cat.getCode(), cat);
            if (cat.getCode().equals(cat.getParentCode())) {
                root = cat;
            }
        }
        for (Category cat : categories) {
            Category parent = categoryMap.get(cat.getParentCode());
            if (cat != root) {
                parent.addChildCode(cat.getCode());
            }
            cat.setParentCode(parent.getCode());
        }
        if (root == null) {
            throw new EntException("Error found in the category tree: undefined root");
        }
        this.insertObjectsOnCache(cache, root, categoryMap);
    }

    protected void insertObjectsOnCache(Cache cache, Category root, Map<String, Category> categoryMap) {
        cache.put(CATEGORY_ROOT_CACHE_NAME, root);
        Iterator<Category> iter = categoryMap.values().iterator();
        while (iter.hasNext()) {
            Category category = iter.next();
            cache.put(CATEGORY_CACHE_NAME_PREFIX + category.getCode(), category);
        }
    }

    @Override
    public Category getCategory(String code) {
        Category category = this.get(CATEGORY_CACHE_NAME_PREFIX + code, Category.class);
        if (null != category) {
            return category.clone();
        }
        return null;
    }

    @Override
    public void addCategory(Category category) {
        Cache cache = this.getCache();
        String code = category.getCode();
        cache.put(CATEGORY_CACHE_NAME_PREFIX + code, category);
        this.<List<String>>updateCachedValue(cache, CATEGORY_CODES_CACHE_NAME, codes -> {
            if (null == codes || codes.contains(code)) {
                return codes;
            }
            List<String> newCodes = new ArrayList<>(codes);
            newCodes.add(code);
            return newCodes;
        });
        this.updateParentChildren(cache, category.getParentCode(), children -> {
            if (ArrayUtils.contains(children, code)) {
                return children;
            }
            return ArrayUtils.add(children, code);
        });
    }

    @Override
    public void updateCategory(Category category) {
        Cache cache = this.getCache();
        cache.put(CATEGORY_CACHE_NAME_PREFIX + category.getCode(), category);
        this.checkRootModification(category, cache);
    }

    @Override
    public void deleteCategory(String code) {
        Cache cache = this.getCache();
        Category category = this.getCategory(code);
        if (null == category) {
            return;
        }
        this.updateParentChildren(cache, category.getParentCode(), children -> {
            if (!ArrayUtils.contains(children, code)) {
                return children;
            }
            return ArrayUtils.removeElement(children, code);
        });
        this.<List<String>>updateCachedValue(cache, CATEGORY_CODES_CACHE_NAME, codes -> {
            if (null == codes || !codes.contains(code)) {
                return codes;
            }
            List<String> newCodes = new ArrayList<>(codes);
            newCodes.remove(code);
            return newCodes;
        });
        cache.evict(CATEGORY_CACHE_NAME_PREFIX + code);
    }

    /**
     * Atomically replaces the children codes of the stored parent, and of the stored root when the parent is the root.
     */
    private void updateParentChildren(Cache cache, String parentCode, UnaryOperator<String[]> childrenUpdater) {
        UnaryOperator<Category> parentUpdater = parent -> {
            if (null == parent) {
                return parent;
            }
            String[] children = childrenUpdater.apply(parent.getChildrenCodes());
            if (children == parent.getChildrenCodes()) {
                return parent;
            }
            Category newParent = parent.clone();
            newParent.setChildrenCodes(children);
            return newParent;
        };
        Category parent = this.updateCachedValue(cache, CATEGORY_CACHE_NAME_PREFIX + parentCode, parentUpdater);
        if (null != parent && parent.isRoot()) {
            this.updateCachedValue(cache, CATEGORY_ROOT_CACHE_NAME, parentUpdater);
        }
    }

    private void checkRootModification(Category category, Cache cache) {
        if (category.isRoot()) {
            cache.put(CATEGORY_ROOT_CACHE_NAME, category);
        }
    }

    @Override
    public Category getRoot() {
        Category category = this.get(CATEGORY_ROOT_CACHE_NAME, Category.class);
        if (null != category) {
            return category.clone();
        }
        return null;
    }

    @Override
    protected String getCacheName() {
        return CATEGORY_MANAGER_CACHE_NAME;
    }

    @Override
    public void moveCategory(String categoryCode, String newParentCode) {
        Cache cache = this.getCache();
        Category categoryNode1 = this.getCategory(categoryCode);
        Category categoryNode2 = this.getCategory(newParentCode);

        Category oldParent = this.getCategory(categoryNode1.getParentCode());
        int index1 = Arrays.asList(oldParent.getChildrenCodes()).indexOf(categoryCode);
        String[] newChildren = ArrayUtils.remove(oldParent.getChildrenCodes(), index1);
        oldParent.setChildrenCodes(newChildren);
        cache.put(CATEGORY_CACHE_NAME_PREFIX + oldParent.getCode(), oldParent);

        String[] oldChildDest = categoryNode2.getChildrenCodes();
        categoryNode1.setParentCode(categoryNode2.getCode());
        cache.put(CATEGORY_CACHE_NAME_PREFIX + categoryNode1.getCode(), categoryNode1);

        String[] newChildren2 = ArrayUtils.add(oldChildDest, categoryNode1.getCode());
        categoryNode2.setChildrenCodes(newChildren2);
        cache.put(CATEGORY_CACHE_NAME_PREFIX + categoryNode2.getCode(), categoryNode2);
    }

    @Override
    public Map<String, Integer> getMoveNodeStatus() {
        Map<String, Integer>value = (Map<String, Integer>) this.get(CATEGORY_STATUS_CACHE_NAME, Map.class);
         return new HashMap<>(value);
    }

    @Override
    public void updateMoveNodeStatus(String beanName, Integer status) {
        Map<String, Integer> statusMap = this.getMoveNodeStatus();
        if (null == statusMap) {
            statusMap = new HashMap<>();
        }
        statusMap.put(beanName, status);
        this.getCache().put(CATEGORY_STATUS_CACHE_NAME, statusMap);
    }

}
