package fr.lipn.yasemir.ontology.annotation;
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
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.EnglishStemmer;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;
import fr.lipn.yasemir.Yasemir;
import fr.lipn.yasemir.tools.Tools;

/**
 * SemanticAnnotator implementation that uses a terminology index to assign tags to a document
 * @author buscaldi
 *
 */
public class SentenceBasedAnnotator implements SemanticAnnotator {
	private static int MAX_ANNOTS=10;
	private String termIndexPath;
	/**
	 * Base constructor for IndexBasedAnnotator
	 * @param termIndexPath : the path of the term index generated by YaSemIR
	 */
	public SentenceBasedAnnotator(String termIndexPath) {
		this.termIndexPath=termIndexPath;
	}
	/**
	 * Implementation of the annotate method by IndexBasedAnnotator.
	 * 
	 * The input text is splitted in fragments according to punctuation;
	 * every fragment is used as a query and sent to a Lucene SE that
	 * was used to index the terminology (BM25 weight).
	 * Up to the 20 top results returned by the system are taken as the annotation for the
	 * fragment text. All the fragment annotations combined compose the document annotation
	 * that is returned by this method.
	 * 
	 */
	public DocumentAnnotation annotate(String document){
		DocumentAnnotation ret = new DocumentAnnotation();
		
		try {
			IndexReader reader = IndexReader.open(FSDirectory.open(new File(termIndexPath)));
			IndexSearcher searcher = new IndexSearcher(reader);
			searcher.setSimilarity(new BM25Similarity());
			
			
			/*
			document=document.replaceAll("\\[.*?\\]", "").trim();
			//document = document.replaceAll( "\\p{Punct}", " " );
			String [] fragments = document.split("[;:\\.,]");
			*/
			
			String [] fragments = (String[]) getSentences(document).toArray();
			
			for(String ofragment :  fragments) {
				ofragment=ofragment.replaceAll( "\\p{Punct}", " " );
				ofragment=ofragment.trim();
				String sa[] = ofragment.split("(?<=[ \\n])");
				EnglishStemmer st = new EnglishStemmer();
				StringBuffer fbuf= new StringBuffer();
				for(String s : sa){
					st.setCurrent(s.trim());
					st.stem();
					fbuf.append(st.getCurrent());
					fbuf.append(" ");
				}
				
				String fragment=fbuf.toString().trim(); //stemmed fragment
				
				if(fragment.length()==0) continue;
				//System.err.println("Annotating: "+fragment);
						
				QueryParser parser = new QueryParser(Version.LUCENE_44, "labels", Yasemir.analyzer);
				Query query = parser.parse(fragment);
				String stemmedFragment = query.toString("labels").replaceAll("labels:", "");
				
				TopDocs results = searcher.search(query, 20);
			    ScoreDoc[] hits = results.scoreDocs;
			    
			    int numTotalHits = results.totalHits;
			    //System.err.println(numTotalHits + " total matching classes");
			    
			    if(numTotalHits > 0) {
				    hits = searcher.search(query, numTotalHits).scoreDocs;
				    for(int i=0; i< Math.min(numTotalHits, MAX_ANNOTS); i++){
				    	Document doc = searcher.doc(hits[i].doc);
				    	String ptrn = "(?i)("+doc.get("labels").replaceAll(", ", "|")+")";
				    	//System.err.println("OWLClass="+doc.get("id")+" score="+hits[i].score);
				    	if(Tools.checkPattern(stemmedFragment, ptrn)){
				    		//System.err.println("OK: OWLClass="+doc.get("id")+" score="+hits[i].score);
				    		Annotation ann = new Annotation(doc.get("id"));
				    		String ontoID = ann.getRelatedOntology().getOntologyID();
				    		
				    		Vector<Annotation> annotations = ret.get(ontoID);
				    		if(annotations == null) annotations = new Vector<Annotation>();
					    	annotations.add(ann);
					    	ret.put(ontoID, annotations);
				    	}
				    }
			    }
								 
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
		 
	}
	/**
	 * Method that uses DocumentPreprocessor from Stanford Parser to split text into sentences
	 * @param text
	 * @return
	 */
	private Vector<String> getSentences(String text){
		  Vector<String> sentenceList = new Vector<String>();
		  DocumentPreprocessor dp = new DocumentPreprocessor(new StringReader(text));
		  
		  Iterator<List<HasWord>> it = dp.iterator();
		  while (it.hasNext()) {
		     StringBuilder sentenceSb = new StringBuilder();
		     List<HasWord> sentence = it.next();
		     for (HasWord token : sentence) {
		        if(sentenceSb.length()>1) {
		           sentenceSb.append(" ");
		        }
		        sentenceSb.append(token);
		     }
		     sentenceList.add(sentenceSb.toString());
		  }
		  /*
		  for(String sentence:sentenceList) {
		     System.err.println(sentence);
		  }
		  */
		  return sentenceList;
		  
	  }
	
}


