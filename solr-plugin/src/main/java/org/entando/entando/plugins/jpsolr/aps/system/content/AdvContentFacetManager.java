/*
 * Copyright 2021-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpsolr.aps.system.content;

import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.plugins.jacms.aps.system.services.content.widget.UserFilterOptionBean;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.exception.RestServerError;
import org.entando.entando.aps.system.services.searchengine.FacetedContentsResult;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.web.common.exceptions.ValidationConflictException;
import org.entando.entando.web.common.model.Filter;
import org.entando.entando.web.common.model.FilterOperator;
import org.entando.entando.web.common.model.FilterType;
import org.entando.entando.plugins.jpsolr.aps.system.solr.ISolrSearchEngineManager;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFacetedContentsResult;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrSearchEngineFilter;
import org.entando.entando.plugins.jpsolr.web.content.model.SolrFilter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.entando.entando.plugins.jpsolr.conditions.SolrActive;
import org.entando.entando.plugins.jpsolr.web.content.model.AdvRestContentListRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author E.Santoboni
 */
@Service
@SolrActive(true)
public class AdvContentFacetManager implements IAdvContentFacetManager {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(AdvContentFacetManager.class);

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[a-zA-Z0-9_.:,-]+");
    private static final Pattern INJECTION_PATTERN = Pattern.compile("\\$\\{|%24%7B", Pattern.CASE_INSENSITIVE);

    private static final String INVALID_PARAMETER_CODE = "INVALID_PARAMETER";
    private static final String INVALID_PARAMETER_MESSAGE_KEY = "parameter.invalid";

    private final ICategoryManager categoryManager;
    private final ICmsSearchEngineManager searchEngineManager;
    private final IAuthorizationManager authorizationManager;
    private final ILangManager langManager;

    @Autowired
    public AdvContentFacetManager(ICategoryManager categoryManager, ICmsSearchEngineManager searchEngineManager,
            IAuthorizationManager authorizationManager, ILangManager langManager) {
        this.categoryManager = categoryManager;
        this.searchEngineManager = searchEngineManager;
        this.authorizationManager = authorizationManager;
        this.langManager = langManager;
    }

    @Override
    public SolrFacetedContentsResult getFacetResult(SearchEngineFilter[] baseFilters,
            List<String> facetNodeCodes, List<UserFilterOptionBean> beans, List<String> groupCodes)
            throws EntException {
        try {
            SearchEngineFilter[] filters = this.getFilters(baseFilters, beans);
            SearchEngineFilter[] categoryFilters = null;
            if (null != facetNodeCodes && !facetNodeCodes.isEmpty()) {
                List<SearchEngineFilter<String>> categoryFiltersList = facetNodeCodes.stream()
                        .filter(c -> this.categoryManager.getCategory(c) != null)
                        .map(c -> new SearchEngineFilter<>("category", false, c)).collect(Collectors.toList());
                categoryFilters = categoryFiltersList.toArray(new SearchEngineFilter[categoryFiltersList.size()]);
            }
            return (SolrFacetedContentsResult) this.searchEngineManager
                    .searchFacetedEntities(filters, categoryFilters, groupCodes);
        } catch (Exception ex) {
            throw new EntException("Error loading facet result", ex);
        }
    }

    @Override
    public FacetedContentsResult getFacetResult(SearchEngineFilter[] baseFilters,
            SearchEngineFilter[] facetNodeCodes, List<UserFilterOptionBean> beans, List<String> groupCodes)
            throws EntException {
        SearchEngineFilter[] filters = this.getFilters(baseFilters, beans);
        return this.searchEngineManager.searchFacetedEntities(filters, facetNodeCodes, groupCodes);
    }

