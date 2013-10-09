package fr.lipn.yasemir.search;
/*
 * Copyright (C) 2013, Université Paris Nord
 *
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
import java.io.File;
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
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import fr.lipn.yasemir.Yasemir;
import fr.lipn.yasemir.ontology.ClassWeightHandler;
import fr.lipn.yasemir.ontology.annotation.Annotation;
import fr.lipn.yasemir.weighting.ckpd.NGramTerm;
import fr.lipn.yasemir.weighting.ckpd.TermFactory;

/**
 * Class providing the methods to search semantically a string
 * @author buscaldi
 *
 */
public class SemanticSearcher {
	IndexReader reader;
	IndexSearcher searcher;
	QueryParser parser;
	String basefield = "text";
	
	public static int MAX_HITS=1000;
	/**
	 * Initializes a SemanticSearcher with the given language and IndexReader
	 * @param lang
	 * @param reader
	 * @throws IOException 
	 */
	public SemanticSearcher() throws IOException {
		reader = IndexReader.open(FSDirectory.open(new File(Yasemir.INDEX_DIR)));
		
		this.searcher = new IndexSearcher(reader);
	    if(Yasemir.SCORE.equals("BM25")) searcher.setSimilarity(new BM25Similarity());
	    
	    ClassWeightHandler.init(reader);
	    if(Yasemir.CKPD_ENABLED) TermFactory.init(reader, Yasemir.analyzer);
	    
		parser = new QueryParser(Version.LUCENE_44, basefield, Yasemir.analyzer);
	}
	/**
	 * Method that returns an ordered list of RankedDocument instances
	 */
	public Vector<RankedDocument> search(String line) throws ParseException, IOException {
		Vector<RankedDocument> ret = new Vector<RankedDocument>();
		
		Vector<NGramTerm> queryNGT = null;
		if(Yasemir.CKPD_ENABLED) queryNGT = TermFactory.makeTermSequence(line);
		
		String debugStr;
  	  	if(Yasemir.MODE==Yasemir.SEMANTIC) debugStr="SEMANTIC";
  	  	else if(Yasemir.MODE==Yasemir.CLASSIC) debugStr="CLASSIC";
  	  	else debugStr="HYBRID";
  	  	
  	  	if(Yasemir.MODE==Yasemir.CLASSIC || Yasemir.MODE==Yasemir.HYBRID){
	    	  Query query = parser.parse(line);
	    	  
	    	  if(Yasemir.DEBUG) System.err.println("[YaSemIr - "+debugStr+"] Searching for: " + query.toString(basefield));
	            
		      searcher.search(query, null, MAX_HITS);
		      
		      TopDocs results = searcher.search(query, MAX_HITS);
			  ScoreDoc[] hits = results.scoreDocs;
			    
			  int numTotalHits = results.totalHits;
			  if(Yasemir.DEBUG) System.err.println("[YaSemIr] "+numTotalHits + " total matching documents");
			  if(numTotalHits > 0) {
		    	for (int i = 0; i < Math.min(numTotalHits, MAX_HITS); i++) {
			        Document doc = searcher.doc(hits[i].doc);
			        List<IndexableField> docFields =doc.getFields();
			        String id = doc.get("id");
			        String textAbst=doc.get("text");
			        float docWeight = hits[i].score;
			        StringBuffer concepts = new StringBuffer();
			        StringBuffer parents = new StringBuffer();
			        for(IndexableField f : docFields){
			        	String fname=f.name();
			        	if(fname.endsWith("annot")) {
			        		concepts.append(fname+":"+doc.get(fname));
			        		concepts.append(" ");
			        	} else if(fname.endsWith("annot_exp")){
			        		parents.append(fname+":"+doc.get(fname));
			        		parents.append(" ");
			        	}
			        }
			        

			        RankedDocument clDoc = new RankedDocument(id, textAbst, concepts.toString(), parents.toString());
			        clDoc.setWeight(docWeight);
			        
			        if(Yasemir.CKPD_ENABLED) clDoc.setCKPDWeight(queryNGT);
			        
			        ret.add(clDoc);
			        
		    	}
		     }
	      }
  	  	  if(Yasemir.MODE==Yasemir.SEMANTIC || Yasemir.MODE==Yasemir.HYBRID){
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
		      
		      if(queryAnnotation.isEmpty()) return ret;
		      
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
			        StringBuffer parents = new StringBuffer();
			        for(IndexableField f : docFields){
			        	String fname=f.name();
			        	if(fname.endsWith("annot")) {
			        		concepts.append(fname+":"+doc.get(fname));
			        		concepts.append(" ");
			        	} else if(fname.endsWith("annot_exp")){
			        		parents.append(fname+":"+doc.get(fname));
			        		parents.append(" ");
			        	}
			        }
			        RankedDocument srDoc = new RankedDocument(id, textAbst, concepts.toString(), parents.toString(), queryAnnotation);
			        srDoc.setWeight(Yasemir.SIM_MEASURE);

			        
			        if(Yasemir.MODE==Yasemir.SEMANTIC) ret.add(srDoc);
			        else {
			        	int pos = ret.indexOf(srDoc);
			        	if(pos > -1) {
			        		ret.elementAt(pos).fuseWeights(srDoc.getScore());
			        	} else ret.add(srDoc);
			        }
			    }
		    	
		    	
		    }

	      }
		
		if(Yasemir.MODE!=Yasemir.CLASSIC) Collections.sort(ret);
		return ret;
	}
	
	public void close() throws IOException{
		this.reader.close();
	}

}
