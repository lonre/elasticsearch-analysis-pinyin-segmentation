package org.elasticsearch.plugin.analysis.pinyin;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.index.analysis.PinyinAnalysisBinderProcessor;
import org.elasticsearch.index.similarity.CsSimilarityProvider;
import org.elasticsearch.index.similarity.SimilarityModule;
import org.elasticsearch.indices.analysis.PinyinIndicesAnalysisModule;
import org.elasticsearch.plugins.Plugin;

import java.util.Collection;
import java.util.Collections;

/**
 * The Pinyin Analysis plugin integrates Pinyin4j(http://pinyin4j.sourceforge.net/) module into elasticsearch.
 */
public class AnalysisPinyinSegmentationPlugin extends Plugin {

    @Override
    public String name() {
        return "analysis-pinyin-segmentation";
    }

    @Override
    public String description() {
        return "Chinese to Pinyin convert support";
    }


    @Override
    public Collection<Module> nodeModules() {
        return Collections.<Module>singletonList(new PinyinIndicesAnalysisModule());
    }

    public void onModule(AnalysisModule module) {
        module.addProcessor(new PinyinAnalysisBinderProcessor());
    }

    public void onModule(SimilarityModule module) {
        module.addSimilarity("CsSimilarity", CsSimilarityProvider.class);
    }
}
