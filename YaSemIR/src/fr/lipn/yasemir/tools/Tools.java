package fr.lipn.yasemir.tools;

import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.lipn.yasemir.ontology.annotation.Annotation;

public class Tools {
	
	public static boolean checkPattern(String text, String pattern){
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(text.toLowerCase());
		
		if(m.find()) {
			//System.err.println("found pattern: "+m.group());
			return true;
		}
		return false;
	}
	
	

	public static Collection<? extends Annotation> extractCategories(List<String> categoryList) {
		Vector<Annotation> annotations = new Vector<Annotation>();
		for(String catID : categoryList){
			Annotation ann = new Annotation(catID);
			annotations.add(ann);
		}
		return annotations;
	}
}
