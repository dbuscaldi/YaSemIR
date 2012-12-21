package fr.lipn.yasemir.weighting.ckpd;

import java.util.Vector;
import java.util.HashSet;


public class NGramComparer {

	public static double compare(Vector<NGramTerm> tSentence, Vector<NGramTerm> tSentence1){
		double simValue=0.0;
		HashSet<NGram> set0 = NGramFactory.getNGramSet(tSentence);
		HashSet<NGram> set1 = NGramFactory.getNGramSet(tSentence1);
		
		NGram ngram0 = NGramFactory.getNGram(tSentence);
		NGram ngram1 = NGramFactory.getNGram(tSentence);
		NGram longestSent;
		if(ngram0.getSize() > ngram1.getSize()) longestSent=ngram0;
		else longestSent=ngram1;
		
		HashSet<NGram> intSet= new HashSet<NGram>(set0);
	    intSet.retainAll(set1);
	    HashSet<NGram> coveringSet = new HashSet<NGram>();
	    for(NGram n : intSet) {
	    	HashSet<NGram> rest = new HashSet<NGram>(intSet);
	    	rest.remove(n);
	    	boolean flag=true;
	    	for(NGram o : rest){
	    		if(n.containedIn(o)) {
	    			flag=false; break;
	    		}
	    	}
	    	if(flag) coveringSet.add(n);
	    }
	    /*
	    System.err.println("covering set:");
	    for(NGram ng : coveringSet){
    		System.err.println(ng.repr());
    	}
	    System.err.println("-------------------------------");
	    */
	    NGram longestNG=new NGram();
	    for(NGram ng : coveringSet){
	    	if(ng.getSize()> longestNG.getSize()) longestNG=ng;
	    }
	    
	    longestSent.setWeights();
	    //prepare weights
	    for(NGram ng : coveringSet){
	    	ng.calculateDistance(longestNG, longestSent);
	    	ng.setWeights();
	    }
	    
	    double ngWSum=0.0;
	    for(NGram ng : coveringSet){
	    	double ngw=ng.getWeight()/ng.getDistanceCoeff();
	    	//System.err.println(ngw+" weight for: "+ng.repr());
	    	ngWSum+=ngw;
	    }
	    //System.err.println("longestSent weight"+longestSent.getWeight());
	    simValue=ngWSum/longestSent.getWeight();
	    
	    return simValue;
	}
}
