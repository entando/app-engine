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
package com.agiletec.plugins.jacms.apsadmin.content.attribute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoListAttribute;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import com.agiletec.aps.system.common.tree.ITreeNode;
import com.agiletec.apsadmin.portal.PageTreeAction;
import com.agiletec.apsadmin.system.ApsAdminSystemConstants;
import com.agiletec.apsadmin.system.ITreeAction;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.content.model.SymbolicLink;
import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.LinkAttribute;
import com.agiletec.plugins.jacms.apsadmin.content.attribute.action.link.PageLinkAction;
import com.agiletec.plugins.jacms.apsadmin.content.attribute.action.link.helper.ILinkAttributeActionHelper;
import com.agiletec.plugins.jacms.apsadmin.content.util.AbstractBaseTestContentAction;
import org.apache.struts2.action.Action;
import org.apache.struts2.ActionSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author E.Santoboni
 */
class TestPageLinkAction extends AbstractBaseTestContentAction {

	@Test
    void testConfigPageLink_1() throws Throwable {
		String contentOnSessionMarker = this.initJoinLinkTest("admin", "ART1", "VediAnche", "it");
		this.initContentAction("/do/jacms/Content/Link", "configPageLink", contentOnSessionMarker);
		String result = this.executeAction();
		assertEquals(Action.SUCCESS, result);
		ITreeNode root = ((PageTreeAction) this.getAction()).getAllowedTreeRootNode();
		assertNotNull(root);
		assertEquals("homepage", root.getCode());
		assertEquals(3, root.getChildrenCodes().length);
		ITreeNode showableRoot = ((PageTreeAction) this.getAction()).getShowableTree();
		assertEquals("homepage", showableRoot.getCode());
		assertEquals(0, showableRoot.getChildrenCodes().length);
	}

	@Test
    void testConfigPageLink_2() throws Throwable {
		String contentOnSessionMarker = this.initJoinLinkTest("admin", "ART102", "VediAnche", "it");
		this.initContentAction("/do/jacms/Content/Link", "configPageLink", contentOnSessionMarker);
		String result = this.executeAction();
		assertEquals(Action.SUCCESS, result);
		ITreeNode root = ((PageTreeAction) this.getAction()).getAllowedTreeRootNode();
		assertNotNull(root);
		assertEquals("homepage", root.getCode());
		assertEquals(4, root.getChildrenCodes().length);
		ITreeNode showableRoot = ((PageTreeAction) this.getAction()).getShowableTree();
		assertEquals("homepage", showableRoot.getCode());
		assertEquals(0, showableRoot.getChildrenCodes().length);
	}

	@Test
    void testOpenPageNode_1() throws Throwable {
		String contentOnSessionMarker = this.initJoinLinkTest("admin", "ART102", "VediAnche", "it");
		this.initContentAction("/do/jacms/Content/Link", "openCloseTreeNode", contentOnSessionMarker);
		this.addParameter("treeNodeActionMarkerCode", ITreeAction.ACTION_MARKER_OPEN);
		this.addParameter("targetNode", "homepage");
		String result = this.executeAction();
		assertEquals(Action.SUCCESS, result);
		this.checkTestOpenPageTree_ART102();
	}

	@Test
    void testOpenPageNode_2() throws Throwable {
		String contentOnSessionMarker = this.initJoinLinkTest("editorCustomers", "ART102", "VediAnche", "it");
		this.initContentAction("/do/jacms/Content/Link", "openCloseTreeNode", contentOnSessionMarker);
		this.addParameter("treeNodeActionMarkerCode", ITreeAction.ACTION_MARKER_OPEN);
		this.addParameter("targetNode", "homepage");
		String result = this.executeAction();
		assertEquals(Action.SUCCESS, result);
		this.checkTestOpenPageTree_ART102();
	}

	private void checkTestOpenPageTree_ART102() throws Throwable {
		ITreeNode root = ((PageTreeAction) this.getAction()).getAllowedTreeRootNode();
		assertNotNull(root);
		assertEquals("homepage", root.getCode());
		assertEquals(4, root.getChildrenCodes().length);
		ITreeNode showableRoot = ((PageTreeAction) this.getAction()).getShowableTree();
		assertEquals("homepage", showableRoot.getCode());
		assertEquals(4, showableRoot.getChildrenCodes().length);
	}

