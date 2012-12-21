package fr.lipn.yasemir.search;

import java.util.List;
import java.util.Vector;

public class OHSUQuery {
	private String id;
	private String title;
	private String desc;
	private Vector<String> categories;
	
	public void setID(String id) {
		this.id=id.trim();
	}
	
	public void setTitle(String title) {
		this.title=title.trim();
	}
	
	public void setDescription(String desc){
		this.desc=desc.trim();
	}
	
	public void setCategories(String categories){
		this.categories=new Vector<String>();
		String [] cats = categories.split(";");
		for(String s : cats) this.categories.add(s.trim().toLowerCase());
	}
	
	public String getTitle(){
		return this.title;
	}
	
	public String getDescription(){
		return this.desc;
	}
	
	public String getCategories() {
		StringBuffer ret = new StringBuffer();
		for(String s : categories){
			ret.append(s);
			ret.append(" ");
		}
		return ret.toString().trim();
	}
	
	public List<String> getCategoryList() {
		return categories;
	}
	
	public String getID(){
		return this.id;
	}

	public void setCategoryVector(Vector<String> catVec) {
		this.categories=new Vector<String>();
		this.categories.addAll(catVec);
	}
}
