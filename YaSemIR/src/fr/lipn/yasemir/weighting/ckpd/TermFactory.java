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
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.IndexReader;
public class TermFactory {
	public static IndexReader reader; //we use this to calculate term frequencies
	public static Analyzer analyzer;
	
	public static void init(IndexReader s, Analyzer a) {
		reader=s;
		analyzer=a;
	}
	
	public static Vector<NGramTerm> makeTermSequence(String text){
		List<String> result = new ArrayList<String>();
		try {
			TokenStream stream  = analyzer.tokenStream("text", new StringReader(text));

        
            while(stream.incrementToken()) {
                //result.add(stream.getAttribute(TermAttribute.class).term()); //old way
                result.add(stream.getAttribute(CharTermAttribute.class).toString());
            }
        }
        catch(IOException e) {
            // not thrown b/c we're using a string reader...
        }
        
        Vector<NGramTerm> ngtv = new Vector<NGramTerm>();
        for(String s: result){
        	NGramTerm t = new NGramTerm(s);
        	ngtv.add(t);
        }
		return ngtv;
	}
}
