package crawler;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.util.CharArraySet;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import java.io.StringReader;
import java.io.IOException;
/**
 * Created by jifeiqian on 2/15/17.
 */
public class Utility {
    private static final Version LUCENE_VERSION = Version.LUCENE_40;
    private static String stopWords = "a, able, about, across, after, all, almost, also, am, among, an, and";
    private static CharArraySet getStopWords(String stopwords) {
        List<String> stopwordsList = new ArrayList<String>();
        for (String stop : stopwords.split(",")) {
            stopwordsList.add(stop.trim());
        }
        return new CharArraySet(LUCENE_VERSION, stopwordsList, true);
    }

    public static String strJoin(List<String> aArr, String sSep) {
        StringBuilder sbStr = new StringBuilder();
        for (int i = 0; i < aArr.size(); i++) {
            if (i > 0) {
                sbStr.append(sSep);
            }
            sbStr.append(aArr.get(i));
        }
        return sbStr.toString();
    }

    //remove stop word, tokenize, stem
    public static List<String> cleanedTokenize(String input) {
        List<String> tokens = new ArrayList<String>();
        StringReader reader = new StringReader(input.toLowerCase());
        StandardTokenizer tokenizer = new StandardTokenizer(LUCENE_VERSION, reader);
        TokenStream tokenStream = new StandardFilter(LUCENE_VERSION, tokenizer);
        tokenStream = new StopFilter(LUCENE_VERSION, tokenStream, getStopWords(stopWords));
        tokenStream = new KStemFilter(tokenStream);

        try {
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                tokens.add(tokenStream.getAttribute(CharTermAttribute.class).toString());
            }
            tokenStream.end();
            tokenStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tokens;
    }

    public static List<String> getNgram(String query) {
        List<String> grams = new ArrayList<>();
        List<String> tokens = cleanedTokenize(query);
        for (int i = 2; i <= tokens.size() - 1; i++) {
            for (int j = 0; j + i <= tokens.size(); j++) {
                StringBuilder sb = new StringBuilder();
                for (int k = j; k < j + i; k++) {
                    if (k > j) sb.append(" ");
                    sb.append(tokens.get(k));
                }
                grams.add(sb.toString());
            }
        }
        return grams;
    }
}
