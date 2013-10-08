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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.semanticweb.owlapi.model.OWLClass;
import org.tartarus.snowball.ext.EnglishStemmer;

/**
 * SemanticAnnotator implementation that uses a terminology index to assign tags to a document
 * @author buscaldi
 *
 */
public class IndexBasedAnnotator implements SemanticAnnotator {
	private static int MAX_ANNOTS=10;
	private String termIndexPath;
	/**
	 * Base constructor for IndexBasedAnnotator
	 * @param termIndexPath : the path of the term index generated by YaSemIR
	 */
	public IndexBasedAnnotator(String termIndexPath) {
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
	public HashMap<String, Vector<Annotation>> annotate(String document){
		HashMap<String, Vector<Annotation>> ret = new HashMap<String, Vector<Annotation>>();
		
		try {
			IndexReader reader = IndexReader.open(FSDirectory.open(new File(termIndexPath)));
			IndexSearcher searcher = new IndexSearcher(reader);
			searcher.setSimilarity(new BM25Similarity());
			
			//Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_44);
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_44);
			
			document=document.replaceAll("Support, .+?;", "");
			document=document.replaceAll("\\[.*?\\]", "").trim();
			//document = document.replaceAll( "\\p{Punct}", " " );
			String [] fragments = document.split("[;:\\.,]");
			
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
						
				QueryParser parser = new QueryParser(Version.LUCENE_44, "labels", analyzer);
				Query query = parser.parse(fragment);
				//System.err.println("Searching for: " + query.toString("terms"));
				
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
				    	if(checkPattern(fragment, ptrn)){
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
	
	private boolean checkPattern(String text, String pattern){
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(text.toLowerCase());
		
		if(m.find()) {
			//System.err.println("found pattern: "+m.group());
			return true;
		}
		return false;
	}
	
	public void addSemanticAnnotation(Document doc, String text){
		HashMap<String, Vector<Annotation>> anns = this.annotate(text);
		for(String oid : anns.keySet()){
			StringBuffer av_repr = new StringBuffer();
			for(Annotation a : anns.get(oid)){
				av_repr.append(a.getOWLClass().getIRI().getFragment());
				av_repr.append(" ");
				//String ontoID=a.getRelatedOntology().getOntologyID();
			}
			//doc.add(new Field(oid+"annot", av_repr.toString().trim(), Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new TextField(oid+"annot", av_repr.toString().trim(), Field.Store.YES));
			//we add the annotation and the supertypes to an extended index to be used during the beginning of the search process
			Set<OWLClass> expansion = new HashSet<OWLClass>();
			for(Annotation a : anns.get(oid)){
				Set<OWLClass> sup_a = a.getRelatedOntology().getAllSuperClasses(a.getOWLClass());
				Set<OWLClass> roots = a.getRelatedOntology().getOntologyRoots(); //this is needed to calculate the frequencies of root classes
				roots.retainAll(sup_a);
				expansion.addAll(sup_a);
			}
			for(OWLClass c : expansion){
				av_repr.append(c.getIRI().getFragment());
				av_repr.append(" ");
			}
			//doc.add(new Field(oid+"annot_exp", av_repr.toString().trim(), Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new TextField(oid+"annot_exp", av_repr.toString().trim(), Field.Store.YES));
		}
	}

	public Collection<? extends Annotation> extractCategories(List<String> categoryList) {
		Vector<Annotation> annotations = new Vector<Annotation>();
		for(String catID : categoryList){
			Annotation ann = new Annotation(catID);
			annotations.add(ann);
		}
		return annotations;
	}
	
}

class WeightedTag {
	String tag;
	Double w;
	
	WeightedTag(String t, double w){
		this.tag=t;
		this.w=new Double(w);
	}

	public String getName(){
		return tag;
	}
	
	public Double getWeight() {
		return w;
	}
	
	
}

class TagComparator implements Comparator<WeightedTag>{
	 
    @Override
    public int compare(WeightedTag t1, WeightedTag t2) {
    	Double w1=t1.getWeight();
    	Double w2=t2.getWeight();
    	
    	return -w1.compareTo(w2);
        
    }
}
