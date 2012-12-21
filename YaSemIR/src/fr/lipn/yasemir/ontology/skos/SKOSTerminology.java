package fr.lipn.yasemir.ontology.skos;
/*
 * Copyright (C) 2012, Université Paris Nord
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

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

public class SKOSTerminology {
	boolean isStemmed=false; //used to track whether the terminology is stored in stemmed form or not
	
	public SKOSTerminology(String path){
		Model model = ModelFactory.createDefaultModel();
		InputStream in = FileManager.get().open( path );
		if (in == null) {
		    throw new IllegalArgumentException(
		                                 "File: " + path + " not found");
		}

		// read the RDF/XML file
		model.read(in, null);

		// write it to standard out
		//model.write(System.out);
		
	}
}
