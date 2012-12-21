package fr.lipn.yasemir.ontology.annotation;

import java.util.Vector;

import org.apache.lucene.document.Document;

public interface SemanticAnnotator {
	public Vector<Annotation> annotate(String document);
}
