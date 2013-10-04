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

import org.apache.lucene.index.Term;

public class NGramTerm {
	private String text;
	double weight;
	
	public NGramTerm(String txt) {
		this.setText(txt);
		int nCount;
		try {
			nCount = TermFactory.reader.docFreq(new Term("text", txt))+1; //+1 to avoid infinity
			this.weight = 1.0-(Math.log10((double)nCount))/(Math.log10((double)TermFactory.reader.maxDoc()));
		} catch (IOException e) {
			e.printStackTrace();
			this.weight=0d;
		}
		
	}

	public String repr() {
		return "term:"+getText();
	}
	
	public boolean equals(Object other) {
		NGramTerm t = (NGramTerm)other;
		return this.equals(t);
	}
	
	public boolean equals(NGramTerm other){
		return this.getText().equals(other.getText()); //ignoring POS
	}
	
	public int hashCode(){
		return this.getText().hashCode();
	}

	public double getWeight() {
		return this.weight;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
