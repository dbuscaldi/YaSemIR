package fr.lipn.yasemir.search;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

import org.semanticweb.owlapi.model.OWLClass;

public class ClassWeightHandler {
	private static HashMap<String, Integer> freqMap;
	private static int N;
	
	public static void init(){
		freqMap=new HashMap<String, Integer>();
		//read data from freqs.dat
        File file = new File("freqs.dat");
        try {
            Scanner scanner = new Scanner(file);
            boolean first=true;
            while (scanner.hasNextLine()) {
                String line = (scanner.nextLine()).trim();
                if(first){
                	N=Integer.parseInt(line);
                	first = false;
                } else {
                	String [] els = line.split("\t");
                	freqMap.put(els[0], new Integer(els[1]));
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
	}
	
	public static double getProb(OWLClass cls){
		if (cls==null) return 1d;
		String rep=cls.toStringID();
		if(freqMap.containsKey(rep)){
			double v=freqMap.get(rep).doubleValue();
			return 1+v/(double)N;
		} else return 1d;
	}
	
	public static double getGaussProb(OWLClass cls){
		double f=6d;
		if (cls==null) return 1d;
		String rep=cls.toStringID();
		if(freqMap.containsKey(rep)){
			double v=freqMap.get(rep).doubleValue();
			return f*Math.pow((v/N), 0.5);
		} else return 1d;
	}
	
	public static double getIDF(OWLClass cls){
		if (cls==null) return 1d;
		String rep=cls.toStringID();
		if(freqMap.containsKey(rep)){
			double v=freqMap.get(rep).doubleValue();
			return Math.log((double)N/v);
		} else return 1d;
	}
	
	public static double get1(OWLClass cls){
		return 1d; //method that corresponds to giving all classes the same weight
	}
}
