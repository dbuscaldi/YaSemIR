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
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

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
	    String usage = "java fr.irit.moano.indexing.SimpleIndexer"
	                 + " [-index INDEX_PATH] -docs DOCS_PATH [-update]\n\n"
	                 + "This indexes the documents in DOCS_PATH, creating a Lucene index"
	                 + "in INDEX_PATH that can be searched with SearchFiles";
	    String indexPath = "index";
	    String docsPath = null;
	    
	    for(String arg : args){
	    	System.err.println(arg);
	    }
	    
	    boolean create = true;
	    for(int i=0;i<args.length;i++) {
	      if ("-index".equals(args[i])) {
	        indexPath = args[i+1];
	        i++;
	      } else if ("-docs".equals(args[i])) {
	        docsPath = args[i+1];
	        i++;
	      } else if ("-update".equals(args[i])) {
	        create = false;
	      }
	    }

	    if (docsPath == null) {
	      System.err.println("Usage: " + usage);
	      System.exit(1);
	    }

	    final File docDir = new File(docsPath);
	    if (!docDir.exists() || !docDir.canRead()) {
	      System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
	      System.exit(1);
	    }
	    
	    Date start = new Date();
	    try {
	      System.out.println("Indexing to directory '" + indexPath + "'...");

	      Directory dir = FSDirectory.open(new File(indexPath));
	      Analyzer analyzer = new FrenchAnalyzer(Version.LUCENE_31);
	      IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_31, analyzer);

	      if (create) {
	        // Create a new index in the directory, removing any
	        // previously indexed documents:
	        iwc.setOpenMode(OpenMode.CREATE);
	      } else {
	        // Add new documents to an existing index:
	        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
	      }

	      // Optional: for better indexing performance, if you
	      // are indexing many documents, increase the RAM
	      // buffer.  But if you do this, increase the max heap
	      // size to the JVM (eg add -Xmx512m or -Xmx1g):
	      //
	      // iwc.setRAMBufferSizeMB(256.0);

	      IndexWriter writer = new IndexWriter(dir, iwc);
	      indexDocs(writer, docDir);

	      // NOTE: if you want to maximize search performance,
	      // you can optionally call forceMerge here.  This can be
	      // a terribly costly operation, so generally it's only
	      // worth it when your index is relatively static (ie
	      // you're done adding documents to it):
	      //
	      // writer.forceMerge(1);

	      writer.close();

	      Date end = new Date();
	      System.out.println(end.getTime() - start.getTime() + " total milliseconds");

	    } catch (IOException e) {
	      System.out.println(" caught a " + e.getClass() +
	       "\n with message: " + e.getMessage());
	    }
	  }

	  /**
	   * Indexes the given file using the given writer, or if a directory is given,
	   * recurses over files and directories found under the given directory.
	   * 
	   * NOTE: This method indexes one document per input file.  This is slow.  For good
	   * throughput, put multiple documents into your input file(s).  An example of this is
	   * in the benchmark module, which can create "line doc" files, one document per line,
	   * using the
	   * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
	   * >WriteLineDocTask</a>.
	   *  
	   * @param writer Writer to the index where the given file/dir info will be stored
	   * @param file The file to index, or the directory to recurse into to find files to index
	   * @throws IOException
	   */
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
	    		System.out.println("indexing " + file);
	            try {
	            		XMLVilmorinFileHandler hdlr = new XMLVilmorinFileHandler(file);
	            		Document doc=hdlr.getParsedDocument();
	            		writer.addDocument(doc);
	            } catch (Exception e) {
	            	e.printStackTrace();
	            } 
		      }
		    }
	    }
	  }
	
}
