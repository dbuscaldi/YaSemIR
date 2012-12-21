package fr.lipn.yasemir.weighting.ckpd;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.search.IndexSearcher;

public class TermFactory {
	public static IndexSearcher searcher; //we use this to calculate term frequencies
	public static Analyzer analyzer;
	
	public static void init(IndexSearcher s, Analyzer a) {
		searcher=s;
		analyzer=a;
	}
	
	public static Vector<NGramTerm> makeTermSequence(String text){
		List<String> result = new ArrayList<String>();
        TokenStream stream  = analyzer.tokenStream("text", new StringReader(text));

        try {
            while(stream.incrementToken()) {
                result.add(stream.getAttribute(TermAttribute.class).term());
                //System.err.println(stream.getAttribute(TermAttribute.class).term());
            }
        }
        catch(IOException e) {
            // not thrown b/c we're using a string reader...
        }
        
        Vector<NGramTerm> ngtv = new Vector<NGramTerm>();
        for(String s: result){
        	NGramTerm t = new NGramTerm(s);
        	ngtv.add(t);
        }
		return ngtv;
	}
}
