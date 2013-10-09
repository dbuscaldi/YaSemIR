package fr.lipn.yasemir.indexing;
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
import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ca.CatalanAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.pt.PortugueseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import fr.lipn.yasemir.Yasemir;
/**
 * Class providing the necessary methods to index the document collection specified in the configuration file
 * @author buscaldi
 *
 */
public class YasemirIndexBuilder {
	private String indexPath;
	private String docsPath;
	private String lang;
	private File docDir;
	
	boolean create = true; //NOTE: should we allow an update mode?
	
	/**
	 * Initialisation of the Index builder
	 * @param config_file
	 */
	public YasemirIndexBuilder(String config_file){
		Yasemir.setIndexing(true); //activate indexing mode
		Yasemir.init(config_file);
		
		indexPath = Yasemir.INDEX_DIR;
		docsPath = Yasemir.COLLECTION_DIR;
		lang = Yasemir.COLLECTION_LANG;

	    if (docsPath == null) {
	      System.err.println("[YaSemIR] No collection specified in the config file!");
	      System.exit(1);
	    }

	    docDir = new File(docsPath);
	    if (!docDir.exists() || !docDir.canRead()) {
	      System.err.println("[YaSemIR] Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
	      System.exit(1);
	    }
	    

	    
	}
	/**
	 * Method that starts the actual indexing
	 */
	public void run() {
		Date start = new Date();
	    try {
	      System.err.println("[YaSemIR] Indexing to directory '" + indexPath + "'...");

	      Directory dir = FSDirectory.open(new File(indexPath));

	      //IndexWriter Configuration
	      IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_44, Yasemir.analyzer);
	      if(Yasemir.SCORE.equals("BM25")) iwc.setSimilarity(new BM25Similarity());
	      else iwc.setSimilarity(new DefaultSimilarity());

	      if (create) {
	        // Create a new index in the directory, removing any
	        // previously indexed documents:
	        iwc.setOpenMode(OpenMode.CREATE);
	      } else {
	        // Add new documents to an existing index:
	        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
	      }

	      IndexWriter writer = new IndexWriter(dir, iwc);
	      indexDocs(writer, docDir);

	      writer.close();

	      Date end = new Date();
	      System.err.println(end.getTime() - start.getTime() + " total milliseconds");
	    } catch (IOException e) {
	    	System.err.println(" caught a " + e.getClass() +
	   	       "\n with message: " + e.getMessage());
   	    }
	}
	    
	private void indexDocs(IndexWriter writer, File file) throws IOException {
	    // do not try to index files that cannot be read
	    if (file.canRead()) {
	      if (file.isDirectory()) {
	        String[] files = file.list();
	        // an IO error could occur
	        if (files != null) {
	          for (String f : files) {
	            indexDocs(writer, new File(file, f));
	          }
	        }
	      } else {
	    	if(file.getName().endsWith(".xml")) {
	    		System.err.println("[YaSemIR] indexing " + file);
	    		YasemirXMLFileHandler hdlr = null;
	            try {
	            		hdlr = new YasemirXMLFileHandler(file);
	            		
	            } catch (Exception e) {
	            	e.printStackTrace();
	            }
	            
	            for(Document doc : hdlr.getParsedDocuments()){
        			writer.addDocument(doc);
        		}
	            
		      }
		    }
	   }
	}
	
}
