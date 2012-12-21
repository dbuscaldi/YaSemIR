package fr.lipn.yasemir.ontology.annotation;

import org.semanticweb.owlapi.model.OWLClass;

import fr.lipn.yasemir.ontology.Ontology;

public class Annotation {
	private String cname;
	private OWLClass owlClass;

	public Annotation(String str) {
		cname=str;
		owlClass=Ontology.classForID(str);
	}
	
	public OWLClass getOWLClass(){
		return owlClass;
	}
	
	public String toString(){
		return cname;
	}

}
