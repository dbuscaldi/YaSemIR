package fr.lipn.yasemir.ontology;

import java.util.Set;
import org.semanticweb.owlapi.model.OWLClass;

public class ConceptSimilarity {
	public final static int WU=0;
	public final static int PROXYGENEA1=1;
	public final static int PROXYGENEA2=2;
	public final static int PROXYGENEA3=3;
	
	/**
	 * Picker method to select the concept similarity method
	 * @param type
	 * @param c1
	 * @param c2
	 * @param localRoot
	 * @return
	 */
	public static float computeSimilarity(int type, OWLClass c1, OWLClass c2, OWLClass localRoot){
		float res=0f;
		switch(type){
			case WU: res=computeWuPalmerSimilarity(c1, c2, localRoot); break;
			case PROXYGENEA1: res=computeProxiGenea(c1, c2, localRoot); break;
			case PROXYGENEA2: res=computeProxiGenea2(c1, c2, localRoot); break;
			case PROXYGENEA3: res=computeProxiGenea3(c1, c2, localRoot); break;
			default: res=computeProxiGenea(c1, c2, localRoot);
		}
		return res;
	}
	
	public static float computeWuPalmerSimilarity(OWLClass c1, OWLClass c2, OWLClass localRoot)
	{

		Set<OWLClass> subsumers1 = Ontology.getAllSuperClasses(c1);
		subsumers1.add(c1);
		Set<OWLClass> subsumers2 = Ontology.getAllSuperClasses(c2);
		subsumers2.add(c2);
		subsumers1.retainAll(subsumers2);
		subsumers1.removeAll(Ontology.getAllSuperClasses(localRoot));
		int greatestDepth = 1;
		for (OWLClass father:subsumers1)
		{
			int currentDepth = Ontology.computeDepth(father,localRoot);
			if (currentDepth>greatestDepth)
				greatestDepth = currentDepth;
		}
		int d_c1=Ontology.computeDepth(c1,localRoot);
		int d_c2=Ontology.computeDepth(c2,localRoot);
		if(greatestDepth > Math.min(d_c1, d_c2)) greatestDepth=Math.min(d_c1, d_c2); //FIXME: patch to overcome some strange issues
		
		float result = new Float(2*greatestDepth)/new Float(d_c1+d_c2);
		return result;
	}
	
	public static float computeProxiGenea(OWLClass c1, OWLClass c2, OWLClass localRoot)
	{
		Set<OWLClass> subsumers1 = Ontology.getAllSuperClasses(c1);
		subsumers1.add(c1);
		Set<OWLClass> subsumers2 = Ontology.getAllSuperClasses(c2);
		subsumers2.add(c2);
		subsumers1.retainAll(subsumers2);
		subsumers1.removeAll(Ontology.getAllSuperClasses(localRoot));
		subsumers1.remove(localRoot);
		int greatestDepth = 1;
		for (OWLClass father:subsumers1)
		{
			int currentDepth = Ontology.computeDepth(father,localRoot);
			if (currentDepth>greatestDepth)
				greatestDepth = currentDepth;
		}
		int d_c1=Ontology.computeDepth(c1,localRoot);
		int d_c2=Ontology.computeDepth(c2,localRoot);
		if(greatestDepth > Math.min(d_c1, d_c2)) greatestDepth=Math.min(d_c1, d_c2); //FIXME: patch to overcome some strange issues
		
		float result = new Float(1+greatestDepth*greatestDepth)/new Float(1+d_c1*d_c2); //added 1 to num and den to avoid issues with root concepts
		return result;
	}
	
	public static float computeProxiGenea2(OWLClass c1, OWLClass c2, OWLClass localRoot)
	{
		Set<OWLClass> subsumers1 = Ontology.getAllSuperClasses(c1);
		subsumers1.add(c1);
		Set<OWLClass> subsumers2 = Ontology.getAllSuperClasses(c2);
		subsumers2.add(c2);
		subsumers1.retainAll(subsumers2);
		subsumers1.removeAll(Ontology.getAllSuperClasses(localRoot));
		int greatestDepth = 1;
		for (OWLClass father:subsumers1)
		{
			int currentDepth = Ontology.computeDepth(father,localRoot);
			if (currentDepth>greatestDepth)
				greatestDepth = currentDepth;
		}
		int d_c1=Ontology.computeDepth(c1,localRoot);
		int d_c2=Ontology.computeDepth(c2,localRoot);
		if(greatestDepth > Math.min(d_c1, d_c2)) greatestDepth=Math.min(d_c1, d_c2); //FIXME: patch to overcome some strange issues
		float result = new Float(greatestDepth)/new Float(d_c1+d_c2-greatestDepth);
		return result;
	}
	
	public static float computeProxiGenea3(OWLClass c1, OWLClass c2, OWLClass localRoot)
	{
		Set<OWLClass> subsumers1 = Ontology.getAllSuperClasses(c1);
		subsumers1.add(c1);
		Set<OWLClass> subsumers2 = Ontology.getAllSuperClasses(c2);
		subsumers2.add(c2);
		subsumers1.retainAll(subsumers2);
		subsumers1.removeAll(Ontology.getAllSuperClasses(localRoot));
		int greatestDepth = 1;
		for (OWLClass father:subsumers1)
		{
			int currentDepth = Ontology.computeDepth(father,localRoot);
			if (currentDepth>greatestDepth)
				greatestDepth = currentDepth;
		}
		int d_c1=Ontology.computeDepth(c1,localRoot);
		int d_c2=Ontology.computeDepth(c2,localRoot);
		if(greatestDepth > Math.min(d_c1, d_c2)) greatestDepth=Math.min(d_c1, d_c2); //FIXME: patch to overcome some strange issues
		float result = 1/new Float(1+d_c1+d_c2-2*greatestDepth);
		return result;
	}
}
