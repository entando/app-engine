package org.entando.entando.aps.system.services;

import com.agiletec.aps.system.common.tree.ITreeNode;
import lombok.extern.slf4j.Slf4j;

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
            for (String childNodeCode : parentNode.getChildrenCodes()) {
                T childNode = this.getTreeNode(childNodeCode);

                if (this.isNodeAllowed(childNode)) {
                    nodes.add(childNode);
                    log.debug("added child:'{}' for parentNode:'{}' absolutePosition:'{}'",
                            childNodeCode, parentNode.getCode(), childNode.getPosition());
                }
                buildNodesList(childNode, nodes, false);
            }
        }
        return nodes;
    }

    public abstract T getTreeNode(String nodeCode);

    protected abstract boolean isNodeAllowed(T node);
}
