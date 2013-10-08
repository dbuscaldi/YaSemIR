package fr.lipn.yasemir.test;

import java.io.File;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import fr.lipn.yasemir.configuration.Yasemir;

public class TestTermIndex {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Yasemir.init("cuisine/cuisine.xml");
		
		try {
				IndexReader reader = IndexReader.open(FSDirectory.open(new File(Yasemir.TERM_DIR)));
				
				for(int i=0; i< reader.maxDoc(); i++) {
					Document d = reader.document(i);
					System.err.println("Concept: "+d.get("id"));
					System.err.println("Labels: "+d.get("labels"));
									}
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

}
