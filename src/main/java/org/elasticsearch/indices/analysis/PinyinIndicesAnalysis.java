package org.elasticsearch.indices.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.analysis.PinyinSegmentationTokenFilter;
import org.elasticsearch.index.analysis.PinyinTokenFilter;
import org.elasticsearch.index.analysis.PreBuiltTokenFilterFactoryFactory;
import org.elasticsearch.index.analysis.TokenFilterFactory;

public class PinyinIndicesAnalysis extends AbstractComponent {
    @Inject
    public PinyinIndicesAnalysis(Settings settings, IndicesAnalysisService indicesAnalysisService) {
        super(settings);

        indicesAnalysisService.tokenFilterFactories().put("pinyin_segment", new PreBuiltTokenFilterFactoryFactory(new TokenFilterFactory() {
            @Override
            public String name() {
                return "pinyin_segment";
            }

            @Override
            public TokenStream create(TokenStream tokenStream) {
                return new PinyinSegmentationTokenFilter(new PinyinTokenFilter(tokenStream));
            }
        }));
    }
}
