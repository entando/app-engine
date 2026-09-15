/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package com.agiletec.plugins.jpwebdynamicform.aps.system.services.message;

import com.agiletec.aps.system.common.SearchableFields;
import com.agiletec.aps.system.common.entity.AbstractEntitySearcherDAO;
import com.agiletec.aps.system.common.entity.IEntityManager;
import com.agiletec.aps.system.common.entity.model.ApsEntityRecord;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.plugins.jpwebdynamicform.aps.system.services.message.model.MessageRecordVO;
import org.entando.entando.ent.exception.EntException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of Data Access Object delegated for the Message searching operations.
 * @author E.Mezzano
 */
public class MessageSearcherDAO extends AbstractEntitySearcherDAO implements IMessageSearcherDAO {

	private static final String MESSAGEID = "messageid";
	private static final String MESSAGETYPE = "messagetype";

	/** The search keys this searcher accepts. */
	private static final SearchableFields SEARCHABLE_FIELDS = SearchableFields.columns(
			"username")
			.alias(IEntityManager.ENTITY_ID_FILTER_KEY, MESSAGEID)
			.alias(IEntityManager.ENTITY_TYPE_CODE_FILTER_KEY, MESSAGETYPE)
			.alias(IMessageManager.CREATION_DATE_FILTER_KEY, "creationdate");

	@Override
	protected ApsEntityRecord createRecord(ResultSet result) throws Throwable {
		MessageRecordVO record = new MessageRecordVO();
		record.setId(result.getString(MESSAGEID));
		record.setXml(result.getString("messagexml"));
		record.setTypeCode(result.getString(MESSAGETYPE));
		record.setUsername(result.getString("username"));
		record.setLangCode(result.getString("langcode"));
		record.setCreationDate(result.getTimestamp("creationdate"));
		return record;
	}

	@Override
	public List<String> searchId(EntitySearchFilter[] filters, boolean answered) throws EntException {
		Connection conn = null;
		List<String> idList = new ArrayList<>();
		PreparedStatement stat = null;
		ResultSet result = null;
		try {
			conn = this.getConnection();
			stat = this.buildStatement(filters, false, answered, conn);
			result = stat.executeQuery();
            while (result.next()) {
                String id = result.getString(this.getMasterTableIdFieldName());
                if (!idList.contains(id)) {
                    idList.add(id);
                }
            }
		} catch (Throwable t) {
			processDaoException(t, "Errore in caricamento lista id ", "searchId");
		} finally {
			closeDaoResources(result, stat, conn);
		}
		return idList;
	}

	protected PreparedStatement buildStatement(EntitySearchFilter[] filters, boolean selectAll, boolean answered, Connection conn) {
		String query = this.createMessageQueryString(filters, selectAll, answered);
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(query);
			int index = 0;
			index = this.addAttributeFilterStatementBlock(filters, index, stat);
            this.addMetadataFieldFilterStatementBlock(filters, index, stat);
        } catch (Throwable t) {
			processDaoException(t, "Errore in fase di creazione statement", "buildStatement");
		}
		return stat;
	}
    
	protected String createMessageQueryString(EntitySearchFilter[] filters, boolean selectAll, boolean answered) {
		StringBuffer query = this.createBaseQueryBlock(filters, false, selectAll);
		boolean hasAppendWhereClause = this.appendFullAttributeFilterQueryBlocks(filters, query, false);
		hasAppendWhereClause = this.appendMetadataFieldFilterQueryBlocks(filters, query, hasAppendWhereClause);
		this.appendAnsweredFilterQueryBlocks(answered, query, hasAppendWhereClause);
		boolean grouped = this.appendGroupByQueryBlock(filters, query, selectAll);
		appendOrderQueryBlocks(filters, query, false, grouped);
		return query.toString();
	}
	
	protected boolean appendAnsweredFilterQueryBlocks(boolean answered, StringBuffer query, boolean hasAppendWhereClause) {
		String masterTableName = this.getEntityMasterTableName();
		String masterTableIdFieldName = this.getEntityMasterTableIdFieldName();
		hasAppendWhereClause = this.verifyWhereClauseAppend(query, hasAppendWhereClause);
		if (answered) {
			query.append(masterTableName).append(".").append(masterTableIdFieldName)
					.append(" IN ( SELECT messageid FROM jpwebdynamicform_answers )");
		} else {
			query.append(masterTableName).append(".").append(masterTableIdFieldName)
					.append(" NOT IN ( SELECT messageid FROM jpwebdynamicform_answers )");
		}
		return hasAppendWhereClause;
	}
	
	@Override
	protected String getEntityMasterTableName() {
		return "jpwebdynamicform_messages";
	}

	@Override
	protected String getEntityMasterTableIdFieldName() {
		return MESSAGEID;
	}

	@Override
	protected String getEntityMasterTableIdTypeFieldName() {
		return MESSAGETYPE;
	}

	@Override
	protected String getEntitySearchTableName() {
		return "jpwebdynamicform_search";
	}

	@Override
	protected String getEntitySearchTableIdFieldName() {
		return MESSAGEID;
	}
	
	@Override
	protected String getEntityAttributeRoleTableName() {
		return "jpwebdynamicform_attroles";
	}
	
	@Override
	protected String getEntityAttributeRoleTableIdFieldName() {
		return MESSAGEID;
	}
	
	@Override
	protected SearchableFields getSearchableFields() {
		return SEARCHABLE_FIELDS;
	}
	
}
