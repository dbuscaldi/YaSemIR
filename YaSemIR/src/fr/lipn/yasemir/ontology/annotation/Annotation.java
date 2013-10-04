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
import org.semanticweb.owlapi.model.OWLClass;

import fr.lipn.yasemir.ontology.KnowledgeBattery;
import fr.lipn.yasemir.ontology.Ontology;
/**
 * This class represents a document annotation
 * @author buscaldi
 *
 */
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
