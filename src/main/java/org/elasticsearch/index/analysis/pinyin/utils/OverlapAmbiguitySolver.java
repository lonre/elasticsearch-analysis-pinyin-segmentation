package org.elasticsearch.index.analysis.pinyin.utils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.elasticsearch.index.analysis.pinyin.entity.TokenEntity;

import java.util.List;
import java.util.Set;

public class OverlapAmbiguitySolver {

    static final ImmutableSet<Character> endChar = ImmutableSet.of(
            'g', 'n', 'r', 'a'
    );

    static final ImmutableSet<Character> startChar = ImmutableSet.of(
            'a', 'e', 'i', 'o', 'u'
    );

    static final ImmutableSet<Character> forbiddenCharOfA = ImmutableSet.of(
            'a', 'e', 'u'
    );

    // if the overlap ambiguity is hit in this map, do not add to the final result
    // always the high-frequency phrase are contained such as "jing an" that always refers to "静安" while "jin gan" has no meaning
    // TODO generate this set by auto-training, such as calculating the appearance
    // TODO or use a bi-gram/tri-gram model to predict the likelihood
    static final ImmutableMap<String, Set<String>> ignorance = ImmutableMap.<String, Set<String>>builder()
            .put("jing", ImmutableSet.of("an"))
            .put("ping", ImmutableSet.of("an"))
            .build();

    // add the overwrite set for example that "huangong" is always split to "huan gong" but rare "huang ong"
    static final ImmutableSet.Builder<String> rareWordFinals =
            ImmutableSet.<String>builder().add("ing", "eng", "ong", "ua", "uan", "uang", "uo", "iu", "ui", "u");
    static final ImmutableMap<Character, ImmutableSet<String>> overwrite = ImmutableMap.<Character, ImmutableSet<String>>builder()
            .put('g', rareWordFinals.build())
            .put('n', rareWordFinals.add("ian", "iang").build())
            .put('e', ImmutableSet.of("r", "n"))
            .put('o', ImmutableSet.of("u"))
            .build();


    public static List<TokenEntity> solve(List<TokenEntity> input) {

        if (input.size() <= 1) {
            return input;
        }
        List<TokenEntity> ambiguities = Lists.newArrayList();
        for (int i = input.size() - 2; i >= 0; i--) {
            // for (int i=0; i<input.size()-1; i++) {
            TokenEntity formerToken = input.get(i);
            // if the pair hits the ignorance set, just ignore the ambiguity
            if (ignorance.containsKey(formerToken.getValue()) &&
                    ignorance.get(formerToken.getValue()).contains(input.get(i + 1).getValue())) {
                continue;
            }
            String former = formerToken.getValue();
            char lastCharOfFormer = former.charAt(former.length() - 1);
            // if former ends with ong then cannot be overwrite since "on" is not legal
            if (former.endsWith("ong") || former.endsWith("nian")) {
                continue;
            }
            // if the overwrite set is hit, overwrite the token value
            if (overwrite.containsKey(lastCharOfFormer)) {
                if (overwrite.get(lastCharOfFormer).contains(input.get(i + 1).getValue())) {
                    formerToken.setValue(former.substring(0, former.length() - 1));
                    TokenEntity latterToken = input.get(i + 1);
                    latterToken.setValue(lastCharOfFormer + latterToken.getValue());
                    continue;
                }
            }

            if (endChar.contains(lastCharOfFormer)) {
                TokenEntity latterToken = input.get(i + 1);
                String latter = latterToken.getValue();
                char firstCharOfLatter = latter.charAt(0);
                if (startChar.contains(firstCharOfLatter)) {
                    if (lastCharOfFormer != 'a' || !forbiddenCharOfA.contains(firstCharOfLatter)) {
                        ambiguities.add(formerToken.duplicate()
                                .setValue(former.substring(0, former.length() - 1))
                                .setEndOffset(formerToken.getEndOffset() - 1));
                        ambiguities.add(latterToken.duplicate()
                                .setValue(former.charAt(former.length() - 1) + latter)
                                .setBeginOffset(latterToken.getBeginOffset() - 1));
                    }
                }
            }
        }
        input.addAll(ambiguities);
        return input;

    }
}
