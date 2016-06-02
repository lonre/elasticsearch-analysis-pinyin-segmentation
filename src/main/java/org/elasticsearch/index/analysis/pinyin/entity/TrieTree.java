package org.elasticsearch.index.analysis.pinyin.entity;


/**
 * The root node of the tree that contains no value
 */
public class TrieTree implements TreeNode{

    private TreeNode treeNode;

    public TrieTree (boolean allowPrefixCut) {
        if (allowPrefixCut) treeNode = new TreeNodeWithoutEndpoint();
        else treeNode = new TreeNodeWithEndpoint();
    }

    public boolean add(char[] string) {
        if (string == null) return false;

        return treeNode.addChild(string, 0);
    }

    public boolean contains(char[] string) {
        if (string == null || string.length == 0) return false;
        return treeNode.containsStringInChildren(string, 0);
    }


    @Override
    public boolean addChild(char[] string, int index) {
        return treeNode.addChild(string, index);
    }

    @Override
    public int findEndIndexInChildren(char[] string, int index) {
        return treeNode.findEndIndexInChildren(string, index);
    }

    @Override
    public boolean containsStringInChildren(char[] string, int index) {
        return treeNode.containsStringInChildren(string, index);
    }

    @Override
    public void addNumberNode() {
        treeNode.addNumberNode();
    }
}