	void testFailureJoinPageLink_1() throws Throwable {
		String contentOnSessionMarker = this.initJoinLinkTest("admin", "ART1", "VediAnche", "it");
		this.initContentAction("/do/jacms/Content/Link", "joinPageLink", contentOnSessionMarker);
		this.addParameter("linkType", String.valueOf(SymbolicLink.PAGE_TYPE));
		String result = this.executeAction();
		assertEquals(Action.INPUT, result);
		Map<String, List<String>> fieldErrors = this.getAction().getFieldErrors();
		assertEquals(1, fieldErrors.size());
		List<String> typeFieldErrors = fieldErrors.get("selectedNode");
		assertEquals(1, typeFieldErrors.size());
	}

	@Test
    void testFailureJoinPageLink_2() throws Throwable {
		String contentOnSessionMarker = this.initJoinLinkTest("admin", "ART1", "VediAnche", "it");
		this.initContentAction("/do/jacms/Content/Link", "joinPageLink", contentOnSessionMarker);
		this.addParameter("linkType", String.valueOf(SymbolicLink.PAGE_TYPE));
		this.addParameter("selectedNode", "");
		String result = this.executeAction();
		assertEquals(Action.INPUT, result);
		Map<String, List<String>> fieldErrors = this.getAction().getFieldErrors();
		assertEquals(1, fieldErrors.size());
		List<String> typeFieldErrors = fieldErrors.get("selectedNode");
		assertEquals(1, typeFieldErrors.size());
	}

	@Test
    void testFailureJoinPageLink_3() throws Throwable {
		String contentOnSessionMarker = this.initJoinLinkTest("admin", "ART1", "VediAnche", "it");
		this.initContentAction("/do/jacms/Content/Link", "joinPageLink", contentOnSessionMarker);
		this.addParameter("linkType", String.valueOf(SymbolicLink.PAGE_TYPE));
		this.addParameter("selectedNode", "wrongPageCode");
		String result = this.executeAction();
		assertEquals(Action.INPUT, result);
		Map<String, List<String>> fieldErrors = this.getAction().getFieldErrors();
		assertEquals(1, fieldErrors.size());
		List<String> typeFieldErrors = fieldErrors.get("selectedNode");
		assertEquals(1, typeFieldErrors.size());
	}
    
