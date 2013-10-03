package fr.lipn.yasemir.weighting.ckpd;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.IndexReader;
public class TermFactory {
	public static IndexReader reader; //we use this to calculate term frequencies
	public static Analyzer analyzer;
	
	public static void init(IndexReader s, Analyzer a) {
		reader=s;
		analyzer=a;
	}
	
	public static Vector<NGramTerm> makeTermSequence(String text){
		List<String> result = new ArrayList<String>();
		try {
			TokenStream stream  = analyzer.tokenStream("text", new StringReader(text));

        
            while(stream.incrementToken()) {
                //result.add(stream.getAttribute(TermAttribute.class).term()); //old way
                result.add(stream.getAttribute(CharTermAttribute.class).toString());
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
