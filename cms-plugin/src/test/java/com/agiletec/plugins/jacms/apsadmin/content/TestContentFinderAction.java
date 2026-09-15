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
package com.agiletec.plugins.jacms.apsadmin.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.agiletec.aps.system.common.entity.ApsEntityManager;
import com.agiletec.aps.system.common.entity.IEntityTypesConfigurer;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.ThreeStateAttribute;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.apsadmin.content.util.AbstractBaseTestContentAction;
import org.apache.struts2.action.Action;
import org.junit.jupiter.api.Test;

/**
 * @author E.Santoboni
 */
class TestContentFinderAction extends AbstractBaseTestContentAction {
	
	@Test
    void testGetList() throws Throwable {
		String result = this.executeGetList("admin");
		assertEquals(Action.SUCCESS, result);
		List<String> contents = (List<String>) ((ContentFinderAction)this.getAction()).getContents();
		assertEquals(29, contents.size());
		
		result = this.executeGetList("editorCoach");
		assertEquals(Action.SUCCESS, result);
		contents = (List<String>) ((ContentFinderAction)this.getAction()).getContents();
		assertEquals(8, contents.size());
		
		result = this.executeGetList("editorCustomers");
		assertEquals(Action.SUCCESS, result);
		contents = (List<String>) ((ContentFinderAction)this.getAction()).getContents();
		assertEquals(2, contents.size());
		
		result = this.executeGetList("pageConfigCustomers");
		assertEquals("apslogin", result);
	}
	
	private String executeGetList(String currentUserName) throws Throwable {
		this.initAction("/do/jacms/Content", "list");
		this.setUserOnSession(currentUserName);
		return this.executeAction();
	}
	
	@Test
    void testPerformSearch_1() throws Throwable {
		Map<String, String> params = new HashMap<String, String>();
		this.executeSearch("admin", params);
		ContentFinderAction action = (ContentFinderAction) this.getAction();
		String[] order1 = {"BLT4", "BLT3", "BLT2", "BLT1", "ALL4", "ART112","ART122","ART121","ART120","ART111","ART179","EVN21",
				"EVN20","EVN41","EVN25","EVN24","EVN23","ART102","ART104","EVN103",
				"RAH101","EVN192","EVN191","RAH1","ART180","EVN194","EVN193","ART1","ART187"};
		List<String> contents = action.getContents();
		assertEquals(order1.length, contents.size());
		for (int i=0; i<contents.size(); i++) {
    		assertEquals(order1[i], contents.get(i));
    	}
		
		params.put("lastOrder", "DESC");
		params.put("lastGroupBy", "lastModified");
		params.put("groupBy", "lastModified");
		this.executeChangeOrder("admin", params);
		action = (ContentFinderAction) this.getAction();
		contents = action.getContents();
		assertEquals(order1.length, contents.size());
		for (int i=0; i<contents.size(); i++) {
    		assertEquals(order1[order1.length - i - 1], contents.get(i));
    	}
	}
	
	@Test
    void testPerformSearch_2() throws Throwable {
		Map<String, String> params = new HashMap<String, String>();
		this.executeSearch("supervisorCoach", params);
		ContentFinderAction action = (ContentFinderAction) this.getAction();
		String[] order1 = {"ART112", "ART111", "EVN41", "EVN25", "ART102", "ART104", "EVN103", "RAH101"};
		List<String> contents = action.getContents();
		assertEquals(order1.length, contents.size());
		for (int i=0; i<contents.size(); i++) {
    		assertEquals(order1[i], contents.get(i));
    	}
		params.put("lastOrder", "DESC");
		params.put("lastGroupBy", "lastModified");
		params.put("groupBy", "lastModified");
		this.executeChangeOrder("supervisorCoach", params);
		action = (ContentFinderAction) this.getAction();
		contents = action.getContents();
		assertEquals(order1.length, contents.size());
		for (int i=0; i<contents.size(); i++) {
    		assertEquals(order1[order1.length - i - 1], contents.get(i));
    	}
	}
	
