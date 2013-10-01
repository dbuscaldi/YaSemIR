package fr.lipn.yasemir.indexing;
/*
 * Copyright (C) 2012, Universitè Paris 13
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
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import fr.lipn.yasemir.configuration.Yasemir;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * @author Davide Buscaldi, LIPN
 */
public class YasemirIndexer {
	
	private YasemirIndexer() {}
	
	/** Index all text files under a directory. */
	public static void main(String[] args) {
		Yasemir.init("config.xml");
		
		String indexPath = Yasemir.INDEX_DIR;
		String docsPath = Yasemir.COLLECTION_DIR;
		String lang = Yasemir.COLLECTION_LANG;
		  
	    boolean create = true; //TODO: allow update mode?
	
	    if (docsPath == null) {
	      System.err.println("[YaSemIR] No collection specified in the config file!");
	      System.exit(1);
	    }

	    final File docDir = new File(docsPath);
	    if (!docDir.exists() || !docDir.canRead()) {
	      System.out.println("[YaSemIR] Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
	      System.exit(1);
	    }
	    
	    Date start = new Date();
	    try {
	      System.out.println("[YaSemIR] Indexing to directory '" + indexPath + "'...");

	      Directory dir = FSDirectory.open(new File(indexPath));
	      Analyzer analyzer = null;
	      if(lang.equals("fr")) analyzer = new FrenchAnalyzer(Version.LUCENE_44);
	      else if(lang.equals("it")) analyzer = new ItalianAnalyzer(Version.LUCENE_44);
	      else if(lang.equals("es")) analyzer = new SpanishAnalyzer(Version.LUCENE_44);
	      else if(lang.equals("de")) analyzer = new GermanAnalyzer(Version.LUCENE_44);
	      else if(lang.equals("pt")) analyzer = new PortugueseAnalyzer(Version.LUCENE_44);
	      else if(lang.equals("ca")) analyzer = new CatalanAnalyzer(Version.LUCENE_44);
	      else if(lang.equals("nl")) analyzer = new DutchAnalyzer(Version.LUCENE_44);
	      else analyzer = new EnglishAnalyzer(Version.LUCENE_44);
	      
	      //IndexWriter Configuration
	      IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_44, analyzer);
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
	      System.out.println(end.getTime() - start.getTime() + " total milliseconds");

	    } catch (IOException e) {
	      System.out.println(" caught a " + e.getClass() +
	       "\n with message: " + e.getMessage());
	    }
	  }

	  static void indexDocs(IndexWriter writer, File file)
	    throws IOException {
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
	    		YasemirSimpleXMLFileHandler hdlr = null;
	            try {
	            		hdlr = new YasemirSimpleXMLFileHandler(file);
	            		
	            		//Document doc=hdlr.getParsedDocument();
	            		
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
