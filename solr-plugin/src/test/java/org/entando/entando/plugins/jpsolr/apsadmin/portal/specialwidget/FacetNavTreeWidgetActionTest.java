package org.entando.entando.plugins.jpsolr.apsadmin.portal.specialwidget;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import com.agiletec.aps.system.common.tree.ITreeNode;
import com.agiletec.aps.system.common.tree.ITreeNodeManager;
import com.agiletec.aps.system.services.category.Category;
import com.agiletec.apsadmin.system.BaseAction;
import com.agiletec.apsadmin.system.ITreeAction;
import com.agiletec.apsadmin.system.ITreeNodeBaseActionHelper;
import com.agiletec.apsadmin.system.TreeNodeWrapper;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.opensymphony.xwork2.Action;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.entando.entando.aps.system.services.widgettype.IWidgetTypeManager;
import org.entando.entando.aps.system.services.widgettype.WidgetType;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.plugins.jpsolr.aps.system.JpSolrSystemConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FacetNavTreeWidgetActionTest {

    private static final String CATEGORY_1 = "category1";
    private static final String CATEGORY_2 = "category2";
    private static final String WIDGET_TYPE = "widgetType";

    @Mock
    private ITreeNodeManager treeNodeManager;
    @Mock
    private IWidgetTypeManager widgetTypeManager;
    @Mock
    private IContentManager contentManager;
    @Mock
    private ITreeNodeBaseActionHelper treeHelper;

    @InjectMocks
    @Spy
    private FacetNavTreeWidgetAction action;

    @Test
    void shouldValidate() {
        mockCategory(CATEGORY_1);
        mockCreateValuedShowlet();
        Mockito.doNothing().when(action).addFieldError(any(), any());
        Mockito.doReturn("").when(action).getText(anyString(), any(String[].class));
        Mockito.when(contentManager.getSmallContentTypesMap()).thenReturn(Map.of());

        action.setFacetRootNodes(String.join(",", CATEGORY_1, CATEGORY_2));
        action.setWidgetTypeCode(WIDGET_TYPE);
        action.validate();

        Mockito.verify(action).addFieldError(eq(JpSolrSystemConstants.FACET_ROOTS_WIDGET_PARAM_NAME), any());
        Mockito.verify(action).getText("message.facetNavWidget.facets.notValid", new String[]{CATEGORY_2});
    }

    @Test
    void shouldBuildTreeWithMarkerOpen() throws Exception {
        mockCreateValuedShowlet();
        Set<String> targets = Set.of(CATEGORY_1, CATEGORY_2);
        Set<String> nodesToOpen = Set.of(CATEGORY_1);
        Mockito.when(treeHelper.checkTargetNodes(eq(CATEGORY_1), eq(targets), isNull())).thenReturn(nodesToOpen);

        action.setTreeNodeActionMarkerCode(ITreeAction.ACTION_MARKER_OPEN);
        action.setTreeNodesToOpen(targets);
        action.setTargetNode(CATEGORY_1);
        action.setWidgetTypeCode(WIDGET_TYPE);
        String result = action.buildTree();

        Assertions.assertEquals(Action.SUCCESS, result);
        Mockito.verify(action).setTreeNodesToOpen(nodesToOpen);
        Assertions.assertEquals(nodesToOpen, action.getTreeNodesToOpen());
    }

    @Test
    void shouldBuildTreeWithMarkerClose() throws Exception {
        mockCreateValuedShowlet();
        Set<String> targets = Set.of(CATEGORY_1, CATEGORY_2);
        Set<String> nodesToOpen = Set.of();
        Mockito.when(treeHelper.checkTargetNodesOnClosing(eq(CATEGORY_1), eq(targets), isNull()))
                .thenReturn(nodesToOpen);

        action.setTreeNodeActionMarkerCode(ITreeAction.ACTION_MARKER_CLOSE);
        action.setTreeNodesToOpen(targets);
        action.setTargetNode(CATEGORY_1);
        action.setWidgetTypeCode(WIDGET_TYPE);
        String result = action.buildTree();

        Assertions.assertEquals(Action.SUCCESS, result);
        Mockito.verify(action).setTreeNodesToOpen(nodesToOpen);
        Assertions.assertEquals(nodesToOpen, action.getTreeNodesToOpen());
    }

    @Test
    void shouldHandleExceptionInBuildTree() throws Exception {
        mockCreateValuedShowlet();
        Set<String> targets = Set.of(CATEGORY_1, CATEGORY_2);
        Mockito.doThrow(EntException.class).when(treeHelper)
                .checkTargetNodesOnClosing(eq(CATEGORY_1), eq(targets), isNull());

        action.setTreeNodeActionMarkerCode(ITreeAction.ACTION_MARKER_CLOSE);
        action.setTreeNodesToOpen(targets);
        action.setTargetNode(CATEGORY_1);
        action.setWidgetTypeCode(WIDGET_TYPE);
        String result = action.buildTree();

        Assertions.assertEquals(BaseAction.FAILURE, result);
    }

    @Test
    void shouldGetShowableTree() throws Exception {
        Set<String> targets = Set.of(CATEGORY_1, CATEGORY_2);
        ITreeNode root = Mockito.mock(ITreeNode.class);
        TreeNodeWrapper tree = Mockito.mock(TreeNodeWrapper.class);
        Mockito.when(treeHelper.getAllowedTreeRoot(null)).thenReturn(root);
        Mockito.when(treeHelper.getShowableTree(eq(targets), eq(root), isNull())).thenReturn(tree);

        action.setTreeNodesToOpen(targets);
        ITreeNode showableTree = action.getShowableTree();
        Assertions.assertEquals(tree, showableTree);
    }

    private void mockCategory(String code) {
        Category category = new Category();
        category.setCode(code);
        Mockito.when(treeNodeManager.getNode(code)).thenReturn(category);
    }

    private void mockCreateValuedShowlet() {
        WidgetType widgetType = new WidgetType();
        widgetType.setCode(WIDGET_TYPE);
        widgetType.setTypeParameters(List.of());
        Mockito.when(widgetTypeManager.getWidgetType(WIDGET_TYPE)).thenReturn(widgetType);
    }
}
