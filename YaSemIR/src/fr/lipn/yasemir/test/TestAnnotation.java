package fr.lipn.yasemir.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Vector;

import fr.lipn.yasemir.Yasemir;
import fr.lipn.yasemir.ontology.annotation.Annotation;


public class TestAnnotation {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		//Yasemir.setIndexing(true);
		Yasemir.init("cuisine/cuisine.xml");
		
		
		String queryString = null;
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "iso-8859-1"));
		
		while (true) {
		      if (queryString == null) {                        // prompt the user
		        System.out.println("Enter query: ");
		      }

		      String line = queryString != null ? queryString : in.readLine();

		      if (line == null || line.length() == -1) {
		        break;
		      }

		      line = line.trim();
		      if (line.length() == 0) {
		        break;
		      }
		      
		      System.out.println("Annotating: " + line);
		      
		      HashMap<String, Vector<Annotation>> anns= null;
		      //System.err.println("[YaSemIr] Annotating: " + line);
		      anns = Yasemir.annotator.annotate(line);
		      
		      System.out.println("[YaSemIr] Annotations:");
		      for(String oid : anns.keySet()) {
		    	  System.out.println("oid: "+oid);
		    	  Vector<Annotation> ann = anns.get(oid);
		    	  for(Annotation a : ann){
			    	  System.out.println(a.getOWLClass().getIRI());
			      }
		      }
		     
		      System.out.println("---------------------------");
		}
	}

}
