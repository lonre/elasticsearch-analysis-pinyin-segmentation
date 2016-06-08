package org.elasticsearch.index.analysis;

public class PinyinAnalysisBinderProcessor extends AnalysisModule.AnalysisBinderProcessor {
    @Override
    public void processTokenFilters(TokenFiltersBindings tokenFiltersBindings) {
        tokenFiltersBindings.processTokenFilter("pinyin_segment", PinyinTokenFilterFactory.class);
    }
}
