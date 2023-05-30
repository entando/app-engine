package org.entando.entando.aps.system.services;

import com.agiletec.aps.system.common.tree.ITreeNode;
import com.agiletec.aps.system.common.tree.TreeNode;
import lombok.extern.slf4j.Slf4j;
import org.entando.entando.ent.util.EntLogging;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class TreeNodeHelper<T extends ITreeNode> {

    public List<T> getNodes(String parentNodeCode) {
        T parentNode = this.getTreeNode(parentNodeCode);
        List<T> nodes = buildNodesList(parentNode, new ArrayList<>(), true);
        // update parent code to match requested parent code
        nodes.forEach(p -> p.setParentCode(parentNodeCode));
        return nodes;
    }

    private List<T> buildNodesList(T parentNode, List<T> nodes, boolean root) {
        log.debug("build node list for parentNode:'{}'", parentNode.getCode());
        if (root || !this.isNodeAllowed(parentNode)) {
            int absolutePosition = 0;
            for (String childNodeCode : parentNode.getChildrenCodes()) {
                absolutePosition++;
                T childNode = this.getTreeNode(childNodeCode);
                log.debug("iteration build child:'{}' for parentNode:'{}' position:'{}' absolutePosition:'{}'",
                        childNodeCode, parentNode.getCode(), childNode.getPosition(), absolutePosition);

                if (this.isNodeAllowed(childNode)) {
                    childNode.setAbsolutePosition(absolutePosition);
                    nodes.add(childNode);
                }
                buildNodesList(childNode, nodes, false);
            }
        }
        return nodes;
    }

    public abstract T getTreeNode(String nodeCode);

    protected abstract boolean isNodeAllowed(T node);
}
