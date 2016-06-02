package org.elasticsearch.index.analysis.pinyin.entity;

public interface TreeNode {

    // add the string[index] to child
    public boolean addChild(char[] string, int index);

    /**
     * find the endIndex that string[index], string[index+1], ... , string[endIndex-1]
     * composes a string that can be found from the children and
     * string[index], string[index+1], ... , string[endIndex] cannot be found
     * @param string string
     * @param index index
     * @return endIndex
     */
    public int findEndIndexInChildren(char[] string, int index);

    public boolean containsStringInChildren(char[] string, int index);

    public void addNumberNode();
}
