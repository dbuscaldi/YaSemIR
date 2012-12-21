package fr.lipn.yasemir.weighting.ckpd;

import java.io.IOException;

import org.apache.lucene.index.Term;

public class NGramTerm {
	String text;
	double weight;
	
	public NGramTerm(String txt) {
		this.text=txt;
		int nCount;
		try {
			nCount = TermFactory.searcher.docFreq(new Term("text", txt))+1; //+1 to avoid infinity
			this.weight = 1.0-(Math.log10((double)nCount))/(Math.log10((double)TermFactory.searcher.maxDoc()));
		} catch (IOException e) {
			e.printStackTrace();
			this.weight=0d;
		}
		
	}

	public String repr() {
		return "term:"+text;
	}
	
	public boolean equals(Object other) {
		NGramTerm t = (NGramTerm)other;
		return this.equals(t);
	}
	
	public boolean equals(NGramTerm other){
		return this.text.equals(other.text); //ignoring POS
	}
	
	public int hashCode(){
		return this.text.hashCode();
	}

	public double getWeight() {
		return this.weight;
	}
}