	@Test
    void testPerformSearch_3() throws Throwable {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lastOrder", "ASC");
		params.put("lastGroupBy", "created");
		params.put("text", "desc");
		params.put("state", Content.STATUS_DRAFT);
		this.executeSearch("admin", params);
		
		ContentFinderAction action = (ContentFinderAction) this.getAction();
		String[] order = {"ART179", "ART187"};
		List<String> contents = action.getContents();
		assertEquals(order.length, contents.size());
		for (int i=0; i<contents.size(); i++) {
    		assertEquals(order[i], contents.get(i));
    	}
	}
	
	/**
	 * Test the newly added search criteria contentId, #1
	 */
	@Test
    void testPerformSearch_4() throws Throwable {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lastOrder", "ASC");
		params.put("lastGroupBy", "created");
		params.put("state", Content.STATUS_DRAFT);
		params.put("contentIdToken", "RA");
		this.executeSearch("admin", params);
		
		ContentFinderAction action = (ContentFinderAction) this.getAction();
		
		List<String> contents = action.getContents();
		String[] order = {"RAH1", "RAH101"};
		assertEquals(order.length, contents.size());
		for (int index=0; index < contents.size(); index++) {
    		assertEquals(order[index], contents.get(index));
    	}
	}
	
	/**
	 * Thest the newly added search criteria contentId, #2
	 */
	@Test
    void testPerformSearch_5() throws Throwable {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lastOrder", "DESC");
		params.put("lastGroupBy", "created");
		params.put("state", Content.STATUS_READY);
		params.put("contentIdToken", "r");
		this.executeSearch("admin", params);
		ContentFinderAction action = (ContentFinderAction) this.getAction();
		List<String> contents = action.getContents();
        assertEquals(1, contents.size());
		
		this.executeSearch("admin", params);
		action = (ContentFinderAction) this.getAction();
		contents = action.getContents();
		String[] order = {"ART180"};
		assertEquals(order.length, contents.size());
		for (int index=0; index < contents.size(); index++) {
    		assertEquals(order[index], contents.get(index));
    	}
	}

	@Test
    void testPerformSearch_6() throws Throwable {
		Map<String, String> params = new HashMap<String, String>();
		params.put("contentType", "ART");
		this.executeSearch("admin", params);
		ContentFinderAction action = (ContentFinderAction) this.getAction();
		String[] order1 = {"ART112","ART122","ART121","ART120",
				"ART111","ART179","ART102","ART104","ART180","ART1","ART187"};
		List<String> contents = action.getContents();
		assertEquals(order1.length, contents.size());
		for (int i=0; i<contents.size(); i++) {
    		assertEquals(order1[i], contents.get(i));
    	}
	}

	@Test
    void testPerformSearch_7() throws Throwable {
		Map<String, String> params = new HashMap<String, String>();
		params.put("contentType", "ART");
		params.put("Data_dateStartFieldName", "12/02/2009");
		this.executeSearch("admin", params);
		ContentFinderAction action = (ContentFinderAction) this.getAction();
		String[] order1 = {"ART121", "ART120", "ART179"};
		List<String> contents = action.getContents();
		assertEquals(order1.length, contents.size());
		for (int i=0; i<contents.size(); i++) {
    		assertEquals(order1[i], contents.get(i));
    	}
		
		params.put("categoryCode", "general_cat1");
		this.executeSearch("admin", params);
		action = (ContentFinderAction) this.getAction();
		contents = action.getContents();
		assertEquals(1, contents.size());
		assertTrue(contents.contains("ART179"));
	}
	
	@Test
    void testPerformSearch_8() throws Throwable {
		Map<String, String> params = new HashMap<String, String>();
		params.put("contentType", "ART");
		params.put("Data_dateStartFieldName", "12/02/2009");
		params.put("Data_dateEndFieldName", "02/06/2009");
		this.executeSearch("admin", params);
		ContentFinderAction action = (ContentFinderAction) this.getAction();
		String[] order1 = {"ART121", "ART120"};
		List<String> contents = action.getContents();
		assertEquals(order1.length, contents.size());
		for (int i=0; i<contents.size(); i++) {
    		assertEquals(order1[i], contents.get(i));
    	}
	}
	
