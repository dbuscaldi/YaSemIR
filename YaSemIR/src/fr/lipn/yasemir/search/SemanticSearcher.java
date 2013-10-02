package fr.lipn.yasemir.search;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ca.CatalanAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.pt.PortugueseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.util.Version;

import fr.lipn.yasemir.configuration.Yasemir;
import fr.lipn.yasemir.ontology.annotation.Annotation;
import fr.lipn.yasemir.weighting.ckpd.NGramTerm;
import fr.lipn.yasemir.weighting.ckpd.TermFactory;

public class SemanticSearcher {
	Analyzer analyzer;
	IndexReader reader;
	IndexSearcher searcher;
	QueryParser parser;
	String basefield = "text";
	
	public static int MAX_HITS=1000;
	
	public SemanticSearcher(String lang, IndexReader reader) {
		if(lang.equals("fr")) analyzer = new FrenchAnalyzer(Version.LUCENE_44);
	    else if(lang.equals("it")) analyzer = new ItalianAnalyzer(Version.LUCENE_44);
	    else if(lang.equals("es")) analyzer = new SpanishAnalyzer(Version.LUCENE_44);
	    else if(lang.equals("de")) analyzer = new GermanAnalyzer(Version.LUCENE_44);
	    else if(lang.equals("pt")) analyzer = new PortugueseAnalyzer(Version.LUCENE_44);
	    else if(lang.equals("ca")) analyzer = new CatalanAnalyzer(Version.LUCENE_44);
	    else if(lang.equals("nl")) analyzer = new DutchAnalyzer(Version.LUCENE_44);
	    else analyzer = new EnglishAnalyzer(Version.LUCENE_44);
		
		this.searcher = new IndexSearcher(reader);
	    if(Yasemir.SCORE.equals("BM25")) searcher.setSimilarity(new BM25Similarity());
	    
		parser = new QueryParser(Version.LUCENE_44, basefield, analyzer);
	}
	
	public Vector<RankedDocument> search(String line) throws ParseException, IOException {
		Vector<RankedDocument> ret = new Vector<RankedDocument>();
		
		Vector<NGramTerm> queryNGT = null;
		if(Yasemir.CKPD_ENABLED) queryNGT = TermFactory.makeTermSequence(line);
			
		if(Yasemir.MODE==Yasemir.CLASSIC){
	    	  Query query = parser.parse(line);
	    	  
	    	  if(Yasemir.DEBUG) System.err.println("[YaSemIr - CLASSIC] Searching for: " + query.toString(basefield));
	            
		      searcher.search(query, null, MAX_HITS);
		      
		      TopDocs results = searcher.search(query, MAX_HITS);
			  ScoreDoc[] hits = results.scoreDocs;
			    
			  int numTotalHits = results.totalHits;
			  if(Yasemir.DEBUG) System.err.println("[YaSemIr] "+numTotalHits + " total matching documents");
			  if(numTotalHits > 0) {
		    	for (int i = 0; i < Math.min(numTotalHits, MAX_HITS); i++) {
			        Document doc = searcher.doc(hits[i].doc);
			        String id = doc.get("id");
			        String textAbst=doc.get("text");
			        float docWeight = hits[i].score;

			        RankedDocument srDoc = new RankedDocument(id, textAbst, null);
			        srDoc.setWeight(docWeight);
			        
			        if(Yasemir.CKPD_ENABLED) srDoc.setCKPDWeight(queryNGT);
			        
			        ret.add(srDoc);
		    	}
		     }
	      } else {
	          HashMap<String, Vector<Annotation>> queryAnnotation = null;
	          if(Yasemir.DEBUG)  System.err.println("[YaSemIr] Annotating: " + line);
		      
	          queryAnnotation=Yasemir.annotator.annotate(line);
		      
		      if(Yasemir.DEBUG) {
		    	  System.err.println("[YaSemIr] Annotations:");
		      
			      for(String oid : queryAnnotation.keySet()) {
			    	  Vector<Annotation> ann = queryAnnotation.get(oid);
			    	  for(Annotation a : ann){
				    	  System.err.println(a.getOWLClass().getIRI());
				      }
			      }
			      System.err.println("---------------------------");
		      }
	     	  
		      if(Yasemir.MODE==Yasemir.SEMANTIC || Yasemir.MODE==Yasemir.HYBRID){
		    	  String debugStr;
		    	  if(Yasemir.MODE==Yasemir.SEMANTIC) debugStr="SEMANTIC";
		    	  else debugStr="HYBRID";
		    	  
	    		  StringBuffer extQueryText = new StringBuffer();
		    	  for(String oid : queryAnnotation.keySet()) {
			    	  Vector<Annotation> ann = queryAnnotation.get(oid);
			    	  for(Annotation a : ann){
			    		  extQueryText.append(oid+"annot_exp:\""+a.getOWLClass().getIRI().getFragment()+"\"");
			    		  extQueryText.append(" ");
				      }
			      }
		    	  Query query=parser.parse(extQueryText.toString().trim());
		    	  if(Yasemir.DEBUG) System.err.println("[YaSemIr - "+debugStr+"] Searching for: " + query.toString());
		    	 
		    	  TopDocs results = searcher.search(query, MAX_HITS);
				  ScoreDoc[] hits = results.scoreDocs;
				    
				  int numTotalHits = results.totalHits;
				  if(Yasemir.DEBUG) System.err.println("[YaSemIr] "+numTotalHits + " total matching documents");
				  if(numTotalHits > 0) {
			    	for (int i = 0; i < Math.min(numTotalHits, MAX_HITS); i++) {
				        Document doc = searcher.doc(hits[i].doc);
				        String id = doc.get("id");
				        String textAbst=doc.get("text");
				        List<IndexableField> docFields =doc.getFields();
				        StringBuffer concepts = new StringBuffer();
				        for(IndexableField f : docFields){
				        	String fname=f.name();
				        	if(fname.endsWith("annot") || fname.endsWith("annot_exp")) {
				        		concepts.append(fname+":"+doc.get(fname));
				        		concepts.append(" ");
				        	}
				        }
				        RankedDocument srDoc = new RankedDocument(id, textAbst, concepts.toString().trim(), queryAnnotation);
				        srDoc.setWeight(Yasemir.SIM_MEASURE);
				        if(Yasemir.MODE==Yasemir.HYBRID) srDoc.includeClassicWeight(hits[i].score);
				        
				        ret.add(srDoc);
				    }
			    	
			    	
			    }
	    	  }
	    	
	      }
		
		if(Yasemir.MODE!=Yasemir.CLASSIC) Collections.sort(ret);
		return ret;
	}

}
