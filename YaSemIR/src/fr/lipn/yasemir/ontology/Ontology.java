package fr.lipn.yasemir.ontology;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class Ontology {
	private static OWLOntology onto;
	
	public static void init(String ontologyFileLocation){
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        File file = new File(ontologyFileLocation);
        // Now load the local copy
        try {
			onto = manager.loadOntologyFromOntologyDocument(file);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			onto = null;
		}
        System.err.println("Loaded ontology: " + onto);
	}
	
	public static String getBaseAddr(){
		return "http://org.snu.bike/MeSH#"; //FIXME: parametrizzare
	}
	/**
	 * Two classes are comparable if they share at least a part of their concept path
	 * @param a
	 * @param b
	 * @return
	 */
	public static Set<OWLClass> comparableRoots(OWLClass a, OWLClass b){
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
	public static Set<OWLClass> getConceptSuperTypes(OWLClass c){
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
	public static int computeDepth(OWLClass c, OWLClass localRoot){
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
	public static Set<OWLClass> getAllSuperClasses(OWLClass c){
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
	private static void buildHierarchy(OWLClass c, Set<OWLClassExpression> visited){
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
	public static boolean isGeneric(OWLClass c){
		return (c.isTopEntity() || c.getIRI().getFragment().endsWith("All")); //ad hoc for MeSH
	}
	/**
	 * Returns the classes corresponding to the top-domains in the Ontology (exactly under root or All for MeSH)
	 * @return the classes corresponding to the top-domains
	 */
	public static Set<OWLClass> getOntologyRoots(){
		/* 
		 * Get a class from an IRI
		 * OWLClass corresponding = o.getEntitiesInSignature(clIRI).iterator().next().asOWLClass();
		 */
		OWLClass all = classForID("http://org.snu.bike/MeSH#All");
		HashSet<OWLClass> ret = new HashSet<OWLClass>();
		for(OWLClassExpression ce : all.getSubClasses(onto)){
			ret.add(ce.asOWLClass());
		}
		return ret;
	}
	
	/**
	 * Returns an OWLClass corresponding to the passed id
	 * @param id a String representing the class, e.g. "http://org.snu.bike/MeSH#All"
	 * @return The corresponding OWLClass in the Ontology, null if it is not in the ontology
	 */
	public static OWLClass classForID(String id){
		Set<OWLEntity> s = onto.getEntitiesInSignature(IRI.create(id));
		if(!s.isEmpty()) return s.iterator().next().asOWLClass();
		else return null;
	}
	
	public static OWLClass leastCommonSubsumer(OWLClass a, OWLClass b){
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
	
	private static HashMap<OWLClass, Integer> dijkstra(OWLClass c, Set<OWLClass> graph){
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
