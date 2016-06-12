package org.elasticsearch.index.analysis;

import com.google.common.collect.Sets;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * This class transfer Chinese Character into pinyin
 */
public class PinyinTokenFilter extends TokenFilter {

    private static final ESLogger logger = Loggers.getLogger(PinyinTokenFilter.class);

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();

    private Queue<String> pinyinOfOneWord = new LinkedList<String>();
    private boolean initialLetterMode = false;
    private StringBuffer initialLetters;

    @Override
    public final boolean incrementToken() throws IOException {

        // if the pinyin for the former word is still in the queue,
        // then poll the pinyin and set to the term attribute
        if (pinyinOfOneWord.size() > 0) {
            this.termAtt.setEmpty().append(pinyinOfOneWord.poll());
            posIncrAtt.setPositionIncrement(0);
            return true;
        }

        // Loop over tokens in the token stream to find the next one
        // that is not empty
        String nextToken = null;
        while (nextToken == null) {

            // Reached the end of the token stream being processed
            if (!this.input.incrementToken()) {
                // if no token from the former filter/tokenizer
                // then check the initialLetter
                if (initialLetterMode && initialLetters != null) {
                    this.termAtt.setEmpty().append(initialLetters);
                    // set increment to 0 to be compatible with the phrase_prefix_match
                    // since the generated prefix is regarded as the last term
                    // so 刘德华 => liu de hua ldh, while 刘德 => liu de ld
                    // and hua and ldh, de and ld are treated as in the same position
                    posIncrAtt.setPositionIncrement(0);
                    initialLetters = null;
                    return true;
                } else {
                    return false;
                }
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

        // Now nextToken is a meaningful string
        String token = nextToken;
        this.termAtt.setEmpty();
        initialLetters = initialLetters == null ? new StringBuffer() : initialLetters;
        // Assume token is either a Chinese word such as 刘德华
        // or a sequence of English letter and number such as qemu3d
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c < 128) {
                // if c is an english letter or a number, just append to termAtt
                // in this case, append all c to one termAtt
                if (('a' <= c && c <= 'z') || ('0' <= c && c <= '9')) {
                    this.termAtt.append(c);
                } else if (('A' <= c && c <= 'Z')) {
                    this.termAtt.append(Character.toLowerCase(c));
                }
            } else {
                String[] pinyinList = null;
                try {
                    pinyinList = PinyinHelper.toHanyuPinyinStringArray(c, format);
                } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
                    logger.error("Error when trying to parse {}, e = {}", c, badHanyuPinyinOutputFormatCombination);
                }
                if (pinyinList != null && pinyinList.length > 0) {
                    initialLetters.append(pinyinList[0].charAt(0));
                    if (pinyinList.length == 1) {
                        // 不是多音字
                        pinyinOfOneWord.add(pinyinList[0]);
                    } else {
                        // use hashset to remove duplicate
                        // 因为多音字可能只是声调不同
                        pinyinOfOneWord.addAll(Sets.newHashSet(pinyinList));
                    }
                }
                // Anyway put the Chinese to the term attr
                this.termAtt.append(c);
            }
        }
        return true;
    }

    public PinyinTokenFilter(TokenStream in) {
        super(in);
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
    }


}
