package fr.lipn.yasemir.indexing.ohsumed;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;

public class ClassFrequencyCollector {
	static HashMap<String, Integer> classFrequencies;
	static int N;
	
	public static void init(){
		classFrequencies =  new HashMap<String, Integer>();
		N=0;
	}
	
	private static void add(String cls){
		int n=1;
		if(classFrequencies.containsKey(cls)) {
			n+=classFrequencies.get(cls).intValue();
		}
		classFrequencies.put(cls, new Integer(n));
		++N;
	}
	
	public static void add(Set<OWLClass> roots){
		for(OWLClass c : roots) {
			add(c.toStringID());
		}
	}
	
	public static void dump(String filename){
		try {
			Writer out = new OutputStreamWriter(new FileOutputStream(filename), "UTF-8");
	    	out.write(N);
	    	out.write("\n");
	    	for(String k : classFrequencies.keySet()){
	    		out.write(k+"\t"+classFrequencies.get(k));
	    		out.write("\n");
	    	}
	    	out.flush();
	    	out.close();
	    } catch (IOException e) {
			e.printStackTrace();
		}
	}
}
