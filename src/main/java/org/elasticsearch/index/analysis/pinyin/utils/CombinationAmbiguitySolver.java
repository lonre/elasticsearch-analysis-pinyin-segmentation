package org.elasticsearch.index.analysis.pinyin.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.elasticsearch.index.analysis.pinyin.entity.TokenEntity;

import java.util.List;

public class CombinationAmbiguitySolver {

    static ImmutableMap<String, List<String>> combinations = ImmutableMap.<String, List<String>>builder()
            .put("xian", ImmutableList.of("xi", "an"))
            //.put("lian", ImmutableList.of("li", "an"))  // lian is more likely than li an
            .put("tian", ImmutableList.of("ti", "an"))
            //.put("yuan", ImmutableList.of("yu", "an"))  // yuan is more likely than yu an
            .put("niao", ImmutableList.of("ni", "ao"))
            .put("jiao", ImmutableList.of("ji", "ao"))
            .put("dian", ImmutableList.of("di", "an"))
            // TODO more combination ambiguity to be added
            .build();

    public static List<TokenEntity> solve(List<TokenEntity> input) {
        List<TokenEntity> additionalTokens = Lists.newArrayList();
        for (TokenEntity s : input) {
            List<String> subTokens;
            if ((subTokens = combinations.get(s.getValue())) != null) {
                for (String sub : subTokens) {
                    additionalTokens.add(s.duplicate().setValue(sub));
                    // TODO change the offset and position
                }
            }
        }
        input.addAll(additionalTokens);
        return input;
    }
}
