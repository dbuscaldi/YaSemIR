package fr.lipn.yasemir.ontology.annotation;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.semanticweb.owlapi.model.OWLClass;
import org.xml.sax.SAXException;

import fr.lipn.yasemir.ontology.Ontology;
import fr.lipn.yasemir.search.OHSUQuery;
import fr.lipn.yasemir.search.SemanticallyRankedDocument;
import fr.lipn.yasemir.search.XMLQueryHandler;

public class AnnotationEvaluator {
	private static String ontologyLocation = "/users/buscaldi/Works/collabSIG/meshonto.owl";
	private static boolean TITLE_ONLY=false;
	private static boolean ACCEPT_PARENTS=true; //we accept as correct an annotation with a subclass of a reference tag
	
	private static HashSet<String> get_parents(String cat){
		//if I annote with a category, I assume also its parents
		OWLClass c = Ontology.classForID(cat);
		HashSet<OWLClass> parents = (HashSet<OWLClass>) Ontology.getAllSuperClasses(c);
		HashSet<String> pSet = new HashSet<String>();
		for(OWLClass p : parents) {
			pSet.add(p.getIRI().toString());
		}
		return pSet;
	}
	/**
	 * @param args
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
		Ontology.init(ontologyLocation);
		
		HashMap<String, Integer> macroMapTP = new HashMap<String, Integer>(); //true positives by class (class -> #TP)
		HashMap<String, Integer> macroMapFP = new HashMap<String, Integer>(); //false positives by class (class -> #FP)
		HashMap<String, Integer> macroMapFN = new HashMap<String, Integer>(); //false negatives by class (class -> #FN)
		
		int sumTP=0, sumFP=0, sumFN=0; //document (micro) precision
		
		//File queryFile = new File("/users/buscaldi/Works/collabSIG/queries-updated.xml"); //TEST SU QUERIES OHSUMED
		//XMLQueryHandler hdlr = new XMLQueryHandler(queryFile);
		
		//File queryFile = new File("/users/buscaldi/Works/collabSIG/terminology/testQueriesMTI.xml"); //TEST SU DATI  OHSUMED
		File queryFile = new File("/users/buscaldi/Works/collabSIG/terminology/testset_v1.xml"); //TEST SU DATI  TRIESCHNIGG
		XMLMTITestHandler hdlr = new XMLMTITestHandler(queryFile);
		Vector<OHSUQuery> queries = hdlr.getParsedQueries();
		
		IndexBasedAnnotator sa = new IndexBasedAnnotator("termIndex_0.5"); 
		
		for(OHSUQuery oq : queries){
		      String line = oq.getTitle();
		      if(!TITLE_ONLY) line=line +" "+oq.getDescription();
		      
		      HashSet<String> refCats = new HashSet<String>(oq.getCategoryList()); //R set
		      
		      for(String rc:refCats) System.err.println(rc);
		      
		      line = line.trim();
		      
		      System.out.println("Annotating: " + line);
		      Vector<Annotation> ann = new Vector<Annotation>();
		      ann.addAll(sa.annotate(line));
		      
		      System.out.println("Annotation:");
		      HashSet<String> annSet = new HashSet<String>(); //A set
		      HashSet<String> parentSet = new HashSet<String>(); //A set
		      for(Annotation a : ann){
		    	  String a_repr = a.getOWLClass().getIRI().toString();
		    	  annSet.add(a_repr);
		    	  if(ACCEPT_PARENTS){
		    		  parentSet.addAll(get_parents(a_repr));
		    	  }
		    	  System.out.println(a_repr);
		      }
		      System.out.println("---------------------------");
		      
		      if(ACCEPT_PARENTS){
		    	  parentSet.retainAll(refCats);
		    	  annSet.addAll(parentSet);
		      }
		      /********************
		       * EVALUATION PART
		       ********************/
		      //true positives: A intersect R set
		      HashSet<String> tpSet = new HashSet<String>(annSet); // use the copy constructor
		      tpSet.retainAll(refCats);
		      sumTP+=tpSet.size();
		      for(String c : tpSet) {
		    	  Integer freq = macroMapTP.get(c);
		          macroMapTP.put(c, (freq == null ? 1 : freq + 1));
		      }
		      
