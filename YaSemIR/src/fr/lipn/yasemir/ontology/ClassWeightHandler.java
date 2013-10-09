package fr.lipn.yasemir.ontology;
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
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.semanticweb.owlapi.model.OWLClass;

import fr.lipn.yasemir.Yasemir;

/**
 * This class provides weights for concepts, depending on the parameters contained in the configuration file.
 * FIXED: all concepts have the same weight (1)
 * IDF: concepts have a weight according to their idf
 * PROB: concepts have a weight according to their frequency
 * GAUSSPROB: concepts have a weight according to an estimation of their frequency
 * 
 * @author buscaldi
 *
 */
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
