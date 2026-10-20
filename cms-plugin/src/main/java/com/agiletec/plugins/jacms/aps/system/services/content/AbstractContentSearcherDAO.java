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
package com.agiletec.plugins.jacms.aps.system.services.content;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.SearchableFields;
import com.agiletec.aps.system.common.entity.AbstractEntitySearcherDAO;
import com.agiletec.aps.system.common.entity.model.ApsEntityRecord;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.util.DateConverter;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.model.ContentRecordVO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;

/**
 * Abstract Data access object used to search contents.
 * @author E.Santoboni
 */
public abstract class AbstractContentSearcherDAO extends AbstractEntitySearcherDAO implements IContentSearcherDAO {

	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(AbstractContentSearcherDAO.class);

	private static final String CONTENTID = "contentid";
	private static final String CONTENTTYPE = "contenttype";

	/**
	 * The search keys this searcher accepts. Most name their column directly; the entity keys and the
	 * two whose column is spelled differently are declared as aliases. <code>group</code> is accepted
	 * because the chain this replaced accepted it - it is a code literal, not caller input, and keeping
	 * it makes this an exact translation of that chain rather than a silent correction to it.
	 */
	private static final SearchableFields SEARCHABLE_FIELDS = SearchableFields.columns(
			"descr",
			"status",
			"created",
			"published",
			"maingroup",
			"currentversion",
			"firsteditor",
			"lasteditor",
			"restriction",
			"group")
			.alias(IContentManager.ENTITY_ID_FILTER_KEY, CONTENTID)
			.alias(IContentManager.ENTITY_TYPE_CODE_FILTER_KEY, CONTENTTYPE)
			.alias(IContentManager.CONTENT_MODIFY_DATE_FILTER_KEY, "lastmodified")
			.alias(IContentManager.CONTENT_ONLINE_FILTER_KEY, "onlinexml");
    
    @Override
    public int countContents(String[] categories, boolean orClauseCategoryFilter, 
            EntitySearchFilter[] filters, Collection<String> userGroupCodes) {
		if (userGroupCodes == null || userGroupCodes.isEmpty()) {
			return 0;
		}
        Connection conn = null;
        int count = 0;
        PreparedStatement stat = null;
        ResultSet result = null;
        try {
            conn = this.getConnection();
            stat = this.buildStatement(filters, categories, 
                    orClauseCategoryFilter, userGroupCodes, true, false, conn);
            result = stat.executeQuery();
            if (result.next()) {
                count = result.getInt(1);
            }
        } catch (Throwable t) {
            _logger.error("Error while loading the count of IDs", t);
            throw new RuntimeException("Error while loading the count of IDs", t);
        } finally {
            closeDaoResources(result, stat, conn);
        }
        return count;
    }
    
    @Override
	public List<String> loadContentsId(String contentType, String[] categories, boolean orClauseCategoryFilter, 
			EntitySearchFilter[] filters, Collection<String> userGroupCodes) {
		if (!StringUtils.isBlank(contentType)) {
			EntitySearchFilter typeFilter = new EntitySearchFilter(IContentManager.ENTITY_TYPE_CODE_FILTER_KEY, false, contentType, false);
			filters = this.addFilter(filters, typeFilter);
		}
		return this.loadContentsId(categories, orClauseCategoryFilter, filters, userGroupCodes);
	}
    
    @Override
    public List<String> loadContentsId(String[] categories, 
			boolean orClauseCategoryFilter, EntitySearchFilter[] filters, Collection<String> userGroupCodes) {
		List<String> contentsId = new ArrayList<>();
		Connection conn = null;
		PreparedStatement stat = null;
		ResultSet result = null;
		try {
			conn = this.getConnection();
			stat = this.buildStatement(filters, categories, orClauseCategoryFilter, userGroupCodes, false, false, conn);
			result = stat.executeQuery();
            while (result.next()) {
                String id = result.getString(this.getMasterTableIdFieldName());
                if (!contentsId.contains(id)) {
                    contentsId.add(id);
                }
            }
		} catch (Throwable t) {
			_logger.error("Error loading contents id list",  t);
			throw new RuntimeException("Error loading contents id list", t);
		} finally {
			closeDaoResources(result, stat, conn);
		}
		return contentsId;
	}
	
