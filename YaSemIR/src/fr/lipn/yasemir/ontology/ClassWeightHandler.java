package fr.lipn.yasemir.ontology;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.semanticweb.owlapi.model.OWLClass;

import fr.lipn.yasemir.configuration.Yasemir;


public class ClassWeightHandler {
	private static int N;
	private static IndexReader reader;
	
	public final static int FIXED=0;
	public final static int IDF=1;
	public final static int PROB=2;
	public final static int GAUSSPROB=3;
	
	private static int WEIGHT_TYPE;
	
	public static void init(IndexReader r){
		reader=r;
		N=reader.numDocs();
		WEIGHT_TYPE=Yasemir.CONCEPT_WEIGHTS;
	}
	
	private static double getProb(OWLClass cls){
		if (cls==null) return 1d;
		String oid = KnowledgeBattery.ontoForScheme(cls.getIRI().getStart()).getOntologyID();
		String text = cls.getIRI().getFragment();
		Term t = new Term(oid+"annot_exp", text);
		
		double tfreq;
		try {
			tfreq = reader.docFreq(t);
			return 1+tfreq/(double)N;
		} catch (IOException e) {
			e.printStackTrace();
			return 0d;
		}
	}
	
	private static double getGaussProb(OWLClass cls){
		double f=6d;
		if (cls==null) return 1d;
		String oid = KnowledgeBattery.ontoForScheme(cls.getIRI().getStart()).getOntologyID();
		String text = cls.getIRI().getFragment();
		Term t = new Term(oid+"annot_exp", text);
		
		double tfreq;
		try {
			tfreq = reader.docFreq(t);
			return f*Math.pow((tfreq/N), 0.5);
		} catch (IOException e) {
			e.printStackTrace();
			return 0d;
		}
	}
	
	private static double getIDF(OWLClass cls){
		if (cls==null) return 1d;
		String oid = KnowledgeBattery.ontoForScheme(cls.getIRI().getStart()).getOntologyID();
		String text = cls.getIRI().getFragment();
		Term t = new Term(oid+"annot_exp", text);
		
		//String rep=cls.toStringID();
		double tfreq;
		try {
			tfreq = reader.docFreq(t);
			return Math.log((double)N/(tfreq+1d));
		} catch (IOException e) {
			e.printStackTrace();
			return 0d;
		}
	}
	
	private static double get1(OWLClass cls){
		return 1d; //method that corresponds to giving all classes the same weight
	}

	public static double getWeight(OWLClass root) {
		switch(WEIGHT_TYPE) {
			case FIXED: return get1(root);
			case IDF: return getIDF(root);
			case PROB: return getProb(root);
			case GAUSSPROB: return getGaussProb(root);
			default: return get1(root);
		}
	}
}
