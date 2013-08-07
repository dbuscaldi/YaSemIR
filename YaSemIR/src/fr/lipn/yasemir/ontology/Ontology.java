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

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.tartarus.snowball.ext.EnglishStemmer;

import fr.lipn.yasemir.ontology.skos.SKOSTerminology;

public class Ontology {
	private OWLOntology onto;
	private OWLClass root;
	
	/**
	 * Constructor that uses owl:Thing as root concept
	 * @param ontologyFileLocation
	 */
	public Ontology(String ontologyFileLocation){
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        File file = new File(ontologyFileLocation);
        // Now load the local copy
        try {
			onto = manager.loadOntologyFromOntologyDocument(file);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			onto = null;
		}
        Set<OWLClass> allClasses = onto.getClassesInSignature(true);
        for(OWLClass c : allClasses) {
        	if(c.isTopEntity()){
        		this.root=c;
        		break;
        	}
        }
        
        if(this.root==null) System.err.println("[YaSemIR]: ERROR: No root class found!!!");
        else System.err.println("[YaSemIR]: Warning: no root class given, using "+this.root);
        
        System.err.println("[YaSemIR]: Loaded ontology: " + onto+ " with root class "+this.root);
	}
	
	public String getOntologyID() {
		MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
			md5.update(this.getBaseAddr().getBytes());
			BigInteger hash = new BigInteger(1, md5.digest());
			hash.toString(32);
			return (hash.toString(32));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return ""; //never attained
	}
	
	/**
	 * Constructor that specifies a root concept different than OWL:Thing
	 * @param ontologyFileLocation
	 * @param root
	 */
	public Ontology(String ontologyFileLocation, String root){
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        File file = new File(ontologyFileLocation);
        // Now load the local copy
        try {
			onto = manager.loadOntologyFromOntologyDocument(file);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			onto = null;
		}
		this.root = classForID(root);
		
		System.err.println("[YaSemIR]: Loaded ontology: " + onto + " with root class "+root);
	}
	/**
	 * Returns the namespace of the ontoogy followed by a "#" symbol
	 * @return
	 */
	public String getBaseAddr(){
		return onto.getOntologyID().getOntologyIRI().toString()+"#"; //FIXME: lasciare il # o no?
		//return "http://org.snu.bike/MeSH#";
	}
	/**
	 * Two classes are comparable if they share at least a part of their concept path
	 * @param a
	 * @param b
	 * @return
	 */
	public Set<OWLClass> comparableRoots(OWLClass a, OWLClass b){
		Set<OWLClass> pa = getConceptSuperTypes(a);
		Set<OWLClass> pb = getConceptSuperTypes(b);
		pa.retainAll(pb);
		return pa; //if this is not empty, the classes are comparable; if its size is more than 1, then we have multiple inheritance		
	}
	
	/**
	 * Method that returns the top concepts for the given OWL Class
	 * @param c
	 * @return
	 */
	public Set<OWLClass> getConceptSuperTypes(OWLClass c){
		Set<OWLClass> pa = getAllSuperClasses(c);
		Set<OWLClass> lr = getOntologyRoots();
		pa.retainAll(lr);
		return pa;		
	}
	/**
	 * Method that calculates the relative depth of concept c with respect to concept localRoot
	 * @param c
	 * @param localRoot (maybe the least common subsumer or the domain root)
	 * @return
	 */
	public int computeDepth(OWLClass c, OWLClass localRoot){
		int d=0;
		HashSet<OWLClass> f = new HashSet<OWLClass>(); //frontier set
		f.add(c);
		while(!f.contains(localRoot) && !f.isEmpty()){
			HashSet<OWLClass> newF = new HashSet<OWLClass>(); //frontier set
			newF.addAll(f);
			for(OWLClass cf : newF){
				f.remove(cf);
				Set<OWLClassExpression> tmp = cf.getSuperClasses(onto);
				for(OWLClassExpression ce : tmp) f.add(ce.asOWLClass());
			}
			d++;
		}
		return d;
	}
	/**
	 * Method that retrieves a set containing all the superclasses (closure) of c
	 * @param c
	 * @return
	 */
	public Set<OWLClass> getAllSuperClasses(OWLClass c){
		Set<OWLClass> ret = new HashSet<OWLClass>();
		Set<OWLClassExpression> tmp = new HashSet<OWLClassExpression>();
		buildHierarchy(c, tmp);
		for(OWLClassExpression ce : tmp){
			ret.add(ce.asOWLClass());
		}
		return ret;
	}
	