	@Test
    void testPerformSearch_9() throws Throwable {
		Map<String, String> params = new HashMap<String, String>();
		params.put("contentType", "EVN");
		this.executeSearch("editorCoach", params);
		ContentFinderAction action = (ContentFinderAction) this.getAction();
		String[] order1 = {"EVN41", "EVN25", "EVN103"};
		List<String> contents = action.getContents();
		assertEquals(order1.length, contents.size());
		for (int i=0; i<contents.size(); i++) {
    		assertEquals(order1[i], contents.get(i));
    	}
		
		params.put("DataInizio_dateStartFieldName", "06/09/2007");
		params.put("DataInizio_dateEndFieldName", "02/05/2008");
		this.executeSearch("editorCoach", params);
		action = (ContentFinderAction) this.getAction();
		String[] order2 = {"EVN41", "EVN25"};
		contents = action.getContents();
		assertEquals(order2.length, contents.size());
		for (int i=0; i<contents.size(); i++) {
    		assertEquals(order2[i], contents.get(i));
    	}
		
		params.put("Titolo_textFieldName", "ci");
		this.executeSearch("editorCoach", params);
		action = (ContentFinderAction) this.getAction();
		contents = action.getContents();
		assertEquals(1, contents.size());
		assertTrue(contents.contains("EVN41"));
	}
	
	@Test
    void testPerformSearch_10() throws Throwable {
		Map<String, String> params = new HashMap<String, String>();
		params.put("categoryCode", "home");
		params.put("contentType", "EVN");
		this.executeSearch("admin", params);
		String[] order1 = {"EVN21", "EVN20", "EVN41", "EVN25", "EVN24", 
				"EVN23", "EVN103", "EVN192", "EVN191", "EVN194", "EVN193"};
		
		ContentFinderAction action = (ContentFinderAction) this.getAction();
		List<String> contents = action.getContents();
		assertEquals(order1.length, contents.size());
		for (int i=0; i<contents.size(); i++) {
    		assertEquals(order1[i], contents.get(i));
    	}
		
		params.put("categoryCode", "general");
		this.executeSearch("admin", params);
		action = (ContentFinderAction) this.getAction();
		String[] order2 = {"EVN25", "EVN23", "EVN192", "EVN193"};
		contents = action.getContents();
		assertEquals(order2.length, contents.size());
		for (int i=0; i<contents.size(); i++) {
    		assertEquals(order2[i], contents.get(i));
    	}
	}

	@Test
    void testPerformSearch_11() throws Throwable {
		Map<String, String> params = new HashMap<String, String>();
		params.put("ownerGroupName", "coach");
		this.executeSearch("admin", params);
		ContentFinderAction action = (ContentFinderAction) this.getAction();
		String[] order1 = {"ART112", "ART111", "EVN41", "EVN25", "ART104", "EVN103"};
		List<String> contents = action.getContents();
		assertEquals(order1.length, contents.size());
		for (int i=0; i<contents.size(); i++) {
    		assertEquals(order1[i], contents.get(i));
    	}
		
		params.put("ownerGroupName", "customers");
		this.executeSearch("admin", params);
		action = (ContentFinderAction) this.getAction();
		contents = action.getContents();
		String[] order2 = {"ART102", "RAH101"};
		assertEquals(order2.length, contents.size());
		for (int i=0; i<contents.size(); i++) {
    		assertEquals(order2[i], contents.get(i));
    	}
		
		params.put("ownerGroupName", "administrators");
		this.executeSearch("admin", params);
		action = (ContentFinderAction) this.getAction();
		contents = action.getContents();
		String[] order3 = {"ART122", "ART121", "ART120"};
		assertEquals(order3.length, contents.size());
		for (int i=0; i<contents.size(); i++) {
    		assertEquals(order3[i], contents.get(i));
    	}
	}

