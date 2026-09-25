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
package org.entando.entando.aps.system.services.userprofile;

import java.sql.ResultSet;

import org.entando.entando.aps.system.services.userprofile.model.UserProfileRecord;

import com.agiletec.aps.system.common.SearchableFields;
import com.agiletec.aps.system.common.entity.AbstractEntitySearcherDAO;
import com.agiletec.aps.system.common.entity.IEntityManager;
import com.agiletec.aps.system.common.entity.model.ApsEntityRecord;

/**
 * Data Access Object for Search of UserProfile Object. 
 * @author E.Santoboni
 */
public class UserProfileSearcherDAO extends AbstractEntitySearcherDAO {

	private static final String USERNAME = "username";
	private static final String PROFILETYPE = "profiletype";

	/** The search keys this searcher accepts. <code>username</code> is the master table's id column. */
	private static final SearchableFields SEARCHABLE_FIELDS = SearchableFields.columns(
			USERNAME,
			"publicprofile")
			.alias(IEntityManager.ENTITY_ID_FILTER_KEY, USERNAME)
			.alias(IEntityManager.ENTITY_TYPE_CODE_FILTER_KEY, PROFILETYPE);

	@Override
	protected ApsEntityRecord createRecord(ResultSet result) throws Throwable {
		UserProfileRecord record = new UserProfileRecord();
		record.setId(result.getString(USERNAME));
		record.setXml(result.getString("profilexml"));
		record.setTypeCode(result.getString(PROFILETYPE));
		record.setPublicProfile(result.getInt("publicprofile") == 1);
		return record;
	}
	
	@Override
	protected String getEntityMasterTableName() {
		return "authuserprofiles";
	}
	
	@Override
	protected String getEntityMasterTableIdFieldName() {
		return USERNAME;
	}
	
	@Override
	protected String getEntityMasterTableIdTypeFieldName() {
		return PROFILETYPE;
	}
	
	@Override
	protected String getEntitySearchTableName() {
		return "authuserprofilesearch";
	}
	
	@Override
	protected String getEntitySearchTableIdFieldName() {
		return USERNAME;
	}
	
	@Override
	protected String getEntityAttributeRoleTableName() {
		return "authuserprofileattrroles";
	}
	
	@Override
	protected String getEntityAttributeRoleTableIdFieldName() {
		return USERNAME;
	}
	
	@Override
	protected SearchableFields getSearchableFields() {
		return SEARCHABLE_FIELDS;
	}
	
}
