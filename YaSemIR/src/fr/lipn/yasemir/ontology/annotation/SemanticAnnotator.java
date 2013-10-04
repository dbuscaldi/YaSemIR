package fr.lipn.yasemir.ontology.annotation;
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

import java.util.HashMap;
import java.util.Vector;

import org.apache.lucene.document.Document;

/**
 * All Semantic annotation module should implement this interface
 * @author buscaldi
 *
 */
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
