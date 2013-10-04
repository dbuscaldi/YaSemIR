package fr.lipn.yasemir.test;
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
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import fr.lipn.yasemir.weighting.ckpd.NGram;
import fr.lipn.yasemir.weighting.ckpd.NGramFactory;
import fr.lipn.yasemir.weighting.ckpd.NGramTerm;
import fr.lipn.yasemir.weighting.ckpd.TermFactory;

public class TestNGrams {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		String index = "indexOHSUMed_sem";
		IndexReader reader = IndexReader.open(FSDirectory.open(new File(index)));
		Analyzer analyzer;
		analyzer =  new EnglishAnalyzer(Version.LUCENE_44);
		
		String test = "The quick brown fox jumps over the lazy dog.";
		
		TermFactory.init(reader, analyzer);
		Vector<NGramTerm> tv = TermFactory.makeTermSequence(test);
		
		HashSet<NGram> ngset =NGramFactory.getNGramSet(tv);
		for(NGram ng : ngset) {
			System.out.println(ng.repr());
			for(NGramTerm t : ng.getSequence()){
				System.out.println(t.getText()+" -> "+t.getWeight());
			}
		}

	}

}
