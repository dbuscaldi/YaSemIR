package fr.lipn.yasemir.ontology.annotation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

import fr.lipn.yasemir.ontology.Ontology;

public class TestAnnotation {
	private static String ontologyLocation = "/users/buscaldi/Works/collabSIG/meshonto.owl";
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Ontology.init(ontologyLocation);
		
		IndexBasedAnnotator sa = new IndexBasedAnnotator("termIndex_trivial"); 
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
		      Vector<Annotation> ann = new Vector<Annotation>();
		      ann.addAll(sa.annotate(line));
		      
		      System.out.println("Annotation:");
		      for(Annotation a : ann){
		    	  System.out.println(a.getOWLClass().getIRI().getFragment());
		      }
		      System.out.println("---------------------------");
		}
	}

}
