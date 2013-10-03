package fr.lipn.yasemir.ontology.annotation;

import java.util.HashMap;
import java.util.Vector;

import org.apache.lucene.document.Document;


public interface SemanticAnnotator {
	/**
	 * Returns the annotation map (linking an ontology ID to the list of annotations referring to that ontology)
	 * for a given text
	 * @param document
	 * @return
	 */
	public HashMap<String, Vector<Annotation>> annotate(String document);
	/**
	 * Method that add an annotation to the input document
	 * @param doc
	 * @param text
	 */
	public void addSemanticAnnotation(Document doc, String text);
}
