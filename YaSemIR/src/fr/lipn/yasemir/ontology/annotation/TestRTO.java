package fr.lipn.yasemir.ontology.annotation;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.manchester.cs.owl.owlapi.OWLDeclarationAxiomImpl;

public class TestRTO {

	/**
	 * @param args
	 * @throws OWLOntologyCreationException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException {
		// Get hold of an ontology manager
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        // Let's load an ontology from the web
        File file = new File("/home/dbuscaldi/Ubuntu One/Works/collabSIG/meshonto.owl");
        // Now load the local copy
        OWLOntology onto = manager.loadOntologyFromOntologyDocument(file);
        System.err.println("Loaded ontology: " + onto);
        // We can always obtain the location where an ontology was loaded from
        IRI documentIRI = manager.getOntologyDocumentIRI(onto);
        System.err.println("    from: " + documentIRI);
        /*for (OWLAxiom ax : onto.getAxioms()) {
        	System.err.println("---------------------------");
        	System.err.println(ax.toString());
        	System.err.println(ax.getAxiomType());
        	System.err.println(ax.getClassesInSignature());
        }*/
        for (OWLAxiom ax : onto.getAxioms()) {
        	if(ax.getAxiomType().equals(AxiomType.DECLARATION)){
        		System.err.println(ax.toString());
        		Set<OWLClass> axclasses = ax.getClassesInSignature();
        		OWLClass axcl = axclasses.iterator().next();
        		//printHierarchy(axcl, onto, 0, new HashSet<OWLClassExpression>());
        		Set<OWLClass> parents = getAllSuperClasses(axcl, onto);
        		
        		IRI clIRI =axcl.getIRI();
        		
        		System.err.println(clIRI.getFragment().replaceAll("[-_]", " "));
        		System.err.println(parents);
        		//System.err.println(clIRI);
        	}
        }
	}
	
	public static void printHierarchy(OWLClass c, OWLOntology o, int level, Set<OWLClassExpression> visited){
		if (isGeneric(c)) return;
		Set<OWLClassExpression> sc = c.getSuperClasses(o);
		String tab = "";
		for(int i=0; i< level; i++) tab+="-";
		for(OWLClassExpression ce : sc){
			if(!visited.contains(ce)){
				System.err.println(tab+ce.toString()+" -> ");
				visited.add(ce);
				printHierarchy(ce.asOWLClass(), o, level+1, visited);
			}
		}
	}
	
	public static Set<OWLClass> getAllSuperClasses(OWLClass c, OWLOntology o){
		Set<OWLClass> ret = new HashSet<OWLClass>();
		Set<OWLClassExpression> tmp = new HashSet<OWLClassExpression>();
		buildHierarchy(c, o, tmp);
		for(OWLClassExpression ce : tmp){
			ret.add(ce.asOWLClass());
		}
		return ret;
	}
	
	private static void buildHierarchy(OWLClass c, OWLOntology o, Set<OWLClassExpression> visited){
		if (isGeneric(c)) return;
		Set<OWLClassExpression> sc = c.getSuperClasses(o);
		for(OWLClassExpression ce : sc){
			if(!visited.contains(ce)){
				visited.add(ce);
				buildHierarchy(ce.asOWLClass(), o, visited);
			}
		}
	}
	
	public static boolean isGeneric(OWLClass c){
		return (c.isTopEntity() || c.getIRI().getFragment().endsWith("All")); //ad hoc for MeSH
	}
	
	public static Set<OWLClass> getLocalRoots(OWLOntology o){
		/* 
		 * Get a class from an IRI
		 * OWLClass corresponding = o.getEntitiesInSignature(clIRI).iterator().next().asOWLClass();
		 */
		OWLClass all = o.getEntitiesInSignature(IRI.create("http://org.snu.bike/MeSH#All")).iterator().next().asOWLClass();
		HashSet<OWLClass> ret = new HashSet<OWLClass>();
		for(OWLClassExpression ce : all.getSubClasses(o)){
			ret.add(ce.asOWLClass());
		}
		return ret;
	}
}