	/**
	 * Auxiliary recursive method used by AllSuperClasses
	 * @param c
	 * @param visited
	 */
	private void buildHierarchy(OWLClass c, Set<OWLClassExpression> visited){
		Set<OWLClassExpression> sc = c.getSuperClasses(onto);
		for(OWLClassExpression ce : sc){
			if(!isGeneric(ce.asOWLClass()) && !visited.contains(ce)){
				visited.add(ce);
				buildHierarchy(ce.asOWLClass(), visited);
			}
		}
	}
	
	/**
	 * Method that tells whether a class represents everything or the top concept
	 * @param c
	 * @return
	 */
	public boolean isGeneric(OWLClass c){
		return c.equals(this.root);
		//return (c.isTopEntity() || c.getIRI().getFragment().endsWith("All")); //ad hoc for MeSH
	}
	/**
	 * Returns the classes corresponding to the top-domains in the Ontology (exactly under root or All for MeSH)
	 * @return the classes corresponding to the top-domains
	 */
	public Set<OWLClass> getOntologyRoots(){
		//OWLClass all = classForID("http://org.snu.bike/MeSH#All");
		HashSet<OWLClass> ret = new HashSet<OWLClass>();
		for(OWLClassExpression ce : this.root.getSubClasses(onto)){
			ret.add(ce.asOWLClass());
		}
		return ret;
	}
	
	/**
	 * Returns an OWLClass corresponding to the passed id
	 * @param id a String representing the class, e.g. "http://org.snu.bike/MeSH#All"
	 * @return The corresponding OWLClass in the Ontology, null if it is not in the ontology
	 */
	public OWLClass classForID(String id){
		Set<OWLEntity> s = onto.getEntitiesInSignature(IRI.create(id));
		if(!s.isEmpty()) return s.iterator().next().asOWLClass();
		else return null;
	}
	
	public OWLClass leastCommonSubsumer(OWLClass a, OWLClass b){
		Set<OWLClass> sup_a= getAllSuperClasses(a);
		sup_a.add(a);//add itself
		Set<OWLClass> sup_b= getAllSuperClasses(b);
		sup_b.add(b);
		
		HashMap<OWLClass, Integer> dist_a=dijkstra(a, sup_a);
		//HashMap<OWLClass, Integer> dist_b=dijkstra(a, sup_b);
		
		sup_a.retainAll(sup_b);
		Set<GraphElement> dist = new HashSet<GraphElement>();
		for(OWLClass c : sup_a){
			dist.add(new GraphElement(c, dist_a.get(c)));
		}
		GraphElement min = Collections.min(dist);
		
		return min.getOWLClass();
 	}
	
	private HashMap<OWLClass, Integer> dijkstra(OWLClass c, Set<OWLClass> graph){
		HashMap<OWLClass, Integer> dist = new HashMap<OWLClass, Integer>();
		for(OWLClass o : graph) dist.put(o, Integer.MAX_VALUE);
		dist.put(c, new Integer(0));
		
		Set<GraphElement> queue = new HashSet<GraphElement>();
		for(Entry<OWLClass, Integer> e : dist.entrySet()){
			GraphElement ge = new GraphElement(e.getKey(), e.getValue());
			queue.add(ge);
		}
		
		while(!queue.isEmpty()){
			GraphElement min=Collections.min(queue);
			queue.remove(min);
			if(min.weight==Integer.MAX_VALUE) break;
			OWLClass n = min.getOWLClass();
			Set<OWLClassExpression> ss = n.getSuperClasses(onto);
			for(OWLClassExpression sce : ss) {
				OWLClass neighbour=sce.asOWLClass();
				Integer alt = new Integer(1);
				alt+=min.getDistance();
				if(dist.containsKey(neighbour)) {
					if(alt < dist.get(neighbour)){
						GraphElement gn = new GraphElement(neighbour, alt); 
						queue.remove(gn);
						queue.add(gn);
						dist.put(neighbour, alt);
					}
				}
			}
		}
		
		return dist;
	}
	
