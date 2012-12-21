package fr.lipn.yasemir.weighting.ckpd;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class TestNGrams {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		String index = "indexOHSUMed_sem";
		IndexReader reader = IndexReader.open(FSDirectory.open(new File(index)));
		IndexSearcher searcher = new IndexSearcher(reader);
		Analyzer analyzer;
		analyzer =  new EnglishAnalyzer(Version.LUCENE_31);
		
		String test = "The quick brown fox jumps over the lazy dog.";
		
		TermFactory.init(searcher, analyzer);
		Vector<NGramTerm> tv = TermFactory.makeTermSequence(test);
		
		HashSet<NGram> ngset =NGramFactory.getNGramSet(tv);
		for(NGram ng : ngset) {
			System.out.println(ng.repr());
			for(NGramTerm t : ng.getSequence()){
				System.out.println(t.text+" -> "+t.weight);
			}
		}

	}

}
