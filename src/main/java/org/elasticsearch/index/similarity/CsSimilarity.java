package org.elasticsearch.index.similarity;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsSimilarity extends Similarity {
    private static final ESLogger logger = Loggers.getLogger(CsSimilarity.class);

    @Override
    public long computeNorm(FieldInvertState state) {
        return (long) (state.getLength() - state.getNumOverlap());
    }

    @Override
    public SimWeight computeWeight(CollectionStatistics collectionStats, TermStatistics... termStats) {
        return new PositionStats(collectionStats.field(), termStats);
    }

    @Override
    public SimScorer simScorer(SimWeight weight, LeafReaderContext context) throws IOException {
        PositionStats positionStats = (PositionStats) weight;
        return new PositionSimScorer(positionStats, context);
    }

    private final class PositionSimScorer extends SimScorer {
        private final PositionStats stats;
        private final NumericDocValues norms;
        private final LeafReaderContext context;
        private final List<Explanation> explanations = new ArrayList<>();

        PositionSimScorer(PositionStats stats, LeafReaderContext context) throws IOException {
            this.stats = stats;
            this.norms = context.reader().getNormValues(stats.field);
            this.context = context;
        }

        @Override
        public float score(int doc, float freq) {
            float totalScore = 0.0f;
            int i = 0;
            while (i < stats.termStats.length) {
                totalScore += scoreTerm(doc, stats.termStats[i].term());
                i++;
            }
            return totalScore + scoreNorm(doc);
        }

        private float scoreTerm(int doc, BytesRef term) {
            float halfScorePosition = 5.0f; // position where score should decrease by 50%
            int termPosition = position(doc, term);
            float normScore = scoreNorm(doc);
            float termScore = stats.boost * halfScorePosition / (halfScorePosition + termPosition) * normScore;

            String func = stats.boost + "*" + halfScorePosition + "/(" + halfScorePosition + "+" + termPosition + ")*" + normScore;
            explanations.add(Explanation.match(termScore, "scoreTerm(boost=" + stats.boost + ", pos=" + termPosition + ", func=" + func + ")"));

            return termScore;
        }

        private float scoreNorm(int doc) {
            long normValue = norms.get(doc);
            if (this.stats.termStats.length >= normValue) {  // 完全匹配
                return 1F;
            }
            return 0.5F;
        }

        private int position(int doc, BytesRef term) {
            int maxPosition = 20;
            try {
                Terms terms = context.reader().getTermVector(doc, stats.field);
                TermsEnum termsEnum = terms.iterator();
                if (!termsEnum.seekExact(term)) {
                    logger.warn("seekExact failed for term: " + term.utf8ToString());
                    return maxPosition;
                }
                PostingsEnum dpEnum = termsEnum.postings(null, PostingsEnum.POSITIONS);
                dpEnum.nextDoc();
                return dpEnum.nextPosition();
            } catch (Exception ex) {
                logger.error("exception occured while getting postion", ex);
                return maxPosition;
            }
        }

        @Override
        public float computeSlopFactor(int distance) {
            return 1.0F / (distance + 1);
        }

        @Override
        public float computePayloadFactor(int doc, int start, int end, BytesRef payload) {
            return 1.0F;
        }

        @Override
        public Explanation explain(int doc, Explanation freq) {
            return Explanation.match(
                score(doc, freq.getValue()),
                "cs score(doc=" + doc + ", freq=" + freq.getValue() + "), sum of:",
                explanations
            );
        }
    }

    private static class PositionStats extends SimWeight {
        private final String field;
        private final TermStatistics[] termStats;
        private float boost;

        public PositionStats(String field, TermStatistics... termStats) {
            this.field = field;
            this.termStats = termStats;
            normalize(1F, 1F);
        }


        @Override
        public float getValueForNormalization() {
            return this.boost * this.boost;
        }

        @Override
        public void normalize(float queryNorm, float boost) {
            this.boost = boost;
        }
    }
}
