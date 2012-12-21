package fr.lipn.yasemir.weighting.ckpd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;

public class NGramFactory {
	
	public static HashSet<NGram> getNGramSet(Vector<NGramTerm> sentence){
		HashSet<NGram> ngramSet = new HashSet<NGram>();
		int maxN=sentence.size();
		for(int i=0; i< maxN; i++){
			for(int j=0; j<sentence.size(); j++) {
				NGram ng = new NGram();
				for(int k=j; k<=(j+i) && k < sentence.size(); k++){
					ng.add(sentence.get(k));
				}
				ngramSet.add(ng);
			}
		}
		return ngramSet;
	}

	public static NGram getNGram(Vector<NGramTerm> tSentence) {
		NGram ng= new NGram();
		for(NGramTerm tw : tSentence){
			ng.add(tw);
		}
		return ng;
	}

}