	@Test
    void testJoinRemovePageLinkInSimpleAttribute() throws Throwable {
        String contentOnSessionMarker = this.initJoinLinkTest("admin", "ART1", "VediAnche", "it");
		this.initContentAction("/do/jacms/Content/Link", "joinPageLink", contentOnSessionMarker);
		this.addParameter("linkType", String.valueOf(SymbolicLink.PAGE_TYPE));
		this.addParameter("selectedNode", "pagina_11");
        this.addParameter("linkAttributeRel", "it rel value");
        this.addParameter("linkAttributeTarget", "it target value");
        this.addParameter("linkAttributeHRefLang", "it hrefLang value");
        
		String result = this.executeAction();
		assertEquals(Action.SUCCESS, result);
		Content content = this.getContentOnEdit(contentOnSessionMarker);
		LinkAttribute attribute = (LinkAttribute) content.getAttribute("VediAnche");
		SymbolicLink symbolicLinkIt = attribute.getSymbolicLinks().get("it");
		assertNotNull(symbolicLinkIt);
		assertEquals("pagina_11", symbolicLinkIt.getPageDestination());
        SymbolicLink symbolicLinkEn = attribute.getSymbolicLinks().get("en");
        Assertions.assertNull(symbolicLinkEn);
        Map<String, String> itProperties = attribute.getLinksProperties().get("it");
        assertNotNull(itProperties);
        assertEquals(3, itProperties.size());
        assertEquals("it rel value", itProperties.get(LinkAttribute.REL_ATTRIBUTE));
        assertEquals("it target value", itProperties.get(LinkAttribute.TARGET_ATTRIBUTE));
        assertEquals("it hrefLang value", itProperties.get(LinkAttribute.HREFLANG_ATTRIBUTE));
        Map<String, String> enProperties = attribute.getLinksProperties().get("en");
        assertNull(enProperties);
        
        contentOnSessionMarker = this.initJoinLinkTest("admin", "ART1", "VediAnche", "en", false);
        
        this.initContentAction("/do/jacms/Content/Link", "joinPageLink", contentOnSessionMarker);
		this.addParameter("linkType", String.valueOf(SymbolicLink.PAGE_TYPE));
		this.addParameter("selectedNode", "pagina_12");
        this.addParameter("linkAttributeRel", "en rel value");
        this.addParameter("linkAttributeTarget", "en target value");
        this.addParameter("linkAttributeHRefLang", "en hrefLang value");
		result = this.executeAction();
		assertEquals(Action.SUCCESS, result);
		content = this.getContentOnEdit(contentOnSessionMarker);
		attribute = (LinkAttribute) content.getAttribute("VediAnche");
		symbolicLinkIt = attribute.getSymbolicLinks().get("it");
		assertNotNull(symbolicLinkIt);
		assertEquals("pagina_11", symbolicLinkIt.getPageDestination());
        symbolicLinkEn = attribute.getSymbolicLinks().get("en");
        Assertions.assertNotNull(symbolicLinkEn);
        assertEquals("pagina_12", symbolicLinkEn.getPageDestination());
        itProperties = attribute.getLinksProperties().get("it");
        assertNotNull(itProperties);
        assertEquals(3, itProperties.size());
        assertEquals("it rel value", itProperties.get(LinkAttribute.REL_ATTRIBUTE));
        assertEquals("it target value", itProperties.get(LinkAttribute.TARGET_ATTRIBUTE));
        assertEquals("it hrefLang value", itProperties.get(LinkAttribute.HREFLANG_ATTRIBUTE));
        enProperties = attribute.getLinksProperties().get("en");
        assertNotNull(enProperties);
        assertEquals(3, enProperties.size());
        assertEquals("en rel value", enProperties.get(LinkAttribute.REL_ATTRIBUTE));
        assertEquals("en target value", enProperties.get(LinkAttribute.TARGET_ATTRIBUTE));
        assertEquals("en hrefLang value", enProperties.get(LinkAttribute.HREFLANG_ATTRIBUTE));
        attribute.setRenderingLang("it");
        assertEquals("it rel value", attribute.getRel());
        attribute.setRenderingLang("en");
        assertEquals("en target value", attribute.getTarget());
        assertEquals("en hrefLang value", attribute.getHrefLang());
        attribute.setRenderingLang("it");
        
        this.initContentAction("/do/jacms/Content", "removeLink", contentOnSessionMarker);
		this.addParameter("attributeName", "VediAnche");
		this.addParameter("langCodeOfLink", "en");
		result = this.executeAction();
		assertEquals(Action.SUCCESS, result);
        
        content = this.getContentOnEdit(contentOnSessionMarker);
		attribute = (LinkAttribute) content.getAttribute("VediAnche");
		symbolicLinkIt = attribute.getSymbolicLinks().get("it");
		assertNotNull(symbolicLinkIt);
		assertEquals("pagina_11", symbolicLinkIt.getPageDestination());
        symbolicLinkEn = attribute.getSymbolicLinks().get("en");
        Assertions.assertNull(symbolicLinkEn);
        itProperties = attribute.getLinksProperties().get("it");
        assertNotNull(itProperties);
        assertEquals(3, itProperties.size());
        enProperties = attribute.getLinksProperties().get("en");
        assertNull(enProperties);
        
        assertEquals("it rel value", attribute.getRel());
        attribute.setRenderingLang("en");
        assertEquals("it target value", attribute.getTarget());
	}
    
