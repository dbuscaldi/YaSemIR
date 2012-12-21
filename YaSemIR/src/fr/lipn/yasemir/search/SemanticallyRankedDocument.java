package fr.lipn.yasemir.search;

import java.util.Set;
import java.util.Vector;

import org.semanticweb.owlapi.model.OWLClass;

import fr.lipn.yasemir.ontology.ConceptSimilarity;
import fr.lipn.yasemir.ontology.Ontology;
import fr.lipn.yasemir.ontology.annotation.Annotation;

public class SemanticallyRankedDocument implements Comparable<SemanticallyRankedDocument> {
	private String docID;
	private Vector<Annotation> docAnnot;
	private Vector<Annotation> queryAnnot;
	private Float weight;
	
	public SemanticallyRankedDocument(String docID, String docAnnAsText, Vector<Annotation> queryAnnot){
		this.docID=docID;
		this.queryAnnot=queryAnnot;
		this.docAnnot=new Vector<Annotation>();
		String [] classes = (docAnnAsText.trim()).split(" ");
		for(String c : classes){
			if(!c.equals("")){
				c=Ontology.getBaseAddr()+c; //rebuild class ID from index file - problems due to the prefix http:
				docAnnot.add(new Annotation(c));
			}
		}
		this.weight=new Float(0);
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
				Set<OWLClass> roots = Ontology.comparableRoots(q, d);
				if(!roots.isEmpty()){
					OWLClass localRoot=roots.iterator().next();
					//OWLClass lcs = Ontology.leastCommonSubsumer(q, d); //not needed
					float tmp = ConceptSimilarity.computeSimilarity(measure, q, d, localRoot);					
					//System.err.println("Least common subsumer of "+q+" and "+" "+d+" : "+lcs);
					if(tmp > max) {
						max = tmp;
						bestLocalRoot=localRoot;
						//System.err.println("Best match: "+q+" and "+d+ " -> weight: "+tmp);
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
	public int compareTo(SemanticallyRankedDocument o) {
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
	
	public void includeCKPDWeight(float score) {
		float w= this.weight.floatValue();
		this.weight=new Float(combMNZ(w,score));
	}
	
	private float combMNZ(float scorea, float scoreb){
		float nz=0f;
		if(scorea==0 && scoreb==0) return 0f;
		else {
			if(scorea>0 && scoreb>0) nz=2f;
			else nz=1f;
		}
		return (scorea+scoreb)/nz;
	}
	
	
	
}
