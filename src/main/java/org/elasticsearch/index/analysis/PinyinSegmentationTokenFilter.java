package org.elasticsearch.index.analysis;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.elasticsearch.index.analysis.pinyin.entity.TokenEntity;
import org.elasticsearch.index.analysis.pinyin.utils.PinyinSegmentation;

import java.io.IOException;
import java.util.List;
import java.util.Queue;

/**
 * This filter split pingyin into to syllable unit eg liudehua -> liu de hua
 */
public class PinyinSegmentationTokenFilter extends TokenFilter {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final Queue<TokenEntity> segmentedTokens = Lists.newLinkedList();
    private TokenEntity formerToken = null;
    private int startOffset;
    private PinyinSegmentation pinyinSegmentation;

    @Override
    public final boolean incrementToken() throws IOException {

        // add tokens that are segmented
        if (segmentedTokens.size() != 0) {
            appendToken(segmentedTokens.poll());
            return true;
        }

        startOffset = this.offsetAtt.endOffset();
        this.formerToken = null;
        // Loop over tokens in the token stream to find the next one
        // that is not empty
        String nextToken = null;
        while (nextToken == null) {

            // Reached the end of the token stream being processed
            if (!this.input.incrementToken()) {
                return false;
            }

            // Get text of the current token and remove any
            // leading/trailing whitespace.
            String currentTokenInStream =
                this.input.getAttribute(CharTermAttribute.class)
                    .toString().trim();

            // Save the token if it is not an empty string
            if (currentTokenInStream.length() > 0) {
                nextToken = currentTokenInStream;
            }
        }
        // put the token without segmentation
        // Save the current token only when it can be segmented
        List<TokenEntity> splitted = pinyinSegmentation.split(nextToken);
        if (splitted.size() > 1) {
            // if pinyin can be splitted
            this.segmentedTokens.addAll(splitted);
            appendToken(this.segmentedTokens.poll());
        } else {
            this.termAtt.setEmpty().append(nextToken);
        }

        return true;
    }

    @VisibleForTesting
    void appendToken(TokenEntity curr) {
        this.termAtt.setEmpty().append(curr.getValue());
        this.offsetAtt.setOffset(
            this.startOffset + curr.getBeginOffset(),
            this.startOffset + curr.getEndOffset());

        if (formerToken != null) {
            if (formerToken.getRelativePosition() == curr.getRelativePosition()) {
                this.posIncrAtt.setPositionIncrement(0);
            } else {
                this.posIncrAtt.setPositionIncrement(curr.getRelativePosition() - formerToken.getRelativePosition());
            }
        } else {
            // when former token is null, keep the position increment from attribute "input"
        }
        this.formerToken = curr;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        this.offsetAtt.setOffset(0, 0);
    }

    public PinyinSegmentationTokenFilter(TokenStream in) {
        super(in);
        this.pinyinSegmentation = new PinyinSegmentation();
    }

}