    protected SearchEngineFilter[] getFilters(SearchEngineFilter[] baseFilters, List<UserFilterOptionBean> beans) {
        SearchEngineFilter[] filters = (null != baseFilters) ? baseFilters : new SearchEngineFilter[0];
        if (null != beans) {
            for (UserFilterOptionBean bean : beans) {
                SearchEngineFilter<?> sf = bean.extractFilter();
                if (null != sf) {
                    filters = ArrayUtils.add(filters, sf);
                }
            }
        }
        return filters;
    }

    @Override
    public SolrFacetedContentsResult getFacetedContents(AdvRestContentListRequest requestList, UserDetails user) {
        this.validateRequest(requestList);
        SolrFacetedContentsResult facetedResult;
        try {
            String langCode = StringUtils.isBlank(requestList.getLang())
                    ? this.langManager.getDefaultLang().getCode()
                    : requestList.getLang();
            SolrSearchEngineFilter[] searchFilters = requestList.extractFilters(langCode);
            SolrSearchEngineFilter[][] doubleFilters = requestList.extractDoubleFilters(langCode);
            if (null != searchFilters) {
                for (SolrSearchEngineFilter<?> searchFilter : searchFilters) {
                    SolrSearchEngineFilter[] filters = new SolrSearchEngineFilter[]{searchFilter};
                    doubleFilters = ArrayUtils.add(doubleFilters, filters);
                }
            }
            SolrSearchEngineFilter[] categorySearchFilters = requestList.extractCategoryFilters();
            List<String> userGroupCodes = this.getAllowedGroups(user);
            facetedResult = ((ISolrSearchEngineManager) this.searchEngineManager).searchFacetedEntities(
                    doubleFilters, categorySearchFilters, userGroupCodes);
        } catch (EntException ex) {
            throw new RestServerError("error in search contents", ex);
        }
        return facetedResult;
    }

    protected void validateRequest(AdvRestContentListRequest requestList) {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(requestList, "advContentSearchRequest");
        // lang
        if (!StringUtils.isBlank(requestList.getLang())) {
            boolean validLang = this.langManager.getLangs().stream()
                    .anyMatch(l -> l.getCode().equals(requestList.getLang()));
            if (!validLang) {
                bindingResult.rejectValue("lang", "INVALID_LANG_CODE",
                        new Object[]{}, "lang.code.invalid");
            }
        }
        // sort
        rejectIfUnsafeIdentifier(requestList.getSort(), "sort", bindingResult);
        // direction
        rejectIfUnsafeIdentifier(requestList.getDirection(), "direction", bindingResult);
        // text
        rejectIfInjection(requestList.getText(), "text", bindingResult);
        // searchOption
        rejectIfUnsafeIdentifier(requestList.getSearchOption(), "searchOption", bindingResult);
        // csvCategories
        if (null != requestList.getCsvCategories()) {
            for (String csv : requestList.getCsvCategories()) {
                rejectIfUnsafeIdentifier(csv, "csvCategories", bindingResult);
            }
        }
        // filters
        validateFilters(requestList.getFilters(), "filters", bindingResult);
        // doubleFilters
        if (null != requestList.getDoubleFilters()) {
            for (SolrFilter[] innerFilters : requestList.getDoubleFilters()) {
                validateFilters(innerFilters, "doubleFilters", bindingResult);
            }
        }
        if (bindingResult.hasErrors()) {
            throw new ValidationConflictException(bindingResult);
        }
    }