	/**
	 * Returns the hashCode of ontology ID
	 */
	public int hashCode() {
		return this.getBaseAddr().hashCode();
	}
	
	/**
	 * Generates a trivial terminology
	 * @return
	 */
	public SKOSTerminology generateTerminology() {
		SKOSTerminology terminology = new SKOSTerminology();
		terminology.setStemming(true);
		Set<OWLClass> nodes = onto.getClassesInSignature();
		for(OWLClass c : nodes) {
			String concLabel = c.getIRI().getFragment();
			concLabel=concLabel.replace('_', ' ');
			concLabel=concLabel.replaceAll("\\p{Punct}", " ");
			concLabel=concLabel.replaceAll(" +", " ");
			concLabel=concLabel.toLowerCase();
			
			String sa[] = concLabel.split("(?<=[ \\n])");
			EnglishStemmer st = new EnglishStemmer();
			StringBuffer fbuf= new StringBuffer();
			for(String s : sa){
				st.setCurrent(s.trim());
				st.stem();
				fbuf.append(st.getCurrent());
				fbuf.append(" ");
			}
			
			String stemmed_label=fbuf.toString().trim();
			
			terminology.makeConceptLabel(c, stemmed_label);
		}
		
		return terminology;
	}
	
	/**
	 * Picker method to select the concept similarity method
	 * @param type
	 * @param c1
	 * @param c2
	 * @param localRoot
	 * @return
	 */
	public float computeSimilarity(int type, OWLClass c1, OWLClass c2, OWLClass localRoot){
		float res=0f;
		switch(type){
			case ConceptSimilarity.WU: res=computeWuPalmerSimilarity(c1, c2, localRoot); break;
			case ConceptSimilarity.PROXYGENEA1: res=computeProxiGenea(c1, c2, localRoot); break;
			case ConceptSimilarity.PROXYGENEA2: res=computeProxiGenea2(c1, c2, localRoot); break;
			case ConceptSimilarity.PROXYGENEA3: res=computeProxiGenea3(c1, c2, localRoot); break;
			default: res=computeProxiGenea(c1, c2, localRoot);
		}
		return res;
	}
	
	public float computeWuPalmerSimilarity(OWLClass c1, OWLClass c2, OWLClass localRoot)
	{

		Set<OWLClass> subsumers1 = this.getAllSuperClasses(c1);
		subsumers1.add(c1);
		Set<OWLClass> subsumers2 = this.getAllSuperClasses(c2);
		subsumers2.add(c2);
		subsumers1.retainAll(subsumers2);
		subsumers1.removeAll(this.getAllSuperClasses(localRoot));
		int greatestDepth = 1;
		for (OWLClass father:subsumers1)
		{
			int currentDepth = this.computeDepth(father,localRoot);
			if (currentDepth>greatestDepth)
				greatestDepth = currentDepth;
		}
		int d_c1=this.computeDepth(c1,localRoot);
		int d_c2=this.computeDepth(c2,localRoot);
		if(greatestDepth > Math.min(d_c1, d_c2)) greatestDepth=Math.min(d_c1, d_c2); //FIXME: patch to overcome some strange issues
		
		float result = new Float(2*greatestDepth)/new Float(d_c1+d_c2);
		return result;
	}
	
