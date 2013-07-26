package fr.lipn.yasemir.ontology.annotation;

import java.util.HashMap;
import java.util.Vector;

import org.apache.lucene.document.Document;


public interface SemanticAnnotator {
	public HashMap<String, Vector<Annotation>> annotate(String document);
	public void addSemanticAnnotation(Document doc, String text);
}
