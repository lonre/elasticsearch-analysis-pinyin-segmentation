package org.elasticsearch.index.similarity;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class CsSimilarityT {
    private Directory directory;

    @Before
    public void setUp() throws Exception {
//        Path indexPath = Files.createTempDirectory("lucene-test");
//        System.out.println("creating index director: " + indexPath);
//        Directory directory = FSDirectory.open(indexPath);
        this.directory = new RAMDirectory();
        IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer());
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter writer = new IndexWriter(directory, iwc);
        writer.commit();
        writer.close();
    }

    @After
    public void tearDown() throws Exception {
        directory.close();
        if (directory instanceof FSDirectory) {
            IOUtils.rm(((FSDirectory) directory).getDirectory());
        }
    }

    @Test
    public void testExplainScore() throws Exception {
        Analyzer analyzer = new StandardAnalyzer();
        String[] values = new String[]{"朝阳公园", "朝阳医院", "北京朝阳医院", "朝阳", "阳光朝露"};
        indexDocs(analyzer, values);

        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(directory));

        searcher.setSimilarity(new CsSimilarity());
        QueryParser parser = new QueryParser("name", analyzer);
        Query query = parser.parse("梅西百");
        TopDocs results = searcher.search(query, 10);
        for (ScoreDoc d : results.scoreDocs) {
            Explanation explanation = searcher.explain(query, d.doc);
            Document document = searcher.doc(d.doc);
            System.out.println(document.get("name"));
            System.out.println(explanation);
            System.out.println("+++++++++++++++++\n");
        }
    }

    @Test
    public void testPharseNormQuery() throws Exception {
        Analyzer analyzer = new StandardAnalyzer();
        String[] values = new String[]{"梅西百货", "梅西百货华盛顿店", "梅西百货迈阿密南滩店"};
        indexDocs(analyzer, values);

        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(directory));

        searcher.setSimilarity(new CsSimilarity());

        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        builder.add(new Term("name", "梅"));
        builder.add(new Term("name", "西"));
        builder.add(new Term("name", "百"));
        builder.add(new Term("name", "货"));
        builder.build();
        Query query = builder.build();
        TopDocs results = searcher.search(query, 10);
        for (ScoreDoc d : results.scoreDocs) {
            Explanation explanation = searcher.explain(query, d.doc);
            Document document = searcher.doc(d.doc);
            System.out.println(document.get("name"));
            System.out.println(explanation);
            System.out.println("+++++++++++++++++\n");
        }
    }

    @Test
    public void testPharseQuery() throws Exception {
        Analyzer analyzer = new StandardAnalyzer();
        String[] values = new String[]{"朝阳公园", "阳光朝露"};
        indexDocs(analyzer, values);

        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(directory));

        searcher.setSimilarity(new CsSimilarity());

        PhraseQuery.Builder builder = new PhraseQuery.Builder();
//        builder.setSlop(1);
        builder.add(new Term("name", "朝"));
        builder.add(new Term("name", "阳"));
        builder.build();
        Query query = builder.build();
        TopDocs results = searcher.search(query, 10);
        for (ScoreDoc d : results.scoreDocs) {
            Explanation explanation = searcher.explain(query, d.doc);
            Document document = searcher.doc(d.doc);
            System.out.println(document.get("name"));
            System.out.println(explanation);
            System.out.println("+++++++++++++++++\n");
        }
    }

    @Test
    public void testCsSimilarity() throws Exception {
        Analyzer analyzer = new CaPinyinAnalyzer();
        String[] values = new String[]{"xxl xinxilan xin xi lan", "xl xila xi la", "xby xibanya xi ban ya"};
        indexDocs(analyzer, values);

        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(directory));

        searcher.setSimilarity(new CsSimilarity());
        QueryParser parser = new QueryParser("name", analyzer);
        Query query = parser.parse("l");
        TopDocs results = searcher.search(query, 10);

        assertEquals(2, results.totalHits);
        assertEquals(values[0], searcher.doc(results.scoreDocs[1].doc).get("name"));
        assertEquals(values[1], searcher.doc(results.scoreDocs[0].doc).get("name"));
    }

    private void indexDocs(Analyzer analyzer, String[] values) throws IOException {
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setSimilarity(new CsSimilarity());
        iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        IndexWriter writer = new IndexWriter(directory, iwc);

        FieldType textIndexedType = new FieldType();
        textIndexedType.setStored(true);
        textIndexedType.setTokenized(true);
        textIndexedType.setStoreTermVectors(true);
        textIndexedType.setStoreTermVectorOffsets(true);
        textIndexedType.setStoreTermVectorPayloads(true);
        textIndexedType.setStoreTermVectorPositions(true);
        textIndexedType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);

        for (String name : values) {
            Document doc = new Document();
            doc.add(new Field("name", name, textIndexedType));
            writer.addDocument(doc);
        }

        writer.close();
    }

    private class CaPinyinAnalyzer extends Analyzer {

        @Override
        protected TokenStreamComponents createComponents(String fieldName) {
            Tokenizer src = new WhitespaceTokenizer();
            TokenStream tokenStream = new LowerCaseFilter(src);
            tokenStream = new EdgeNGramTokenFilter(tokenStream, 1, 50);
            return new TokenStreamComponents(src, tokenStream);
        }
    }
}
