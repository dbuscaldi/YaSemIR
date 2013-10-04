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
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import org.semanticweb.owlapi.model.OWLClass;

import fr.lipn.yasemir.ontology.ClassWeightHandler;
import fr.lipn.yasemir.ontology.KnowledgeBattery;
import fr.lipn.yasemir.ontology.Ontology;
import fr.lipn.yasemir.ontology.annotation.Annotation;
import fr.lipn.yasemir.weighting.ckpd.NGramComparer;
import fr.lipn.yasemir.weighting.ckpd.NGramTerm;
import fr.lipn.yasemir.weighting.ckpd.TermFactory;
/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 * @author buscaldi
 *
 */
public class RankedDocument implements Comparable<RankedDocument> {
	private String docID;
	private Vector<Annotation> docAnnot; //concepts found in document
	private Vector<Annotation> parentAnnot; //parents derived from document annotation
	private Vector<Annotation> queryAnnot;
	private Float weight;
	private String text;
	
	/**
	 * constructor to be used if CKPD or classic search are used
	 * @param docID
	 * @param text
	 * @param annotations
	 */
	public RankedDocument(String docID, String text, String docAnnAsText, String parentsAsText){
		this.docID=docID;
		this.text=text;
		
		this.docAnnot=new Vector<Annotation>();
		this.parentAnnot= new Vector<Annotation>();
		
		String [] tAnns = (docAnnAsText.trim()).split(" ");
		String [] pAnns = (parentsAsText.trim()).split(" ");
		for(String c : tAnns){
			if(!c.equals("") && !c.equals("Thing")){
				String [] els = c.split(":");
				int cutpos = els[0].indexOf("annot");
				String oid= null;
				if(cutpos != -1) {
					oid = els[0].substring(0, cutpos);
					//System.err.println("recomposed: "+oid+":"+els[1]);
					docAnnot.add(new Annotation(oid, els[1]));
					
				} else {
					oid = KnowledgeBattery.ontoForClassID(c).getOntologyID();
					//System.err.println("recomposed: "+oid+":"+c);
					docAnnot.add(new Annotation(oid, c));
				}
				
			}
		}
		
		for(String p : pAnns){
			if(!p.equals("") && !p.equals("Thing")){
				String [] els = p.split(":");
				int cutpos = els[0].indexOf("annot_exp");
				String oid= null;
				
				if(cutpos != -1) {
					oid = els[0].substring(0, cutpos);
					parentAnnot.add(new Annotation(oid, els[1]));
					
				} else {
					oid = KnowledgeBattery.ontoForClassID(p).getOntologyID();
					parentAnnot.add(new Annotation(oid, p));
				}
				
			}
		}
	}
	/**
	 * Constructor to be used for semantic or hybrid search
	 * @param docID
	 * @param text
	 * @param docAnnAsText
	 * @param parentsAsText
	 * @param queryAnnot
	 */
	public RankedDocument(String docID, String text, String docAnnAsText, String parentsAsText, HashMap<String, Vector<Annotation>> queryAnnot){
		this.docID=docID;
		this.queryAnnot = new Vector<Annotation>();
		
		this.text=text;
		
		for(String k : queryAnnot.keySet()){
			this.queryAnnot.addAll(queryAnnot.get(k));
		}
		
		this.docAnnot=new Vector<Annotation>();
		this.parentAnnot= new Vector<Annotation>();
		String [] tAnns = (docAnnAsText.trim()).split(" ");
		String [] pAnns = (parentsAsText.trim()).split(" ");
		for(String c : tAnns){
			if(!c.equals("") && !c.equals("Thing")){
				String [] els = c.split(":");
				int cutpos = els[0].indexOf("annot");
				String oid= null;
				
				if(cutpos != -1) {
					oid = els[0].substring(0, cutpos);
					//System.err.println("recomposed: "+oid+":"+els[1]);
					docAnnot.add(new Annotation(oid, els[1]));
					
				} else {
					oid = KnowledgeBattery.ontoForClassID(c).getOntologyID();
					//System.err.println("recomposed: "+oid+":"+c);
					docAnnot.add(new Annotation(oid, c));
				}
				
			}
		}
		
		for(String p : pAnns){
			if(!p.equals("") && !p.equals("Thing")){
				String [] els = p.split(":");
				int cutpos = els[0].indexOf("annot_exp");
				String oid= null;
				
				if(cutpos != -1) {
					oid = els[0].substring(0, cutpos);
					parentAnnot.add(new Annotation(oid, els[1]));
					
				} else {
					oid = KnowledgeBattery.ontoForClassID(p).getOntologyID();
					parentAnnot.add(new Annotation(oid, p));
				}
				
			}
		}
		
		this.weight=new Float(0);
	}
	
	/**
	 * This method set the document weight to a fixed score (this method is not intended to be used in semantic search mode)
	 */
	public void setWeight(float score){
		this.weight= new Float(score);
	}
	