	@Test
    void testJoinPageLinkInListAttribute() throws Throwable {
        this.executeEdit("ALL4", "admin");
        String contentOnSessionMarker = this.extractSessionMarker("ALL4", ApsAdminSystemConstants.EDIT);
        this.initContentAction("/do/jacms/Content", "addListElement", contentOnSessionMarker);
        this.addParameter("attributeName", "MonoLLink");
		this.addParameter("listLangCode", "it");
        String result = this.executeAction();
		assertEquals("chooseLink", result);
        Content content = this.getContentOnEdit(contentOnSessionMarker);
        MonoListAttribute monolist = (MonoListAttribute) content.getAttribute("MonoLLink");
        assertEquals(3, monolist.getAttributes().size());
        Map<String, String> properties = Map.of("attributeName", "MonoLLink", "langCodeOfLink", "it", "elementIndex", "2");
        this.executeChooseLink(properties, contentOnSessionMarker);
		this.initContentAction("/do/jacms/Content/Link", "joinPageLink", contentOnSessionMarker);
		this.addParameter("linkType", String.valueOf(SymbolicLink.PAGE_TYPE));
		this.addParameter("selectedNode", "pagina_11");
        this.addParameter("linkAttributeRel", "ml it rel value");
        this.addParameter("linkAttributeTarget", "ml it target value");
        this.addParameter("linkAttributeHRefLang", "ml it hrefLang value");
        result = this.executeAction();
		assertEquals(Action.SUCCESS, result);
		content = this.getContentOnEdit(contentOnSessionMarker);
        monolist = (MonoListAttribute) content.getAttribute("MonoLLink");
		LinkAttribute attribute = (LinkAttribute) monolist.getAttribute(2);
		SymbolicLink symbolicLinkIt = attribute.getSymbolicLinks().get("it");
		assertNotNull(symbolicLinkIt);
		assertEquals("pagina_11", symbolicLinkIt.getPageDestination());
        SymbolicLink symbolicLinkEn = attribute.getSymbolicLinks().get("en");
        Assertions.assertNull(symbolicLinkEn);
        Map<String, String> itProperties = attribute.getLinksProperties().get("it");
        assertNotNull(itProperties);
        assertEquals(3, itProperties.size());
        assertEquals("ml it rel value", itProperties.get(LinkAttribute.REL_ATTRIBUTE));
        assertEquals("ml it target value", itProperties.get(LinkAttribute.TARGET_ATTRIBUTE));
        assertEquals("ml it hrefLang value", itProperties.get(LinkAttribute.HREFLANG_ATTRIBUTE));
        Map<String, String> enProperties = attribute.getLinksProperties().get("en");
        assertNull(enProperties);
        
        properties = Map.of("attributeName", "MonoLLink", "langCodeOfLink", "en", "elementIndex", "1");
        this.executeChooseLink(properties, contentOnSessionMarker);
        
		this.initContentAction("/do/jacms/Content/Link", "joinPageLink", contentOnSessionMarker);
		this.addParameter("linkType", String.valueOf(SymbolicLink.PAGE_TYPE));
		this.addParameter("selectedNode", "pagina_12");
        
		result = this.executeAction();
		assertEquals(Action.SUCCESS, result);
		content = this.getContentOnEdit(contentOnSessionMarker);
        monolist = (MonoListAttribute) content.getAttribute("MonoLLink");
		attribute = (LinkAttribute) monolist.getAttribute(1);
		symbolicLinkIt = attribute.getSymbolicLinks().get("it");
		assertNotNull(symbolicLinkIt);
		assertNull(symbolicLinkIt.getPageDestination());
        assertEquals("http://www.entando.com", symbolicLinkIt.getUrlDest());
        symbolicLinkEn = attribute.getSymbolicLinks().get("en");
        Assertions.assertNotNull(symbolicLinkEn);
        assertEquals("pagina_12", symbolicLinkEn.getPageDestination());
	}
    
    
    @Test
    void testJoinPageLinkInCompositeAttribute() throws Throwable {
        this.executeEdit("ALL4", "admin");
        String contentOnSessionMarker = this.extractSessionMarker("ALL4", ApsAdminSystemConstants.EDIT);
        
        Map<String, String> properties = Map.of("parentAttributeName", "Composite", 
                "attributeName", "Link", "langCodeOfLink", "en");
        this.executeChooseLink(properties, contentOnSessionMarker);
		this.initContentAction("/do/jacms/Content/Link", "joinPageLink", contentOnSessionMarker);
		this.addParameter("linkType", String.valueOf(SymbolicLink.PAGE_TYPE));
		this.addParameter("selectedNode", "pagina_11");
        this.addParameter("linkAttributeRel", "comp en rel value");
        this.addParameter("linkAttributeTarget", "comp en target value");
        this.addParameter("linkAttributeHRefLang", "comp en hrefLang value");
        String result = this.executeAction();
		assertEquals(Action.SUCCESS, result);
		Content content = this.getContentOnEdit(contentOnSessionMarker);
        CompositeAttribute composite = (CompositeAttribute) content.getAttribute("Composite");
		LinkAttribute attribute = (LinkAttribute) composite.getAttribute("Link");
		SymbolicLink symbolicLinkIt = attribute.getSymbolicLinks().get("it");
		assertNotNull(symbolicLinkIt);
		assertNull(symbolicLinkIt.getPageDestination());
        assertEquals("http://www.google.com", symbolicLinkIt.getUrlDest());
        Map<String, String> itProperties = attribute.getLinksProperties().get("it");
        assertNull(itProperties);
        SymbolicLink symbolicLinkEn = attribute.getSymbolicLinks().get("en");
        Assertions.assertNotNull(symbolicLinkEn);
        assertEquals("pagina_11", symbolicLinkEn.getPageDestination());
        Map<String, String> enProperties = attribute.getLinksProperties().get("en");
        assertNotNull(enProperties);
        assertEquals(3, enProperties.size());
        assertEquals("comp en rel value", enProperties.get(LinkAttribute.REL_ATTRIBUTE));
        assertEquals("comp en target value", enProperties.get(LinkAttribute.TARGET_ATTRIBUTE));
        assertEquals("comp en hrefLang value", enProperties.get(LinkAttribute.HREFLANG_ATTRIBUTE));
        this.executeChooseLink(properties, contentOnSessionMarker);
        this.initContentAction("/do/jacms/Content/Link", "configPageLink", contentOnSessionMarker);
        result = this.executeAction();
        assertEquals(Action.SUCCESS, result);
        ActionSupport action = this.getAction();
        Assertions.assertTrue(action instanceof PageLinkAction);
        assertEquals(enProperties.get(LinkAttribute.REL_ATTRIBUTE), ((PageLinkAction) action).getLinkAttributeRel());
        assertEquals(enProperties.get(LinkAttribute.TARGET_ATTRIBUTE), ((PageLinkAction) action).getLinkAttributeTarget());
        assertEquals(enProperties.get(LinkAttribute.HREFLANG_ATTRIBUTE), ((PageLinkAction) action).getLinkAttributeHRefLang());
	}
    
