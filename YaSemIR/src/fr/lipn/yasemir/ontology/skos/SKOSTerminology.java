package fr.lipn.yasemir.ontology.skos;
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

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.semanticweb.owlapi.model.OWLClass;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

/**
 * Class used to access and create a SKOS terminology 
 * @author buscaldi
 *
 */
public class SKOSTerminology {
	boolean isStemmed=false; //used to track whether the terminology is stored in stemmed form or not
	Model model;
	String lang;
	String dc, skos; //NameSpace Prefixes URIs
	private String path;
	private ResIterator sharedResIterator;
	private String ontoID;
	
	/**
	 * Creates an empty terminology connected to the related ontology
	 */
	public SKOSTerminology(String ontoID){
		model = ModelFactory.createDefaultModel();
		this.ontoID=ontoID;
		this.path=ontoID+"_trivial";
	}
	
	public SKOSTerminology(String ontoID, String path){
		model = ModelFactory.createDefaultModel();
		InputStream in = FileManager.get().open( path );
		this.ontoID=ontoID;
		this.path=path;
		
		if (in == null) {
		    throw new IllegalArgumentException("File: " + path + " not found");
		}

		// read the RDF/XML file
		model.read(in, null);
		
		dc = model.getNsPrefixURI("dc");
		skos = model.getNsPrefixURI("skos");
		
		setLanguage();
		
		//this part is for test only: at this point we already loaded the terminology
		/*StmtIterator si = model.listStatements();
		while(si.hasNext()){
			Statement stmt = si.next();
			Resource  subject   = stmt.getSubject();     // get the subject
		    Property  predicate = stmt.getPredicate();   // get the predicate
		    RDFNode   object    = stmt.getObject();      // get the object
		    if(subject.hasURI("http://org.snu.bike/MeSH#tangier_disease")){
		    	System.err.println(stmt);
		    }

		}*/
	}
	
	private void setLanguage(){
		Property P = model.createProperty(dc+"type");
		NodeIterator ni =model.listObjectsOfProperty(P);
		while(ni.hasNext()){
			RDFNode rn = ni.next();
			Literal l = rn.asLiteral();
			String value =l.getLexicalForm();
			if(value.startsWith("stem")) isStemmed=true;
			lang=l.getLanguage();
			
			//System.err.println(lang+ " "+isStemmed);
		}
		
	}
	
	public List<String> getLabels(OWLClass concept){
		Vector<String> labels = new Vector<String>();
		String cID = concept.toStringID();
		Resource subj = model.createResource(cID);
		Property pref = model.createProperty(skos, "prefLabel");
		Property alt = model.createProperty(skos, "altLabel");
		NodeIterator ni = model.listObjectsOfProperty(subj, pref);
		while(ni.hasNext()){
			RDFNode rn = ni.next();
			Literal l = rn.asLiteral();
			labels.add(l.getLexicalForm());
		}
		ni = model.listObjectsOfProperty(subj, alt);
		while(ni.hasNext()){
			RDFNode rn = ni.next();
			Literal l = rn.asLiteral();
			labels.add(l.getLexicalForm());
		}
		return labels;	
	}
	
	
	public void resetIterator(){
		this.sharedResIterator = model.listSubjects();
	}
	
	public boolean hasMoreLabels(){
		return this.sharedResIterator.hasNext();
	}
	
	public Vector<String> getNextLabels(){
		Property pref = model.createProperty(skos, "prefLabel");
		Property alt = model.createProperty(skos, "altLabel");
		
		Vector<String> ret = new Vector<String>();
		
		if(sharedResIterator.hasNext()){
			Resource subj = sharedResIterator.next();
			StringBuffer buf = new StringBuffer();
			
			ret.add(subj.getURI());
			
			NodeIterator ni = model.listObjectsOfProperty(subj, pref);
			while(ni.hasNext()){
				RDFNode rn = ni.next();
				Literal l = rn.asLiteral();
				buf.append(l.getLexicalForm());
				buf.append(" ");
			}
			ni = model.listObjectsOfProperty(subj, alt);
			while(ni.hasNext()){
				RDFNode rn = ni.next();
				Literal l = rn.asLiteral();
				buf.append(l.getLexicalForm());
				buf.append(" ");
			}
			
			ret.add(buf.toString().trim());
		}
		
		return ret;
	}
	/*
	 
	public HashMap<String, Vector<String>> getAllTerminologyData(){
		Property pref = model.createProperty(skos, "prefLabel");
		Property alt = model.createProperty(skos, "altLabel");
		
		HashMap<String, Vector<String>> ret = new HashMap<String, Vector<String>>();
		
		ResIterator ri = model.listSubjects();
		
		while(ri.hasNext()){
			Resource subj = ri.next();
			Vector<String> labels = new Vector<String>();
			NodeIterator ni = model.listObjectsOfProperty(subj, pref);
			while(ni.hasNext()){
				RDFNode rn = ni.next();
				Literal l = rn.asLiteral();
				labels.add(l.getLexicalForm());
			}
			ni = model.listObjectsOfProperty(subj, alt);
			while(ni.hasNext()){
				RDFNode rn = ni.next();
				Literal l = rn.asLiteral();
				labels.add(l.getLexicalForm());
			}
			
			ret.put(subj.getLocalName(), labels);
			
		}
		
		return ret; //NOTE: Too expensive?
		
	}
	*/
	

	/**
	 * This method creates a concept c with label label as a preferred label
	 * @param c
	 * @param label
	 */
	public void makeConceptLabel(OWLClass c, String label){
		if(!label.trim().isEmpty()) {
			String ss = c.getIRI().toString();
			//System.err.println("adding "+ss+" prefLabel:"+label);
			Resource subj = model.createResource(c.getIRI().toString());
			Property pref = model.createProperty(skos, "prefLabel");
			subj.addProperty(pref, label);
		}
	}
	
	public String getTerminologyID() {
		//FIXME: path should not be an ID
		return path;
	}
	
	public void print(){
		model.write(System.out);
	}

	public void setStemming(boolean b) {
		this.isStemmed=b;
	}
	
	public boolean isStemmed(){
		return this.isStemmed;
	}
	
}