	@Test
    void testPerformSearch_12() throws Throwable {
		Map<String, String> params = new HashMap<String, String>();
		params.put("ownerGroupName", "coach");
		this.executeSearch("editorCoach", params);
		ContentFinderAction action = (ContentFinderAction) this.getAction();
		String[] order1 = {"ART112", "ART111", "EVN41", "EVN25", "ART104", "EVN103"};
		List<String> contents = action.getContents();
		assertEquals(order1.length, contents.size());
		for (int i=0; i<contents.size(); i++) {
    		assertEquals(order1[i], contents.get(i));
    	}
		
		params.put("ownerGroupName", "customers");
		this.executeSearch("editorCoach", params);
		action = (ContentFinderAction) this.getAction();
		contents = action.getContents();
		String[] order2 = {"ART102", "RAH101"};
		assertEquals(order2.length, contents.size());
		for (int i=0; i<contents.size(); i++) {
    		assertEquals(order2[i], contents.get(i));
    	}
		
		params.put("ownerGroupName", Group.ADMINS_GROUP_NAME);//Invalid group for coach
		this.executeSearch("editorCoach", params);
		action = (ContentFinderAction) this.getAction();
		contents = action.getContents();
		assertEquals(0, contents.size());
		
		params.put("ownerGroupName", Group.FREE_GROUP_NAME);//Invalid group for coach
		this.executeSearch("editorCoach", params);
		action = (ContentFinderAction) this.getAction();
		contents = action.getContents();
		assertEquals(0, contents.size());
	}

	@Test
	void testGetPaginatedContentsIdAfterLoadingResults() throws Throwable {
		this.initAction("/do/jacms/Content", "results");
		this.setUserOnSession("admin");
		String result = this.executeAction();
		assertEquals(Action.SUCCESS, result);
		ContentFinderAction action = (ContentFinderAction) this.getAction();
		action.getPaginatedContentsId(10);
	}

	/**
	 * End-to-end: a Composite-nested boolean made searchable through the content type is (a) offered by
	 * the search form as a path-keyed criterion and (b) usable to restrict the search - the submitted
	 * form field "<composite>_<boolean>_booleanFieldName" reaches the DB searcher and filters the list.
	 */
	@Test
	void testPerformSearchByNestedCompositeBoolean() throws Throwable {
		this.setNestedBooleanSearchable("ALL", true);
		List<String> added = new ArrayList<>();
		try {
			String trueId = this.createAllCloneWithNestedBoolean(Boolean.TRUE, added);
			String falseId = this.createAllCloneWithNestedBoolean(Boolean.FALSE, added);

			// (a) the form now offers the nested boolean under its path key
			Map<String, String> setType = new HashMap<>();
			setType.put("contentType", "ALL");
			this.executeSearch("admin", setType);
			ContentFinderAction action = (ContentFinderAction) this.getAction();
			assertTrue(this.offersAttribute(action, "Composite_Boolean"),
					"the search form should expose the nested boolean 'Composite_Boolean'");

			// (b) submitting the nested boolean field restricts the results
			Map<String, String> params = new HashMap<>();
			params.put("contentType", "ALL");
			params.put("Composite_Boolean_booleanFieldName", "true");
			this.executeSearch("admin", params);
			List<String> contents = ((ContentFinderAction) this.getAction()).getContents();
			assertTrue(contents.contains(trueId));
			assertFalse(contents.contains(falseId));

			params.put("Composite_Boolean_booleanFieldName", "false");
			this.executeSearch("admin", params);
			contents = ((ContentFinderAction) this.getAction()).getContents();
			assertTrue(contents.contains(falseId));
			assertFalse(contents.contains(trueId));
		} finally {
			for (String id : added) {
				this.getContentManager().deleteContent(id);
			}
			this.setNestedBooleanSearchable("ALL", false);
		}
	}

	private boolean offersAttribute(ContentFinderAction action, String name) {
		return action.getSearchableAttributeRefs().stream()
				.anyMatch(ref -> name.equals(ref.key()));
	}

	private void setNestedBooleanSearchable(String typeCode, boolean searchable) throws Throwable {
		Content prototype = this.getContentManager().createContentType(typeCode);
		((CompositeAttribute) prototype.getAttribute("Composite")).getAttribute("Boolean").setSearchable(searchable);
		((IEntityTypesConfigurer) this.getContentManager()).updateEntityPrototype(prototype);
		this.getContentManager().reloadEntitiesReferences(typeCode);
		waitThreads(ApsEntityManager.RELOAD_REFERENCES_THREAD_NAME_PREFIX);
	}

