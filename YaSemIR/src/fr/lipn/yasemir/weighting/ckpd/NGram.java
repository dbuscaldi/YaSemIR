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
import java.util.Vector;

public class NGram {
	private Vector<NGramTerm> sequence;
	private int distanceFromLongest;
	private double weight;
	
	public NGram(){
		sequence=new Vector<NGramTerm>();
	}
	
	public void add(NGramTerm term) {
		sequence.add(term);
	}
	
	public int getSize(){
		return sequence.size();
	}
	
	public boolean equals(Object anotherObj){
		NGram other = (NGram)anotherObj;
		return this.equals(other);
	}
	
	public boolean equals(NGram other){
		if(this.getSize()==other.getSize()){
			for(int i=0; i< sequence.size(); i++){
				if(sequence.get(i).equals(other.sequence.get(i))) continue;
				else return false;
			}
			return true;
		} else return false;
	}
	
	public String repr(){
		StringBuffer buf = new StringBuffer();
		for(NGramTerm t : sequence) {
			buf.append(t.repr());
			buf.append(" ");
		}
		return buf.toString().trim();
	}
	
	public int hashCode(){
		return this.repr().hashCode();
	}
	
	public boolean containedIn(NGram other){
		String r1=this.repr();
		String r2=other.repr();
		if(r2.contains(r1)) return true;
		else return false;
	}
	
	public void calculateDistance(NGram largest, NGram phrase){
		String lrgRep=largest.repr();
		String phrRep=phrase.repr();
		
		int lrgStart=phrRep.indexOf(lrgRep);
		int lrgEnd=lrgStart+lrgRep.length();
		
		int start = phrRep.indexOf(this.repr());
		int end = start+ this.repr().length();
		
		if(start >= lrgStart && end <= lrgEnd) this.distanceFromLongest=0;
		else {
			if(start > lrgEnd) this.distanceFromLongest=(start-lrgEnd);
			if(end < lrgStart) this.distanceFromLongest=(lrgStart-end);
		}
	}
	
	public void setWeights(){
		double tmpW=0.0;
		for(NGramTerm t : sequence) {
			tmpW+=t.getWeight();
		}
		this.weight=tmpW;
	}
	
	public double getWeight(){
		return this.weight;
	}
	
	public double getDistanceCoeff(){
		double eps=0.1;
		return (1.0+eps*Math.log((double)(1+this.distanceFromLongest)));
	}
	
	public Vector<NGramTerm> getSequence() {
		return sequence;
	}

}
