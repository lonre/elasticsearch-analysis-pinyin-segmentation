package org.elasticsearch.index.analysis;/** * Licensed to the Apache Software Foundation (ASF) under one or more * contributor license agreements.  See the NOTICE file distributed with * this work for additional information regarding copyright ownership. * The ASF licenses this file to You under the Apache License, Version 2.0 * (the "License"); you may not use this file except in compliance with * the License.  You may obtain a copy of the License at * *     http://www.apache.org/licenses/LICENSE-2.0 * * Unless required by applicable law or agreed to in writing, software * distributed under the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * See the License for the specific language governing permissions and * limitations under the License. */import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;import org.apache.lucene.analysis.TokenFilter;import org.apache.lucene.analysis.TokenStream;import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;import org.elasticsearch.index.analysis.pinyin.utils.FMMSegmentation;import java.io.IOException;import java.util.LinkedList;import java.util.List;import java.util.Queue;/** */public class PinyinTokenFilter extends TokenFilter {    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);    private HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();    private final Queue<String> segmentedTokens = new LinkedList<String>();    @Override    public final boolean incrementToken() throws IOException {        // add tokens that are segmented        if (segmentedTokens.size() != 0) {            String token = segmentedTokens.poll();            this.termAtt.setEmpty().append(token);            return true;        }        // Loop over tokens in the token stream to find the next one        // that is not empty        String nextToken = null;        while (nextToken == null) {            // Reached the end of the token stream being processed            if ( ! this.input.incrementToken()) {                return false;            }            // Get text of the current token and remove any            // leading/trailing whitespace.            String currentTokenInStream =                    this.input.getAttribute(CharTermAttribute.class)                            .toString().trim();            // Save the token if it is not an empty string            if (currentTokenInStream.length() > 0) {                nextToken = currentTokenInStream;            }        }        this.termAtt.setEmpty().append(nextToken);        // Save the current token only when it is splitted        List<String> splitted = FMMSegmentation.splitSpell(nextToken);        if (splitted.size() > 1) {            this.segmentedTokens.addAll(FMMSegmentation.splitSpell(nextToken));        }        return true;    }    public PinyinTokenFilter(TokenStream in) {        super(in);        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);        format.setVCharType(HanyuPinyinVCharType.WITH_V);    }    @Override    public final void end() throws IOException {        // set final offset      super.end();    }    @Override    public void reset() throws IOException {        super.reset();    }}