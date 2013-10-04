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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.semanticweb.owlapi.model.OWLClass;
import org.tartarus.snowball.ext.EnglishStemmer;

import fr.lipn.yasemir.configuration.Yasemir;

/**
 * This class implements the K-NN annotator by Trieschnigg et al., MeSH Up: effective MeSH text classification for document retrieval
 * @author buscaldi
 *
 */
public class KNNAnnotator implements SemanticAnnotator {
	String termIndexPath;
	//private boolean K_NN = true;
	private final static int K = 10;
	private final static int N = 1000; //limit of document search for K_NN
	private final static int maxTags=5; //limit of tags to extract with the K_NN method
	private String standardIndexPath;
	
	public KNNAnnotator(String termIndexPath) {
		this.standardIndexPath=Yasemir.INDEX_DIR; //uses already indexed documents as training index
		this.termIndexPath=termIndexPath;
	}
	
	/**
	 * The first parameter is a "training" index, where documents have been already annotated
	 * The second parameter is the terminology index
	 * @param standardIndexDir
	 * @param termIndexPath
	 */
	public KNNAnnotator(String standardIndexDir, String termIndexPath) {
		this.standardIndexPath=standardIndexDir; //indexed training collection
		this.termIndexPath=termIndexPath;
	}
	
	public HashMap<String, Vector<Annotation>> annotate(String document){
		HashMap<String, Vector<Annotation>> ret = new HashMap<String, Vector<Annotation>>();
		
		try {
			IndexReader reader = IndexReader.open(FSDirectory.open(new File(termIndexPath)));
			IndexSearcher searcher = new IndexSearcher(reader);
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
				
				//use K-NN annotation (see Trieschnigg et al. 2009)
				IndexReader docreader = IndexReader.open(FSDirectory.open(new File(this.standardIndexPath)));
				IndexSearcher docsearcher = new IndexSearcher(docreader);
				
				QueryParser parser = new QueryParser(Version.LUCENE_44, "text", analyzer);
				Query query = parser.parse(fragment);
				System.err.println("Looking for: "+query);
				TopDocs results = docsearcher.search(query, N); //get the first 100 documents
			    ScoreDoc[] hits = results.scoreDocs;
			    
			    int topLimit = Math.min(results.totalHits, K);
			    int bottomLimit = Math.min(results.totalHits, N)-K;
			    int numTotalHits = Math.min(results.totalHits, N);
			    
			    //System.err.println("top:"+topLimit+" bottom:"+bottomLimit+" total:"+numTotalHits);
			    HashMap<String, Double> ttags = new HashMap<String, Double>();
			    HashMap<String, Integer> btags = new HashMap<String, Integer>();
			    if(topLimit < bottomLimit){
			    	//Get the tags used in the top K documents matching the request
				    hits = docsearcher.search(query, numTotalHits).scoreDocs;
					for(int i=0; i<topLimit; i++){
					  	Document doc = docsearcher.doc(hits[i].doc);
					  	Vector<String> tags = new Vector<String>();
					  	List<IndexableField> docFields =doc.getFields();
				        for(IndexableField f : docFields){
				        	String fname=f.name();
				        	if(fname.endsWith("annot")) {
				        		tags.add(fname+":"+doc.get(fname));
				        	}
				        }
					   	
					   	String [] tagStrings = (String[]) tags.toArray();
					    for(String t : tagStrings){
					    	t=t.replaceAll("\\W|_", " ");
					    	Double nt = ttags.get(t);
					    	if (nt==null) nt= new Double(hits[i].score);
					    	else nt = new Double(hits[i].score+nt.doubleValue());
					    	ttags.put(t, nt);
					   	}
					}
					for(int i=bottomLimit; i<numTotalHits; i++){
					  	Document doc = docsearcher.doc(hits[i].doc);
					  	Vector<String> tags = new Vector<String>();
					  	List<IndexableField> docFields =doc.getFields();
				        for(IndexableField f : docFields){
				        	String fname=f.name();
				        	if(fname.endsWith("annot")) {
				        		tags.add(fname+":"+doc.get(fname));
				        	}
				        }
					   	
					   	String [] tagStrings = (String[]) tags.toArray();
					    for(String t : tagStrings){
					    	t=t.replaceAll("\\W|_", " ");
					    	Integer nt = btags.get(t);
					    	if (nt==null) nt= new Integer(1);
					    	else nt = new Integer((nt.intValue()+1));
					    	btags.put(t, nt);
					   	}
					}
				    
			    }
			    
			    Vector<WeightedTag> tagv=new Vector<WeightedTag>();
			    //now find, for all tags, the corresponding MeSH concepts
			    double sum=0;
			    for(String tag : ttags.keySet()){
			    	double tagStrength = ttags.get(tag).doubleValue();
			    	double compStrength = 0;
			    	if(btags.containsKey(tag)){
			    		compStrength=(btags.get(tag).doubleValue())/((double)K);
			    	}
			    	//System.err.println(tag+ " :str="+tagStrength+", comp="+compStrength);
			    	double weight=tagStrength*(1-compStrength);
			    	sum+=weight;
			    	tagv.add(new WeightedTag(tag, weight));
			    }
			    double avg=sum/(double)tagv.size();
			    
			    double ssum=0;
			    for(WeightedTag wt : tagv){
			    	ssum+=Math.sqrt(Math.pow(wt.getWeight()-avg, 2d));
			    }
			    double stddev=ssum/(double)tagv.size();
			    
			    //System.err.println("avg w: "+avg+" stddev:"+stddev+" limit:"+(avg+2*stddev));
			    double limit = (avg+2*stddev); //definition of statistic outlier
			    
			    TagComparator comparator = new TagComparator();
			    Collections.sort(tagv, comparator);
			   
			    int i=0;
			    for(WeightedTag wt : tagv){
			    	String tag = wt.getName();
			    	if(i>=maxTags) break;
			    	if(wt.getWeight() >= limit) {
				    	QueryParser tagparser = new QueryParser(Version.LUCENE_44, "labels", analyzer);
						Query tagquery = tagparser.parse("\""+tag+"\"");
						
						TopDocs tagresults = searcher.search(tagquery, 5);
					    ScoreDoc[] taghits = tagresults.scoreDocs;
					    
					    int numTagTotalHits = tagresults.totalHits;
					    
					    if(numTagTotalHits > 0) {
						    taghits = searcher.search(tagquery, numTagTotalHits).scoreDocs;
						    Document doc = searcher.doc(taghits[0].doc);
						    
						    Annotation ann = new Annotation(doc.get("id"));
					    	//System.err.println("Adding: "+tag+" w:"+wt.getWeight());
					    	String ontoID = ann.getRelatedOntology().getOntologyID();
				    		
				    		Vector<Annotation> annotations = ret.get(ontoID);
				    		if(annotations == null) annotations = new Vector<Annotation>();
					    	annotations.add(ann);
					    	ret.put(ontoID, annotations);
					    	
							i++;
					    }
			    	}
				    
			    }
			    docreader.close();
				
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
		 
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
			doc.add(new Field(oid+"annot", av_repr.toString().trim(), Field.Store.YES, Field.Index.ANALYZED));
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
			doc.add(new Field(oid+"annot_exp", av_repr.toString().trim(), Field.Store.YES, Field.Index.ANALYZED));
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