	public float computeProxiGenea(OWLClass c1, OWLClass c2, OWLClass localRoot)
	{
		Set<OWLClass> subsumers1 = this.getAllSuperClasses(c1);
		subsumers1.add(c1);
		Set<OWLClass> subsumers2 = this.getAllSuperClasses(c2);
		subsumers2.add(c2);
		subsumers1.retainAll(subsumers2);
		subsumers1.removeAll(this.getAllSuperClasses(localRoot));
		subsumers1.remove(localRoot);
		int greatestDepth = 1;
		for (OWLClass father:subsumers1)
		{
			int currentDepth = this.computeDepth(father,localRoot);
			if (currentDepth>greatestDepth)
				greatestDepth = currentDepth;
		}
		int d_c1=this.computeDepth(c1,localRoot);
		int d_c2=this.computeDepth(c2,localRoot);
		if(greatestDepth > Math.min(d_c1, d_c2)) greatestDepth=Math.min(d_c1, d_c2); //FIXME: patch to overcome some strange issues
		
		float result = new Float(1+greatestDepth*greatestDepth)/new Float(1+d_c1*d_c2); //added 1 to num and den to avoid issues with root concepts
		return result;
	}
	
	public float computeProxiGenea2(OWLClass c1, OWLClass c2, OWLClass localRoot)
	{
		Set<OWLClass> subsumers1 = this.getAllSuperClasses(c1);
		subsumers1.add(c1);
		Set<OWLClass> subsumers2 = this.getAllSuperClasses(c2);
		subsumers2.add(c2);
		subsumers1.retainAll(subsumers2);
		subsumers1.removeAll(this.getAllSuperClasses(localRoot));
		int greatestDepth = 1;
		for (OWLClass father:subsumers1)
		{
			int currentDepth = this.computeDepth(father,localRoot);
			if (currentDepth>greatestDepth)
				greatestDepth = currentDepth;
		}
		int d_c1=this.computeDepth(c1,localRoot);
		int d_c2=this.computeDepth(c2,localRoot);
		if(greatestDepth > Math.min(d_c1, d_c2)) greatestDepth=Math.min(d_c1, d_c2); //FIXME: patch to overcome some strange issues
		float result = new Float(greatestDepth)/new Float(d_c1+d_c2-greatestDepth);
		return result;
	}
	
	public float computeProxiGenea3(OWLClass c1, OWLClass c2, OWLClass localRoot)
	{
		Set<OWLClass> subsumers1 = this.getAllSuperClasses(c1);
		subsumers1.add(c1);
		Set<OWLClass> subsumers2 = this.getAllSuperClasses(c2);
		subsumers2.add(c2);
		subsumers1.retainAll(subsumers2);
		subsumers1.removeAll(this.getAllSuperClasses(localRoot));
		int greatestDepth = 1;
		for (OWLClass father:subsumers1)
		{
			int currentDepth = this.computeDepth(father,localRoot);
			if (currentDepth>greatestDepth)
				greatestDepth = currentDepth;
		}
		int d_c1=this.computeDepth(c1,localRoot);
		int d_c2=this.computeDepth(c2,localRoot);
		if(greatestDepth > Math.min(d_c1, d_c2)) greatestDepth=Math.min(d_c1, d_c2); //FIXME: patch to overcome some strange issues
		float result = 1/new Float(1+d_c1+d_c2-2*greatestDepth);
		return result;
	}
}

class GraphElement implements Comparable<GraphElement> {
	OWLClass cl;
	Integer weight;
	
	public GraphElement(OWLClass c, Integer w){
		this.cl=c;
		this.weight=w;
	}
	
	public OWLClass getOWLClass(){
		return this.cl;
	}
	
	public Integer getDistance(){
		return this.weight;
	}

	@Override
	public int compareTo(GraphElement o) {
		return this.weight.compareTo(o.weight);
	}
	
	public boolean equals(GraphElement o) {
		return this.cl.equals(o.cl);
	}
	
}
