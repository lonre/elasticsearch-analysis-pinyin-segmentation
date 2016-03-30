package org.elasticsearch.index.analysis;/** * Licensed to the Apache Software Foundation (ASF) under one or more * contributor license agreements.  See the NOTICE file distributed with * this work for additional information regarding copyright ownership. * The ASF licenses this file to You under the Apache License, Version 2.0 * (the "License"); you may not use this file except in compliance with * the License.  You may obtain a copy of the License at * *     http://www.apache.org/licenses/LICENSE-2.0 * * Unless required by applicable law or agreed to in writing, software * distributed under the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * See the License for the specific language governing permissions and * limitations under the License. */import org.apache.lucene.analysis.TokenFilter;import org.apache.lucene.analysis.TokenStream;import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;import org.elasticsearch.index.analysis.pinyin.utils.PinyinSegmentation;import java.io.IOException;import java.util.LinkedList;import java.util.List;import java.util.Queue;/** */public class PinyinSegmentationTokenFilter extends TokenFilter {    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);    private final Queue<String> segmentedTokens = new LinkedList<String>();    @Override    public final boolean incrementToken() throws IOException {        // add tokens that are segmented        if (segmentedTokens.size() != 0) {            this.termAtt.setEmpty().append(segmentedTokens.poll());            return true;        }        // Loop over tokens in the token stream to find the next one        // that is not empty        String nextToken = null;        while (nextToken == null) {            // Reached the end of the token stream being processed            if ( ! this.input.incrementToken()) {                return false;            }            // Get text of the current token and remove any            // leading/trailing whitespace.            String currentTokenInStream =                    this.input.getAttribute(CharTermAttribute.class)                            .toString().trim();            // Save the token if it is not an empty string            if (currentTokenInStream.length() > 0) {                nextToken = currentTokenInStream;            }        }        // put the token without segmentation        this.termAtt.setEmpty();        // Save the current token only when it can be segmented        List<String> splitted = PinyinSegmentation.split(nextToken);        if (splitted.size() > 1) {            this.segmentedTokens.addAll(splitted);            this.termAtt.append(this.segmentedTokens.poll());        } else {            this.termAtt.append(nextToken);        }        return true;    }    public PinyinSegmentationTokenFilter(TokenStream in) {        super(in);    }}