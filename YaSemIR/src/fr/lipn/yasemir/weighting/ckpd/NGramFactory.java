package fr.lipn.yasemir.weighting.ckpd;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;
/**
 * Class used to produce the n-grams used by the CKPD method
 * @author buscaldi
 *
 */
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
