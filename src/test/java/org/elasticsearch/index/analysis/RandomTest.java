package org.elasticsearch.index.analysis;

import com.google.common.collect.Lists;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;
import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * Created by lonre on 16/6/9.
 */
public class RandomTest extends LuceneTestCase {

    @Test
    public void testLowerCaseFilter() throws IOException {
        Analyzer analyzer = new StandardAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream("fa", "Hello World!");
        LowerCaseFilter lowerCaseFilter = new LowerCaseFilter(tokenStream);
        lowerCaseFilter.reset();
        while (lowerCaseFilter.incrementToken()) {
            Attribute attribute = lowerCaseFilter.getAttribute(CharTermAttribute.class);
            System.out.println(attribute.toString());
        }
    }

    @Test
    public void testPinyinTokenFilter() throws IOException {
        Analyzer analyzer = new StandardAnalyzer();
//        TokenStream tokenStream = analyzer.tokenStream("fa", "刘德华woaibeijing中国快乐");
        TokenStream tokenStream = analyzer.tokenStream("fa", "mingong");
//        PinyinTokenFilter tokenFilter = new PinyinTokenFilter(tokenStream);
        PinyinSegmentationTokenFilter tokenFilter = new PinyinSegmentationTokenFilter(tokenStream);
        tokenFilter.reset();
        while (tokenFilter.incrementToken()) {
            Attribute attribute = tokenFilter.getAttribute(CharTermAttribute.class);
            System.out.println(attribute.toString());
        }
    }

    @Test
    public void testPinyinSegTokenFilterOffset() throws IOException {
        Analyzer analyzer = new PinyinSegAnalyzer();
        String first = getFirstWordOffset(analyzer, "mingong");
        String second = getFirstWordOffset(analyzer, "mingong");
        assertEquals(first, second);
    }

    private String getFirstWordOffset(Analyzer analyzer, String text) throws IOException {
        List<String> attrs = Lists.newArrayList();
        TokenStream tokenStream = analyzer.tokenStream("", text);
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            //获取词元位置属性
            OffsetAttribute offset = tokenStream.addAttribute(OffsetAttribute.class);
            //获取词元文本属性
            CharTermAttribute term = tokenStream.addAttribute(CharTermAttribute.class);
            //获取词元类型属性
            TypeAttribute type = tokenStream.addAttribute(TypeAttribute.class);
            System.out.println(offset.startOffset() + " - " + offset.endOffset() + " : " + term.toString() + " | " + type.type());
            attrs.add(offset.startOffset() + ":" + offset.endOffset());
        }
        tokenStream.end();
        tokenStream.close();
        return attrs.get(0);
    }

    final class PinyinSegAnalyzer extends Analyzer {

        public PinyinSegAnalyzer() {
        }

        @Override
        protected TokenStreamComponents createComponents(final String fieldName) {
            Tokenizer src = new WikipediaTokenizer();
            TokenStream tokenStream = new LowerCaseFilter(src);
            tokenStream = new PinyinSegmentationTokenFilter(tokenStream);
//            TokenStream tokenStream = new PinyinTokenFilter(src);
            return new TokenStreamComponents(src, tokenStream);
        }
    }
}
