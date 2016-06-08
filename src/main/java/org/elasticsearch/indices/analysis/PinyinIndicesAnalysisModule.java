package org.elasticsearch.indices.analysis;

import org.elasticsearch.common.inject.AbstractModule;

public class PinyinIndicesAnalysisModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(PinyinIndicesAnalysis.class).asEagerSingleton();
    }
}