    private void validateFilters(Filter[] filters, String fieldPrefix,
            BeanPropertyBindingResult bindingResult) {
        if (null == filters) {
            return;
        }
        for (Filter filter : filters) {
            rejectIfUnsafeIdentifier(filter.getAttribute(), fieldPrefix + ".attribute", bindingResult);
            rejectIfUnsafeIdentifier(filter.getEntityAttr(), fieldPrefix + ".entityAttr", bindingResult);
            rejectIfUnsafeIdentifier(filter.getOperator(), fieldPrefix + ".operator", bindingResult);
            rejectIfUnsafeIdentifier(filter.getOrder(), fieldPrefix + ".order", bindingResult);
            rejectIfUnsafeIdentifier(filter.getType(), fieldPrefix + ".type", bindingResult);
            rejectIfInjection(filter.getValue(), fieldPrefix + ".value", bindingResult);
            if (null != filter.getAllowedValues()) {
                for (String av : filter.getAllowedValues()) {
                    rejectIfInjection(av, fieldPrefix + ".allowedValues", bindingResult);
                }
            }
            if (filter instanceof SolrFilter solrFilter) {
                rejectIfUnsafeIdentifier(solrFilter.getSearchOption(),
                        fieldPrefix + ".searchOption", bindingResult);
            }
            rejectIfInvalidBooleanFilter(filter, fieldPrefix, bindingResult);
        }
    }

    /**
     * Booleans have no meaningful range and only two valid values. Without this check a range
     * operator silently builds a nonsense query (SearcherDAO's string-range fallback), and
     * {@code Boolean.parseBoolean} silently coerces any non-"true" string (including a
     * three-state "none") to {@code false} instead of failing.
     */
    private void rejectIfInvalidBooleanFilter(Filter filter, String fieldPrefix,
            BeanPropertyBindingResult bindingResult) {
        if (!FilterType.BOOLEAN.getValue().equalsIgnoreCase(filter.getType())) {
            return;
        }
        String operator = filter.getOperator();
        if (FilterOperator.GREATER.getValue().equalsIgnoreCase(operator)
                || FilterOperator.LOWER.getValue().equalsIgnoreCase(operator)) {
            logger.warn("Rejected range operator '{}' on boolean filter in field '{}'", operator, fieldPrefix);
            bindingResult.rejectValue(null, INVALID_PARAMETER_CODE,
                    new Object[]{fieldPrefix + ".operator"}, INVALID_PARAMETER_MESSAGE_KEY);
        }
        rejectIfNotStrictBoolean(filter.getValue(), fieldPrefix + ".value", bindingResult);
        if (null != filter.getAllowedValues()) {
            for (String av : filter.getAllowedValues()) {
                rejectIfNotStrictBoolean(av, fieldPrefix + ".allowedValues", bindingResult);
            }
        }
    }

    private static void rejectIfNotStrictBoolean(String value, String field,
            BeanPropertyBindingResult bindingResult) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            logger.warn("Rejected non-boolean value in field '{}': '{}'", field, value);
            bindingResult.rejectValue(null, INVALID_PARAMETER_CODE,
                    new Object[]{field}, INVALID_PARAMETER_MESSAGE_KEY);
        }
    }

    private static void rejectIfUnsafeIdentifier(String value, String field,
            BeanPropertyBindingResult bindingResult) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        if (!SAFE_IDENTIFIER.matcher(value).matches()) {
            logger.warn("Rejected unsafe identifier in field '{}': '{}'", field, value);
            bindingResult.rejectValue(null, INVALID_PARAMETER_CODE,
                    new Object[]{field}, INVALID_PARAMETER_MESSAGE_KEY);
        }
    }

    private static void rejectIfInjection(String value, String field,
            BeanPropertyBindingResult bindingResult) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        if (INJECTION_PATTERN.matcher(value).find()) {
            logger.warn("Rejected injection pattern in field '{}': '{}'", field, value);
            bindingResult.rejectValue(null, INVALID_PARAMETER_CODE,
                    new Object[]{field}, INVALID_PARAMETER_MESSAGE_KEY);
        }
    }

    protected List<String> getAllowedGroups(UserDetails currentUser) {
        List<String> groupCodes = new ArrayList<>();
        if (null != currentUser) {
            List<Group> groups = this.authorizationManager.getUserGroups(currentUser);
            groupCodes.addAll(groups.stream().map(Group::getName).collect(Collectors.toList()));
        }
        groupCodes.add(Group.FREE_GROUP_NAME);
        return groupCodes;
    }

}
