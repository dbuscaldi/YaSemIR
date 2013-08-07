package fr.lipn.yasemir.ontology.annotation;

import org.semanticweb.owlapi.model.OWLClass;

import fr.lipn.yasemir.ontology.KnowledgeBattery;
import fr.lipn.yasemir.ontology.Ontology;

public class Annotation {
	private String cname;
	private OWLClass owlClass;
	private Ontology refOnto;

	public Annotation(String str) {
		cname=str;
		refOnto=KnowledgeBattery.ontoForClassID(str);
		if(refOnto != null) owlClass=refOnto.classForID(str);
		else owlClass=null; //NOTE: class not found in the KB!
	}
	
	public Annotation(String oid, String str) {
		cname=str;
		refOnto=KnowledgeBattery.ontoForID(oid);
		if(refOnto != null) owlClass=refOnto.classForID(str);
		else owlClass=null; //NOTE: class not found in the KB!
	}

	public OWLClass getOWLClass(){
		return owlClass;
	}
	
	public String toString(){
		return cname;
	}
	
	public Ontology getRelatedOntology(){
		return refOnto;
	}
	
	public boolean fromSameOntology(Annotation another){
		return another.refOnto.getOntologyID().equals(this.refOnto.getOntologyID());
	}

}