	@Override
	protected SearchableFields getSearchableFields() {
		return SEARCHABLE_FIELDS;
	}
	
	protected PreparedStatement buildStatement(EntitySearchFilter[] filters,
			Collection<String> userGroupCodes, boolean selectAll, Connection conn) {
		return this.buildStatement(filters, null, false, userGroupCodes, false, selectAll, conn);
	}
	
	protected PreparedStatement buildStatement(EntitySearchFilter[] filters,
			String[] categories, Collection<String> userGroupCodes, boolean selectAll, Connection conn) {
		return this.buildStatement(filters, categories, false, userGroupCodes, false, selectAll, conn);
	}
	
	protected PreparedStatement buildStatement(EntitySearchFilter[] filters,
			String[] categories, boolean orClauseCategoryFilter, 
			Collection<String> userGroupCodes, boolean isCount, boolean selectAll, Connection conn) {
		Collection<String> groupsForSelect = this.getGroupsForSelect(userGroupCodes);

		String query = this.createQueryString(filters, null, categories, orClauseCategoryFilter, groupsForSelect, isCount, selectAll);
		//System.out.println("QUERY : " + query);
		PreparedStatement stat = null;
		try {
			stat = this.prepareStatement(conn, query, isCount);
			int index = 0;
			index = super.addAttributeFilterStatementBlock(filters, index, stat);
			index = this.addMetadataFieldFilterStatementBlock(filters, index, stat);
			if (groupsForSelect != null) {
				index = this.addGroupStatementBlock(groupsForSelect, index, stat);
			}
			if (categories != null) {
				index = this.addCategoryStatementBlock(categories, index, stat);
			}
		} catch (Throwable t) {
			_logger.error("Errore in fase di creazione statement",  t);
			throw new RuntimeException("Errore in fase di creazione statement", t);
			//processDaoException(t, "Errore in fase di creazione statement", "buildStatement");
		}
		return stat;
	}
	
	protected int addGroupStatementBlock(Collection<String> groupCodes, int index, PreparedStatement stat) throws Throwable {
		Iterator<String> groupIter = groupCodes.iterator();
		while (groupIter.hasNext()) {
			String groupName = groupIter.next();
			stat.setString(++index, groupName);
		}
		return index;
	}
	
	protected int addCategoryStatementBlock(String[] categories, int index, PreparedStatement stat) throws Throwable {
		if (null == categories) return index;
		for (int i = 0; i < categories.length; i++) {
			stat.setString(++index, categories[i]);
		}
		return index;
	}
	
	protected String createQueryString(EntitySearchFilter[] filters, Collection<String> groupsForSelect, boolean selectAll) {
		return this.createQueryString(filters, null, null, false, groupsForSelect, false, selectAll);
	}
	
	protected String createQueryString(EntitySearchFilter[] filters, 
			String[] categories, Collection<String> groupsForSelect, boolean selectAll) {
		return this.createQueryString(filters, null, categories, false, groupsForSelect, false, selectAll);
	}
	
	protected String createQueryString(EntitySearchFilter[] filters, String[] groups,
			String[] categories, boolean orClauseCategoryFilter, Collection<String> groupsForSelect, boolean isCount, boolean selectAll) {
		StringBuffer query = this.createBaseQueryBlock(filters, isCount, selectAll);
		boolean hasAppendWhereClause = this.appendFullAttributeFilterQueryBlocks(filters, query, false);
		hasAppendWhereClause = this.appendMetadataFieldFilterQueryBlocks(filters, query, hasAppendWhereClause);
		if (null != groupsForSelect && !groupsForSelect.isEmpty()) {
			hasAppendWhereClause = this.verifyWhereClauseAppend(query, hasAppendWhereClause);
			this.addGroupsQueryBlock(query, groupsForSelect);
		}
		if (null != categories && categories.length > 0) {
			hasAppendWhereClause = this.verifyWhereClauseAppend(query, hasAppendWhereClause);
			this.addCategoriesQueryBlock(query, categories, !orClauseCategoryFilter);
		}
		if (null != groups && groups.length > 0) {
			hasAppendWhereClause = this.verifyWhereClauseAppend(query, hasAppendWhereClause);
			this.addGroupsQueryBlock(query, groups);
		}
		boolean grouped = this.appendGroupByQueryBlock(filters, query, selectAll);
        if (!isCount) {
            this.appendOrderQueryBlocks(filters, query, false, grouped);
            this.appendLimitQueryBlock(filters, query);
		}
		return this.toQueryString(query, isCount);
	}
	
