package org.elasticsearch.index.similarity;

import org.apache.lucene.search.similarities.Similarity;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;

public class CsSimilarityProvider extends AbstractSimilarityProvider {
    private CsSimilarity similarity;

    @Inject
    public CsSimilarityProvider(@Assisted String name, @Assisted Settings settings) {
        super(name);
        this.similarity = new CsSimilarity();
    }

    @Override
    public Similarity get() {
        return similarity;
    }
}
