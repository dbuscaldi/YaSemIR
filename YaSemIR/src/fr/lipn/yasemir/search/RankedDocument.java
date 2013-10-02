package fr.lipn.yasemir.search;

import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import org.semanticweb.owlapi.model.OWLClass;

import fr.lipn.yasemir.ontology.KnowledgeBattery;
import fr.lipn.yasemir.ontology.Ontology;
import fr.lipn.yasemir.ontology.annotation.Annotation;
import fr.lipn.yasemir.weighting.ckpd.NGramComparer;
import fr.lipn.yasemir.weighting.ckpd.NGramTerm;
import fr.lipn.yasemir.weighting.ckpd.TermFactory;

public class RankedDocument implements Comparable<RankedDocument> {
	private String docID;
	private Vector<Annotation> docAnnot;
	private Vector<Annotation> queryAnnot;
	private Float weight;
	private String text;
	
	/**
	 * constructor to be used if CKPD or classic search are used
	 * @param docID
	 * @param text
	 * @param annotations
	 */
	public RankedDocument(String docID, String text, HashMap<String, Vector<Annotation>> queryAnnot){
		this.docID=docID;
		this.docAnnot=new Vector<Annotation>();
		this.text=text;
	}
	
	public RankedDocument(String docID, String text, String docAnnAsText, HashMap<String, Vector<Annotation>> queryAnnot){
		this.docID=docID;
		this.queryAnnot = new Vector<Annotation>();
		this.text=text;
		
		for(String k : queryAnnot.keySet()){
			this.queryAnnot.addAll(queryAnnot.get(k));
		}
		
		this.docAnnot=new Vector<Annotation>();
		String [] tAnns = (docAnnAsText.trim()).split(" ");
		for(String c : tAnns){
			//FIXME: annotations with or without oid during indexing??? (cutpos sometimes -1)
			//System.err.println("c --> "+c);
			if(!c.equals("") && !c.equals("Thing")){ //FIXME: why do we have annotations with Thing???
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
		this.weight=new Float(0);
	}
	
	/**
	 * This method set the document weight to a fixed score (to be used by classic search)
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
			clWeights[i]=ClassWeightHandler.get1(bestLocalRoot);
			//clWeights[i]=ClassWeightHandler.getIDF(bestLocalRoot);
			//clWeights[i]=ClassWeightHandler.getProb(bestLocalRoot);
			//clWeights[i]=ClassWeightHandler.getGaussProb(bestLocalRoot);
			
			//if(bestLocalRoot != null && clWeights[i] > 1) System.err.println(bestLocalRoot.toStringID()+" "+clWeights[i]);
		}
		float sum=0f;
		double clwSum=0d;
		for(double d : clWeights) clwSum+=d;
		for(int k=0; k< cWeights.length; k++) {
			sum+=((float)clWeights[k]*cWeights[k]);
		}
		//sum=sum/(float)cWeights.length; //normalisation (we don't have roots weights w/r to TextViz
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
	
	public void includeClassicWeight(float score) {
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
		
		this.includeCKPDWeight(CKPDweight);
		
	}
	
	/**
	 * Combine n-gram based weight using combMNZ
	 * @param score
	 */
	public void includeCKPDWeight(float score) {
		float w= this.weight.floatValue();
		this.weight=new Float(combMNZ(w,score));
	}
	
	/**
	 * Calcultes the combMNZ score
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
	
	public String toString(){
		StringBuffer tmp = new StringBuffer();
		if(this.docAnnot != null) {
		tmp.append("Annotations:\n");
			for(Annotation a : docAnnot) {
				tmp.append(a.getOWLClass());
				tmp.append(" ");
			}
		}
		tmp.append("\nText:\n");
		tmp.append(formatTextWidth(text.toString(), 120));
		return tmp.toString();
	}
	
	private static String formatTextWidth(String input, int maxLineLength) {
	    String [] fragments = input.split("[\\s]+");
	    StringBuilder output = new StringBuilder(input.length());
	    int lineLen = 0;
	    for(String word: fragments) {
	       
	        if (lineLen + word.length() > maxLineLength) {
	            output.append("\n");
	            lineLen = 0;
	        }
	        output.append(word);
	        output.append(" ");
	        lineLen += (word.length()+1);
	    }
	    return output.toString();
	}
	
	
}