    private String initJoinLinkTest(String username, String contentId, String simpleLinkAttributeName, String initEditContent) throws Throwable {
        return this.initJoinLinkTest(username, contentId, simpleLinkAttributeName, initEditContent, true);
    }
    
    private String initJoinLinkTest(String username, String contentId, String simpleLinkAttributeName, String langCode, boolean initEditContent) throws Throwable {
        Map<String, String> properties = Map.of("attributeName", simpleLinkAttributeName, "langCodeOfLink", langCode);
        String contentOnSessionMarker = this.extractSessionMarker(contentId, ApsAdminSystemConstants.EDIT);
        if (initEditContent) {
            this.executeEdit(contentId, username);
        }
        this.executeChooseLink(properties, contentOnSessionMarker);
        HttpSession session = this.getRequest().getSession();
		assertEquals(properties.get("attributeName"), session.getAttribute(ILinkAttributeActionHelper.ATTRIBUTE_NAME_SESSION_PARAM));
		assertEquals(properties.get("langCodeOfLink"), session.getAttribute(ILinkAttributeActionHelper.LINK_LANG_CODE_SESSION_PARAM));
        return contentOnSessionMarker;
    }
    
	private void executeChooseLink(Map<String, String> properties, String contentOnSessionMarker) throws Throwable {
        this.initContentAction("/do/jacms/Content", "chooseLink", contentOnSessionMarker);
        properties.entrySet().forEach(e -> this.addParameter(e.getKey(), e.getValue()));
        String result = this.executeAction();
		assertEquals(Action.SUCCESS, result);
	}

}
