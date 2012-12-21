package fr.lipn.yasemir.weighting.ckpd.ranking;

import java.util.Vector;

import fr.lipn.yasemir.weighting.ckpd.NGramComparer;
import fr.lipn.yasemir.weighting.ckpd.NGramTerm;
import fr.lipn.yasemir.weighting.ckpd.TermFactory;

public class CKPDRankedDocument implements Comparable<CKPDRankedDocument> {
	private String docID;
	private Float weight;
	
	public CKPDRankedDocument(String docID, String text, Vector<NGramTerm> queryNGSet){
		this.docID=docID;
		Vector<NGramTerm> tv = TermFactory.makeTermSequence(text);
		
		this.weight=new Float(NGramComparer.compare(queryNGSet, tv));
	}
	
	public int compareTo(CKPDRankedDocument o) {
		return (-this.weight.compareTo(o.weight));
	}
	
	public float getScore() {
		return this.weight.floatValue();
	}
	
	public String getID() {
		return this.docID;
	}
	

}
