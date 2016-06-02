package org.elasticsearch.index.analysis.pinyin.entity;

import org.elasticsearch.common.collect.Maps;

import java.util.Map;

public class TreeNodeWithEndpoint implements TreeNode {
    // key is the value
    Map<Character, TreeNodeWithEndpoint> children;

    char value;
    boolean endpoint;

    // add the string[index] to child
    @Override
    public boolean addChild(char[] string, int index) {
        if (index >= string.length) {
            endpoint = true;
            return false;
        }

        if (children == null) {
            children = Maps.newHashMap();
        }
        char c = string[index];
        if (children.containsKey(c)) {
            return children.get(c).addChild(string, index+1);
        } else {
            TreeNodeWithEndpoint child = new TreeNodeWithEndpoint();
            child.value = c;
            children.put(c, child);
            return child.addChild(string, index+1);
        }
    }

    /**
     * find the endIndex that string[index], string[index+1], ... , string[endIndex-1]
     * composes a string that can be found from the children and
     * string[index], string[index+1], ... , string[endIndex] cannot be found
     * @param string string
     * @param index index
     * @return endIndex
     */
    @Override
    public int findEndIndexInChildren(char[] string, int index) {
        if (index >= string.length) return index;

        if (!containsChild(string[index])) return index;

        int endIndex = children.get(string[index]).findEndIndexInChildren(string, index + 1);
        if (endIndex == index + 1) {
            if (children.get(string[index]).endpoint) return endIndex;
            else return index;
        } else {
            return endIndex;
        }

    }

    @Override
    public boolean containsStringInChildren(char[] string, int index) {
        return string.length == findEndIndexInChildren(string, index);

    }

    /**
     * add a tree node whose children are all itself with the key be number from 0 to 9, inclusive
     * in order to present a number (may be with infinite length)
     */
    @Override
    public void addNumberNode() {
        TreeNodeWithEndpoint numberNode = new TreeNodeWithEndpoint();
        numberNode.children = Maps.newHashMap();
        numberNode.endpoint = true;
        for (char i = '0'; i <= '9'; i++) {
            numberNode.children.put(i, numberNode);
            children.put(i, numberNode);
        }
    }

    private boolean containsChild(char c) {
        return children != null && children.containsKey(c);
    }

    @Override
    public String toString() {
        return org.elasticsearch.common.base.MoreObjects.toStringHelper(this)
                .add("children", children)
                .add("value", value)
                .add("endpoint", endpoint)
                .toString();
    }
}