	protected void addGroupsQueryBlock(StringBuffer query, Collection<String> userGroupCodes) {
		query.append(" ( ");
		int size = userGroupCodes.size();
		for (int i=0; i<size; i++) {
			if (i!=0) query.append("OR ");
			query.append("contents.maingroup = ? ");
		}
		query.append(") ");
	}
	
	protected void addCategoriesQueryBlock(StringBuffer query, String[] categories, boolean andClause) {
		if (categories != null && categories.length > 0) {
			query.append(" ( ");
			for (int i=0; i<categories.length; i++) {
				if (i>0) {
					if (andClause) {
						query.append(" AND ");
					} else {
						query.append(" OR ");
					}
				}
				query.append(" contents.contentid IN (SELECT contentid FROM ")
					.append(this.getContentRelationsTableName()).append(" WHERE ")
					.append(this.getContentRelationsTableName()).append(".refcategory = ? ) ");
			}
			query.append(" ) ");
		}
	}

    protected void addGroupsQueryBlock(StringBuffer query, String[] groups) {
        if (groups != null && groups.length > 0) {
            query.append(" ( ");
            for (int i = 0; i < groups.length; i++) {
                if (i > 0) {
                    query.append(" AND ");
                }
                query.append(" contents.contentid IN (SELECT contentid FROM ")
                        .append(this.getContentRelationsTableName()).append(" WHERE ")
                        .append(this.getContentRelationsTableName()).append(".refgroup = ? ) ");
            }
            query.append(" ) ");
        }
    }

	protected Collection<String> getGroupsForSelect(Collection<String> userGroupCodes) {
		if (userGroupCodes != null && userGroupCodes.contains(Group.ADMINS_GROUP_NAME)) {
			return null;
		} else {
			Collection<String> groupsForSelect = new HashSet<>();
			if (userGroupCodes != null) groupsForSelect.addAll(userGroupCodes);
			return groupsForSelect;
		}
	}
	
	@Override
	protected ApsEntityRecord createRecord(ResultSet result) throws Throwable {
		ContentRecordVO contentVo = new ContentRecordVO();
		contentVo.setId(result.getString(CONTENTID));
		contentVo.setTypeCode(result.getString(CONTENTTYPE));
		contentVo.setDescription(result.getString("descr"));
		contentVo.setStatus(result.getString("status"));
		String xmlWork = result.getString("workxml");
        contentVo.setCreate(DateConverter.parseDate(result.getString("created"), JacmsSystemConstants.CONTENT_METADATA_DATE_FORMAT));
		contentVo.setModify(DateConverter.parseDate(result.getString("lastmodified"), JacmsSystemConstants.CONTENT_METADATA_DATE_FORMAT));
		contentVo.setPublish(DateConverter.parseDate(result.getString("published"), JacmsSystemConstants.CONTENT_METADATA_DATE_FORMAT));
		String xmlOnLine = result.getString("onlinexml");
        contentVo.setOnLine(!StringUtils.isBlank(xmlOnLine));
		contentVo.setSync(result.getInt("sync") == 1);
		String mainGroupCode = result.getString("maingroup");
		contentVo.setMainGroupCode(mainGroupCode);
		contentVo.setXmlWork(xmlWork);
		contentVo.setXmlOnLine(xmlOnLine);
		contentVo.setVersion(result.getString("currentversion"));
		contentVo.setLastEditor(result.getString("lasteditor"));
		contentVo.setRestriction(result.getString("restriction"));
		return contentVo;
	}
	
	@Override
	protected String getEntityMasterTableName() {
		return "contents";
	}
	@Override
	protected String getEntityMasterTableIdFieldName() {
		return CONTENTID;
	}
	@Override
	protected String getEntityMasterTableIdTypeFieldName() {
		return CONTENTTYPE;
	}
	
	protected abstract String getContentRelationsTableName();
	
}
