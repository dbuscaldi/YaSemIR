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

import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tartarus.snowball.ext.EnglishStemmer;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntTools;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import fr.lipn.yasemir.ontology.skos.SKOSTerminology;

/**
 * Wrapper class for the OWLOntology, with methods needed to compute semantic relatedness
 * @author buscaldi
 *
 */
public class Ontology {
	private OntModel onto;
	private OntClass root;
	private String mainNameSpace;
	
	/**
	 * Constructor that uses owl:Thing as root concept
	 * @param ontologyFileLocation
	 */
	public Ontology(String ontologyFileLocation){
		onto = ModelFactory.createOntologyModel();
		InputStream in = FileManager.get().open( ontologyFileLocation );
		if (in == null) {
			System.err.println("[YaSemIR]: Impossible to load ontology from file: "+ontologyFileLocation);
			System.exit(-1);
		}
		
		onto.read(in, null);
		
		/*
		String docURI= "http://yasemir.org/ontology";
		onto = ModelFactory.createOntologyModel();
		OntDocumentManager dm = onto.getDocumentManager();
		dm.addAltEntry(docURI, ontologyFileLocation);
		onto.read( docURI );
		*/
		
		ExtendedIterator<OntClass> itr = onto.listHierarchyRootClasses();
	 	while(itr.hasNext()){
	 		OntClass c = itr.next();
	 		if(c.isHierarchyRoot()) {
	 			this.root=c;
	 			break;
	 		}
	 	}
        
        if(this.root==null) System.err.println("[YaSemIR]: ERROR: No root class found!!!");
        else System.err.println("[YaSemIR]: Warning: no root class given, using "+this.root);
        
        System.err.println("[YaSemIR]: Loaded ontology: " + onto+ " with root class "+this.root);
	}
	/**
	 * This method returns a unique string identifier associated with the Ontology.
	 * This identifier is used during the indexing process to specify the source of the annotation
	 * @return
	 */
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
		onto = ModelFactory.createOntologyModel();
		InputStream in = FileManager.get().open( ontologyFileLocation );
		if (in == null) {
			System.err.println("[YaSemIR]: Impossible to load ontology from file: "+ontologyFileLocation);
			System.exit(-1);
		}
		
		String rootNS = null;
		int gatePos = root.lastIndexOf('#');
		if(gatePos > -1) {
			rootNS=root.substring(0, gatePos);
		} else {
			gatePos=root.lastIndexOf('/');
			rootNS=root.substring(0, gatePos);
		}
		
		onto.read(in, rootNS);
		
		this.mainNameSpace=rootNS;
		
		/*
		 *     Note to the above method:
		 *     in - the input stream
		 *     base - the base uri to be used when converting relative URI's to absolute URI's. (Resolving relative URIs and fragment IDs is done by prepending the base URI to the relative URI/fragment.) If there are no relative URIs in the source, this argument may safely be null. If the base is the empty string, then relative URIs will be retained in the model. This is typically unwise and will usually generate errors when writing the model back out.
		 */
		
		this.root = classForID(root);
		