	private String createAllCloneWithNestedBoolean(Boolean value, List<String> added) throws Throwable {
		Content clone = this.getContentManager().loadContent("ALL4", false);
		clone.setId(null);
		BooleanAttribute nested = (BooleanAttribute) ((CompositeAttribute) clone.getAttribute("Composite")).getAttribute("Boolean");
		nested.setBooleanValue(value);
		this.getContentManager().saveContent(clone);
		added.add(clone.getId());
		this.getContentManager().insertOnLineContent(clone);
		return clone.getId();
	}

	/**
	 * End-to-end for the ThreeState "Not set" search option: submitting
	 * {@code <attr>_booleanFieldName=none} restricts the list to contents whose ThreeState is unset
	 * (no search record), distinct from "true"/"false" and from "Any" (no filter).
	 */
	@Test
	void testPerformSearchByThreeStateNotSet() throws Throwable {
		this.setThreeStateSearchable("ALL", true);
		List<String> added = new ArrayList<>();
		try {
			String trueId = this.createAllCloneWithThreeState(Boolean.TRUE, added);
			String falseId = this.createAllCloneWithThreeState(Boolean.FALSE, added);
			String unsetId = this.createAllCloneWithThreeState(null, added);

			Map<String, String> params = new HashMap<>();
			params.put("contentType", "ALL");
			params.put("ThreeState_booleanFieldName", "none");
			this.executeSearch("admin", params);
			List<String> contents = ((ContentFinderAction) this.getAction()).getContents();
			assertTrue(contents.contains(unsetId));
			assertFalse(contents.contains(trueId));
			assertFalse(contents.contains(falseId));

			params.put("ThreeState_booleanFieldName", "true");
			this.executeSearch("admin", params);
			contents = ((ContentFinderAction) this.getAction()).getContents();
			assertTrue(contents.contains(trueId));
			assertFalse(contents.contains(unsetId));
			assertFalse(contents.contains(falseId));
		} finally {
			for (String id : added) {
				this.getContentManager().deleteContent(id);
			}
			this.setThreeStateSearchable("ALL", false);
		}
	}

	private void setThreeStateSearchable(String typeCode, boolean searchable) throws Throwable {
		Content prototype = this.getContentManager().createContentType(typeCode);
		prototype.getAttribute("ThreeState").setSearchable(searchable);
		((IEntityTypesConfigurer) this.getContentManager()).updateEntityPrototype(prototype);
		this.getContentManager().reloadEntitiesReferences(typeCode);
		waitThreads(ApsEntityManager.RELOAD_REFERENCES_THREAD_NAME_PREFIX);
	}

	private String createAllCloneWithThreeState(Boolean value, List<String> added) throws Throwable {
		Content clone = this.getContentManager().loadContent("ALL4", false);
		clone.setId(null);
		((ThreeStateAttribute) clone.getAttribute("ThreeState")).setBooleanValue(value);
		this.getContentManager().saveContent(clone);
		added.add(clone.getId());
		this.getContentManager().insertOnLineContent(clone);
		return clone.getId();
	}

	private void executeSearch(String currentUserName, Map<String, String> params) throws Throwable {
		this.initAction("/do/jacms/Content", "search");
		this.setUserOnSession(currentUserName);
		this.addParameters(params);
		String result = this.executeAction();
		assertEquals(Action.SUCCESS, result);
	}
	
	private void executeChangeOrder(String currentUserName, Map<String, String> params) throws Throwable {
		this.initAction("/do/jacms/Content", "changeOrder");
		this.setUserOnSession(currentUserName);
		this.addParameters(params);
		String result = this.executeAction();
		assertEquals(Action.SUCCESS, result);
	}
	
	@Test
    void testSearchWithWrongStatus() throws Throwable {
		Map<String, String> params = new HashMap<String, String>();
		params.put("lastOrder", "ASC");
		params.put("lastGroupBy", "created");
		params.put("text", "desc");
		params.put("state", "wrongStatus");
		this.executeSearch("admin", params);
		
		ContentFinderAction action = (ContentFinderAction) this.getAction();
		List<String> contents = action.getContents();
		assertEquals(0, contents.size());
	}
	
}