		      //false positives: A-R set
		      HashSet<String> fpSet = new HashSet<String>(annSet); // use the copy constructor
		      fpSet.removeAll(refCats);
		      sumFP+=fpSet.size();
		      for(String c : fpSet) {
		    	  Integer freq = macroMapFP.get(c);
		          macroMapFP.put(c, (freq == null ? 1 : freq + 1));
		      }
		      
		      //false negatives: R-A set
		      HashSet<String> fnSet = new HashSet<String>(refCats); // use the copy constructor
		      fnSet.removeAll(annSet);
		      sumFN+=fnSet.size();
		      for(String c : fnSet) {
		    	  Integer freq = macroMapFN.get(c);
		          macroMapFN.put(c, (freq == null ? 1 : freq + 1));
		      }
		      
		}
		
		
	      
	      // now print the macro- and micro- averaged precision and recall
	      
	      //macro:
	      HashSet<String> allKeys = new HashSet<String>();
	      allKeys.addAll(macroMapTP.keySet());
	      allKeys.addAll(macroMapFP.keySet());
	      allKeys.addAll(macroMapFN.keySet());
	      
	      int C=allKeys.size();
	      double sumPC=0d;
	      double sumRC=0d;
	      
	      Vector<Element> precByCls = new Vector<Element>();
	      Vector<Element> recByCls = new Vector<Element>();
	      
	      for(String k : allKeys){
	    	  Integer tp=macroMapTP.get(k);
	    	  if(tp==null) tp=0;
	    	  Integer fp=macroMapFP.get(k);
	    	  if(fp==null) fp=0;
	    	  Integer fn=macroMapFN.get(k);
	    	  if(fn==null) fn=0;
	    	  
	    	  if(tp.intValue()!=0){
	    		double prec = tp.doubleValue()/(tp.doubleValue()+fp.doubleValue());
	    		double rec = tp.doubleValue()/(tp.doubleValue()+fn.doubleValue());
	    		
	    		precByCls.add(new Element(k, prec));
	    		recByCls.add(new Element(k, rec));
	    		
	    		sumPC+=prec;
	    		sumRC+=rec;
	    	  } 
	      }
	      
	      double pMacro= sumPC/(double)C;
	      double rMacro= sumRC/(double)C;
	      
	      double pMicro = (double)sumTP/((double)(sumTP+sumFP));
	      double rMicro = (double)sumTP/((double)(sumTP+sumFN));
	      
	      Collections.sort(precByCls);
	      Collections.sort(recByCls);
	      
	      System.out.println("---------Best precision:");
	      for(int i=0; i < 5; i++){
	    	  Element e = precByCls.get(i);
	    	  System.out.println(e.key+"\tprec:\t"+e.value);
	      }
	      System.out.println("---------Best recall:");
	      for(int i=0; i < 5; i++){
	    	  Element e = recByCls.get(i);
	    	  System.out.println(e.key+"\trecall:\t"+e.value);
	      }
	      
	      Collections.reverse(precByCls);
	      Collections.reverse(recByCls);
	      System.out.println("---------Worst precision:");
	      for(int i=0; i < 5; i++){
	    	  Element e = precByCls.get(i);
	    	  System.out.println(e.key+"\tprec:\t"+e.value);
	      }
	      System.out.println("---------Worst recall:");
	      for(int i=0; i < 5; i++){
	    	  Element e = recByCls.get(i);
	    	  System.out.println(e.key+"\trecall:\t"+e.value);
	      }
	      
	      System.out.println("---------Overall measures:");
	      System.out.println("P_micro: "+pMicro+"\nR_micro: "+rMicro);
	      System.out.println("F_micro: "+(2*pMicro*rMicro)/(pMicro+rMicro));
	      System.out.println("P_macro: "+pMacro+"\nR_macro: "+rMacro);
	      System.out.println("F_macro: "+(2*pMacro*rMacro)/(pMacro+rMacro));

	}

}

class Element implements Comparable<Element>  {
	String key;
	Double value;
	
	public Element(String key, double val){
		this.key=key;
		this.value=new Double(val);
	}
	@Override
	public int compareTo(Element arg0) {
		return arg0.value.compareTo(this.value);
	}
}