		System.err.println("[YaSemIR]: Loaded ontology: " + rootNS + " with root class "+root);
	}
	/**
	 * Returns the main namespace of the ontology
	 * @return
	 */
	public String getBaseAddr(){
		return mainNameSpace;
		//return onto.getNsPrefixURI(onto.toString())+"#";
		//return onto.getOntologyID().getOntologyIRI().toString()+"#";
		//return "http://org.snu.bike/MeSH#";
	}
	/**
	 * Two classes are comparable if they share at least a part of their concept path
	 * @param a
	 * @param b
	 * @return
	 */
	public Set<OntClass> comparableRoots(OntClass a, OntClass b){
		Set<OntClass> pa = getConceptSuperTypes(a);
		Set<OntClass> pb = getConceptSuperTypes(b);
		pa.retainAll(pb);
		return pa; //if this is not empty, the classes are comparable; if its size is more than 1, then we have multiple inheritance		
	}
	
	/**
	 * Method that returns the top concepts for the given OWL Class
	 * @param c
	 * @return
	 */
	public Set<OntClass> getConceptSuperTypes(OntClass c){
		Set<OntClass> pa = getAllSuperClasses(c);
		Set<OntClass> lr = getOntologyRoots();
		pa.retainAll(lr);
		return pa;		
	}
	/**
	 * Method that calculates the relative depth of concept c with respect to concept localRoot
	 * @param c
	 * @param localRoot (maybe the least common subsumer or the domain root)
	 * @return
	 */
	public int computeDepth(OntClass c, OntClass localRoot){
		int d=0;
		HashSet<OntClass> f = new HashSet<OntClass>(); //frontier set
		f.add(c);
		while(!f.contains(localRoot) && !f.isEmpty()){
			HashSet<OntClass> newF = new HashSet<OntClass>(); //frontier set
			newF.addAll(f);
			for(OntClass cf : newF){
				f.remove(cf);
				ExtendedIterator<OntClass> itr = cf.listSuperClasses(true);
				Set<OntClass> tmp = new HashSet<OntClass>();
				while(itr.hasNext()){
					tmp.add(itr.next());
				}
				for(OntClass ce : tmp) f.add(ce);
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
	public Set<OntClass> getAllSuperClasses(OntClass c){
		Set<OntClass> ret = new HashSet<OntClass>();
		ExtendedIterator<OntClass> itr = c.listSuperClasses();
		while(itr.hasNext()) {
			ret.add(itr.next());
		}
		return ret;
	}
	
	/**
	 * Method that tells whether a class represents the top concept
	 * @param c
	 * @return
	 */
	public boolean isGeneric(OntClass c){
		return c.equals(this.root);
	}
	/**
	 * Returns the classes corresponding to the top-domains in the Ontology (exactly under root or All for MeSH)
	 * @return the classes corresponding to the top-domains
	 */
	public Set<OntClass> getOntologyRoots(){
		//OntClass all = classForID("http://org.snu.bike/MeSH#All");
		HashSet<OntClass> ret = new HashSet<OntClass>();
		ExtendedIterator<OntClass> itr = this.root.listSubClasses(true);
		while(itr.hasNext()) {
			ret.add(itr.next());
		}
		return ret;
	}
	
	/**
	 * Returns an OntClass corresponding to the passed id string (IRI format)
	 * @param id a String representing the class, e.g. "http://org.snu.bike/MeSH#All"
	 * @return The corresponding OntClass in the Ontology, null if it is not in the ontology
	 */
	public OntClass classForID(String id){
		return this.onto.getOntClass(id);
		/*Set<OWLEntity> s = onto.getEntitiesInSignature(IRI.create(id));
		if(s.isEmpty()) s= onto.getEntitiesInSignature(IRI.create(this.getBaseAddr()+id));
		if(!s.isEmpty()) return s.iterator().next().asOntClass();
		else return null;
		*/
	}
	
	/**
	 * Returns the least common subsumer between two concepts a and b
	 * @param a
	 * @param b
	 * @return
	 */
	public OntClass leastCommonSubsumer(OntClass a, OntClass b){
		return OntTools.getLCA(onto, a, b);
 	}
	
	/**
	 * Returns the hashCode of ontology ID
	 */
	public int hashCode() {
		return this.getBaseAddr().hashCode();
	}
	
	/**
	 * Generates a trivial terminology composed by the concept labels.
	 * It converts underscores and punctuation symbols to spaces.
	 * It also attempts to split concept names on the basis of case variations:
	 * for instance, "Segment4OfRCA" is split as: "Segment 4 of RCA"
	 * @return
	 */
	public SKOSTerminology generateTerminology() {
		SKOSTerminology terminology = new SKOSTerminology(this.getOntologyID());
		terminology.setStemming(true);
		ExtendedIterator<OntClass> itr = onto.listClasses();
		while(itr.hasNext()) {
			OntClass c = itr.next();
			if (c.getURI()==null) continue; //TODO: verificare perchè ci sono classi con URI null
			String concLabel = c.getLocalName();
			if(!concLabel.equals(concLabel.toUpperCase()) && !concLabel.equals(concLabel.toLowerCase())){ //concept name is not all in capitals or minuscule
				Pattern p = Pattern.compile("([0-9]+|\\p{Lu}[^\\p{Lu}0-9]+|\\p{Lu}+)");
				Matcher m = p.matcher(concLabel);
				
				StringBuffer clbuf = new StringBuffer();
				while(m.find()){
					String str = m.group();
					clbuf.append(str);
					clbuf.append(" ");
				}

				concLabel=clbuf.toString().trim();
			}
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
			//System.err.println("Full label: "+c.getIRI()+" stemmed label: "+stemmed_label);
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
	public float computeSimilarity(int type, OntClass c1, OntClass c2, OntClass localRoot){
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
	
	/**
	 * Method that computes Wu-Palmer conceptual similarity
	 * @param c1
	 * @param c2
	 * @param localRoot
	 * @return
	 */
	public float computeWuPalmerSimilarity(OntClass c1, OntClass c2, OntClass localRoot)
	{

		Set<OntClass> subsumers1 = this.getAllSuperClasses(c1);
		subsumers1.add(c1);
		Set<OntClass> subsumers2 = this.getAllSuperClasses(c2);
		subsumers2.add(c2);
		subsumers1.retainAll(subsumers2);
		subsumers1.removeAll(this.getAllSuperClasses(localRoot));
		int greatestDepth = 1;
		for (OntClass father:subsumers1)
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
	/**
	 * Method that computes the ProxiGenea1 conceptual similarity [Ralalason 2010]
	 * @param c1
	 * @param c2
	 * @param localRoot
	 * @return
	 */
	public float computeProxiGenea(OntClass c1, OntClass c2, OntClass localRoot)
	{
		Set<OntClass> subsumers1 = this.getAllSuperClasses(c1);
		subsumers1.add(c1);
		Set<OntClass> subsumers2 = this.getAllSuperClasses(c2);
		subsumers2.add(c2);
		subsumers1.retainAll(subsumers2);
		subsumers1.removeAll(this.getAllSuperClasses(localRoot));
		subsumers1.remove(localRoot);
		int greatestDepth = 1;
		for (OntClass father:subsumers1)
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
	
	/**
	 * Method that computes the ProxiGenea1 conceptual similarity [Ralalason 2010]
	 * @param c1
	 * @param c2
	 * @param localRoot
	 * @return
	 */
	public float computeProxiGenea2(OntClass c1, OntClass c2, OntClass localRoot)
	{
		Set<OntClass> subsumers1 = this.getAllSuperClasses(c1);
		subsumers1.add(c1);
		Set<OntClass> subsumers2 = this.getAllSuperClasses(c2);
		subsumers2.add(c2);
		subsumers1.retainAll(subsumers2);
		subsumers1.removeAll(this.getAllSuperClasses(localRoot));
		int greatestDepth = 1;
		for (OntClass father:subsumers1)
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
	
	/**
	 * Method that computes the ProxiGenea1 conceptual similarity [Ralalason 2010]
	 * @param c1
	 * @param c2
	 * @param localRoot
	 * @return
	 */
	public float computeProxiGenea3(OntClass c1, OntClass c2, OntClass localRoot)
	{
		Set<OntClass> subsumers1 = this.getAllSuperClasses(c1);
		subsumers1.add(c1);
		Set<OntClass> subsumers2 = this.getAllSuperClasses(c2);
		subsumers2.add(c2);
		subsumers1.retainAll(subsumers2);
		subsumers1.removeAll(this.getAllSuperClasses(localRoot));
		int greatestDepth = 1;
		for (OntClass father:subsumers1)
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
	OntClass cl;
	Integer weight;
	
	public GraphElement(OntClass c, Integer w){
		this.cl=c;
		this.weight=w;
	}
	
	public OntClass getOntClass(){
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