	/**
	 * This method calculates the weight of the document with respect to the query, using the concept similarity measure
	 * @param measure
	 */
	public void setWeight(int measure){
		if(docAnnot.size()==0) {
			this.weight=new Float(0);
			return;
		}
		//for all concepts in query, get the max similarity with the concepts in document, then normalize
		float [] cWeights =  new float[queryAnnot.size()]; //concept weights
		double [] clWeights = new double[queryAnnot.size()]; //weights for the root classes associated to the cWeights
		for(int i=0; i< queryAnnot.size(); i++){
			Annotation qa = queryAnnot.elementAt(i);
			float max=0f;
			OWLClass bestLocalRoot=null;
			OWLClass q= qa.getOWLClass();
			for(int j=0; j < docAnnot.size(); j++){
				Annotation da = docAnnot.elementAt(j);
				OWLClass d = da.getOWLClass();
				if(da.fromSameOntology(qa)) {
					Ontology o = qa.getRelatedOntology();
					Set<OWLClass> roots = o.comparableRoots(q, d);
					if(!roots.isEmpty()){
						OWLClass localRoot=roots.iterator().next();
						//OWLClass lcs = Ontology.leastCommonSubsumer(q, d); //not needed
						float tmp = o.computeSimilarity(measure, q, d, localRoot);					
						//System.err.println("Least common subsumer of "+q+" and "+" "+d+" : "+lcs);
						if(tmp > max) {
							max = tmp;
							bestLocalRoot=localRoot;
							//System.err.println("Best match: "+q+" and "+d+ " -> weight: "+tmp);
						}
					}
				}
			}
			cWeights[i]=max;
			clWeights[i]=ClassWeightHandler.getWeight(bestLocalRoot);
			
			//if(bestLocalRoot != null && clWeights[i] > 1) System.err.println(bestLocalRoot.toStringID()+" "+clWeights[i]);
		}
		float sum=0f;
		double clwSum=0d;
		for(double d : clWeights) clwSum+=d;
		for(int k=0; k< cWeights.length; k++) {
			sum+=((float)clWeights[k]*cWeights[k]);
		}
		sum=sum/((float)clwSum); //normalisation with weights for each localRoot
		//System.err.println("weight for document "+this.docID+" : "+sum);
		this.weight=new Float(sum);
	}

	@Override
	public int compareTo(RankedDocument o) {
		return (-this.weight.compareTo(o.weight));
	}
	
	public float getScore() {
		return this.weight.floatValue();
	}
	
	public String getID() {
		return this.docID;
	}
	
	/**
	 * Combine weights using combMNZ
	 * @param score
	 */
	public void fuseWeights(float score) {
		float w= this.weight.floatValue();
		this.weight=new Float(combMNZ(w,score));
	}
	/**
	 * Calculates and include n-gram based weight
	 * @param queryNGSet
	 */
	public void setCKPDWeight(Vector<NGramTerm> queryNGSet) {
		if(queryNGSet==null) return;
		Vector<NGramTerm> tv = TermFactory.makeTermSequence(text);
		float CKPDweight=new Float(NGramComparer.compare(queryNGSet, tv));
		
		this.fuseWeights(CKPDweight);
	}
	
	/**
	 * Method that calcultes the combMNZ score
	 * @param scorea
	 * @param scoreb
	 * @return
	 */
	private float combMNZ(float scorea, float scoreb){
		float nz=0f;
		if(scorea==0 && scoreb==0) return 0f;
		else {
			if(scorea>0 && scoreb>0) nz=2f;
			else nz=1f;
		}
		return (scorea+scoreb)/nz;
	}
	
	/**
	 * Returns the annotation list as a single String
	 * @return
	 */
	public String getBaseAnnotations(){
		StringBuffer tmp = new StringBuffer();
		if(this.docAnnot != null) {
		//tmp.append("Annotations:\n");
			for(Annotation a : docAnnot) {
				tmp.append(a.getOWLClass());
				tmp.append(" ");
			}
		}
		return tmp.toString().trim();
	}
	/**
	 * Returns the subsumers list as a single string
	 * @return
	 */
	public String getSubsumerAnnotations(){
		StringBuffer tmp = new StringBuffer();
		if(this.parentAnnot != null) {
		//tmp.append("Subsumers:\n");
			for(Annotation a : parentAnnot) {
				tmp.append(a.getOWLClass());
				tmp.append(" ");
			}
		}
		return tmp.toString().trim();
	}
	/**
	 * Returns the document text
	 * @return
	 */
	public String getText() {
		return this.text;
	}
	
	public boolean equals(Object o){
		return this.docID.equals(((RankedDocument)o).docID);
	}
	/*
	public boolean equals(RankedDocument d){
		return this.docID.equals(d.docID);
	}
	*/
	public int hashCode(){
		return this.docID.hashCode();
	}
	
	
}
