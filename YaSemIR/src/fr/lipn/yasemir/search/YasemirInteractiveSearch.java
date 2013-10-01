package fr.lipn.yasemir.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import fr.lipn.yasemir.configuration.Yasemir;
import fr.lipn.yasemir.ontology.KnowledgeBattery;
import fr.lipn.yasemir.ontology.annotation.Annotation;


public class YasemirInteractiveSearch {
	private final static int MAX_HITS=1000;
	
	 /** Simple command-line based on lucene search demo. */
	  public static void main(String[] args) throws Exception {
	    String usage =
	      "Usage:\tjava fr.lipn.yasemir.search.YasemirInteractiveSearch [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/java/4_0/demo.html for details.";
	    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
	      System.out.println(usage);
	      System.exit(0);
	    }
		
		Yasemir.init("config.xml");
		
	    String index = Yasemir.INDEX_DIR;
		String labelIndex = Yasemir.TERM_DIR;
		String lang = Yasemir.COLLECTION_LANG;
		
	    String basefield = "text";
	    String queries = null;
	    int repeat = 0;
	    boolean raw = false;
	    String queryString = null;
	    int hitsPerPage = 10;
	    
	    for(int i = 0;i < args.length;i++) {
	      if ("-index".equals(args[i])) {
	        index = args[i+1];
	        i++;
	      } else if ("-field".equals(args[i])) {
	        basefield = args[i+1];
	        i++;
	      } else if ("-queries".equals(args[i])) {
	        queries = args[i+1];
	        i++;
	      } else if ("-query".equals(args[i])) {
	        queryString = args[i+1];
	        i++;
	      } else if ("-repeat".equals(args[i])) {
	        repeat = Integer.parseInt(args[i+1]);
	        i++;
	      } else if ("-raw".equals(args[i])) {
	        raw = true;
	      } else if ("-paging".equals(args[i])) {
	        hitsPerPage = Integer.parseInt(args[i+1]);
	        if (hitsPerPage <= 0) {
	          System.err.println("There must be at least 1 hit per page.");
	          System.exit(1);
	        }
	        i++;
	      }
	    }
	    
	    IndexReader reader = IndexReader.open(FSDirectory.open(new File(index)));
	    IndexSearcher searcher = new IndexSearcher(reader);
	    if(Yasemir.SCORE.equals("BM25")) searcher.setSimilarity(new BM25Similarity());
	    
	    Analyzer analyzer = null;
	    if(lang.equals("fr")) analyzer = new FrenchAnalyzer(Version.LUCENE_44);
	    else if(lang.equals("it")) analyzer = new ItalianAnalyzer(Version.LUCENE_44);
	    else if(lang.equals("es")) analyzer = new SpanishAnalyzer(Version.LUCENE_44);
	    else if(lang.equals("de")) analyzer = new GermanAnalyzer(Version.LUCENE_44);
	    else if(lang.equals("pt")) analyzer = new PortugueseAnalyzer(Version.LUCENE_44);
	    else if(lang.equals("ca")) analyzer = new CatalanAnalyzer(Version.LUCENE_44);
	    else if(lang.equals("nl")) analyzer = new DutchAnalyzer(Version.LUCENE_44);
	    else analyzer = new EnglishAnalyzer(Version.LUCENE_44);
		
	    BufferedReader in = null;
	    if (queries != null) {
	      in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), "UTF-8"));
	    } else {
	      in = new BufferedReader(new InputStreamReader(System.in, "cp1252"));
	    }
	    
	    QueryParser parser = new QueryParser(Version.LUCENE_44, basefield, analyzer);
	    while (true) {
	      if (queries == null && queryString == null) {                        // prompt the user
	        System.out.println("[YaSemIr] Enter query: ");
	      }

	      String line = queryString != null ? queryString : in.readLine();

	      if (line == null || line.length() == -1) {
	        break;
	      }

	      line = line.trim();
	      if (line.length() == 0) {
	        break;
	      }
	      
	      if(Yasemir.MODE==Yasemir.CLASSIC){
	    	  Query query = parser.parse(line);
	    	  
	    	  System.out.println("[YaSemIr - CLASSIC] Searching for: " + query.toString(basefield));
	            
		      if (repeat > 0) {                           // repeat & time as benchmark
		        Date start = new Date();
		        for (int i = 0; i < repeat; i++) {
		          searcher.search(query, null, 100);
		        }
		        Date end = new Date();
		        System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
		      }
		      
		      doPagingSearch(in, searcher, query, hitsPerPage, raw, queries == null && queryString == null);
	      } else {
	          HashMap<String, Vector<Annotation>> queryAnnotation = null;
		      System.err.println("[YaSemIr] Annotating: " + line);
		      queryAnnotation=Yasemir.annotator.annotate(line);
		      
		      System.err.println("[YaSemIr] Annotations:");
		      for(String oid : queryAnnotation.keySet()) {
		    	  Vector<Annotation> ann = queryAnnotation.get(oid);
		    	  for(Annotation a : ann){
			    	  System.err.println(a.getOWLClass().getIRI());
			      }
		      }
		      
		      System.err.println("---------------------------");
		      
	     	  
		      if(Yasemir.MODE==Yasemir.SEMANTIC){
	    		  StringBuffer extQueryText = new StringBuffer();
		    	  for(String oid : queryAnnotation.keySet()) {
			    	  Vector<Annotation> ann = queryAnnotation.get(oid);
			    	  for(Annotation a : ann){
			    		  extQueryText.append(oid+"annot_exp:\""+a.getOWLClass().getIRI().getFragment()+"\"");
			    		  extQueryText.append(" ");
				      }
			      }
		    	  Query query=parser.parse(extQueryText.toString().trim());
		    	  System.err.println("[YaSemIr - SEMANTIC] Searching for: " + query.toString());
		    	  TopDocs results = searcher.search(query, MAX_HITS);
				  ScoreDoc[] hits = results.scoreDocs;
				    
				  int numTotalHits = results.totalHits;
				  System.err.println("[YaSemIr] "+numTotalHits + " total matching documents");
				  if(numTotalHits > 0) {
			    	Vector<SemanticallyRankedDocument> srDocs= new Vector<SemanticallyRankedDocument>();
			    	for (int i = 0; i < Math.min(numTotalHits, MAX_HITS); i++) {
				        Document doc = searcher.doc(hits[i].doc);
				        String id = doc.get("id");
				        List<IndexableField> docFields =doc.getFields();
				        StringBuffer concepts = new StringBuffer();
				        for(IndexableField f : docFields){
				        	String fname=f.name();
				        	if(fname.endsWith("annot") || fname.endsWith("annot_exp")) {
				        		concepts.append(fname+":"+doc.get(fname));
				        		concepts.append(" ");
				        	}
				        }
				        //String concepts = doc.get(field+"annot"); //here we have the annotation
				        //System.err.println(concepts);
				        SemanticallyRankedDocument srDoc = new SemanticallyRankedDocument(id, concepts.toString().trim(), queryAnnotation);
				        srDoc.setWeight(Yasemir.SIM_MEASURE);
				        srDocs.add(srDoc);
				        //System.out.println(oq.getID()+"\tQ0\t"+id+"\t"+i+"\t"+hits[i].score+"\t"+conf_str);
				    }
			    	
			    	Collections.sort(srDocs);
			    	int rank=0;
			    	for(SemanticallyRankedDocument srd : srDocs){
			    		//System.out.println("Q0\t"+srd.getID()+"\t"+rank+"\t"+String.format(Locale.US, "%.4f",srd.getScore()));
			    		System.out.println(rank);
			    		System.out.println(srd.getID()+" : "+srd.getScore());
			    		System.out.println(formatTextWidth(srd.toString(), 120));
			    	
			    		rank++;
			    		if(rank % 10 == 0){
			    			System.err.println("press enter to continue");
			    			String sline = in.readLine();
			    		}
			    	}
			    }
				//TODO: implement paging for semantic mode
				//doPagingSearch(in, searcher, query, hitsPerPage, raw, queries == null && queryString == null);
	    	  }
	    	  
	    	  if(Yasemir.MODE==Yasemir.HYBRID) {
	    		  StringBuffer extQueryText = new StringBuffer();
		    	  for(String oid : queryAnnotation.keySet()) {
			    	  Vector<Annotation> ann = queryAnnotation.get(oid);
			    	  for(Annotation a : ann){
			    		  extQueryText.append(oid+"annot_exp:\""+a.getOWLClass().getIRI().getFragment()+"\"");
			    		  extQueryText.append(" ");
				      }
			      }
		    	  Query query=parser.parse(extQueryText.toString().trim());
		    	  System.err.println("[YaSemIr - HYBRID] Searching for: " + query.toString());
		    	  TopDocs results = searcher.search(query, MAX_HITS);
				  ScoreDoc[] hits = results.scoreDocs;
				    
				  int numTotalHits = results.totalHits;
				  System.err.println("[YaSemIr] "+numTotalHits + " total matching documents");
				  if(numTotalHits > 0) {
			    	Vector<SemanticallyRankedDocument> srDocs= new Vector<SemanticallyRankedDocument>();
			    	for (int i = 0; i < Math.min(numTotalHits, MAX_HITS); i++) {
				        Document doc = searcher.doc(hits[i].doc);
				        String id = doc.get("id");
				        List<IndexableField> docFields =doc.getFields();
				        StringBuffer concepts = new StringBuffer();
				        for(IndexableField f : docFields){
				        	String fname=f.name();
				        	if(fname.endsWith("annot") || fname.endsWith("annot_exp")) {
				        		concepts.append(fname+":"+doc.get(fname));
				        		concepts.append(" ");
				        	}
				        }
				        //String concepts = doc.get(field+"annot"); //here we have the annotation
				        //System.err.println(concepts);
				        SemanticallyRankedDocument srDoc = new SemanticallyRankedDocument(id, concepts.toString().trim(), queryAnnotation);
				        srDoc.setWeight(Yasemir.SIM_MEASURE);
				        srDoc.includeClassicWeight(hits[i].score); //model with both classic and CKPD weights
				        srDocs.add(srDoc);
				        //System.out.println(oq.getID()+"\tQ0\t"+id+"\t"+i+"\t"+hits[i].score+"\t"+conf_str);
				    }
			    	
			    	Collections.sort(srDocs);
			    	int rank=0;
			    	for(SemanticallyRankedDocument srd : srDocs){
			    		//System.out.println("Q0\t"+srd.getID()+"\t"+rank+"\t"+String.format(Locale.US, "%.4f",srd.getScore()));
			    		System.out.println(rank);
			    		System.out.println(srd.getID()+" : "+srd.getScore());
			    		System.out.println(formatTextWidth(srd.toString(), 120));
			    	
			    		rank++;
			    		if(rank % 10 == 0){
			    			System.err.println("press enter to continue");
			    			String sline = in.readLine();
			    		}
			    	}
			    }
	    	  }
	      }

	      if (queryString != null) {
	        break;
	      }
	    }
	    reader.close();
	  }

	  /**
	   * This demonstrates a typical paging search scenario, where the search engine presents 
	   * pages of size n to the user. The user can then go to the next page if interested in
	   * the next hits.
	   * 
	   * When the query is executed for the first time, then only enough results are collected
	   * to fill 5 result pages. If the user wants to page beyond this limit, then the query
	   * is executed another time and all hits are collected.
	   * 
	   */
	  public static void doPagingSearch(BufferedReader in, IndexSearcher searcher, Query query, 
	                                     int hitsPerPage, boolean raw, boolean interactive) throws IOException {
	 
	    // Collect enough docs to show 5 pages
	    TopDocs results = searcher.search(query, 5 * hitsPerPage);
	    ScoreDoc[] hits = results.scoreDocs;
	    
	    int numTotalHits = results.totalHits;
	    System.out.println(numTotalHits + " total matching documents");

	    int start = 0;
	    int end = Math.min(numTotalHits, hitsPerPage);
	        
	    while (true) {
	      if (end > hits.length) {
	        System.out.println("Only results 1 - " + hits.length +" of " + numTotalHits + " total matching documents collected.");
	        System.out.println("Collect more (y/n) ?");
	        String line = in.readLine();
	        if (line.length() == 0 || line.charAt(0) == 'n') {
	          break;
	        }

	        hits = searcher.search(query, numTotalHits).scoreDocs;
	      }
	      
	      end = Math.min(hits.length, start + hitsPerPage);
	      
	      for (int i = start; i < end; i++) {
	        if (raw) {                              // output raw format
	          System.out.println("doc="+hits[i].doc+" score="+hits[i].score);
	          continue;
	        }

	        Document doc = searcher.doc(hits[i].doc);
	        String path = doc.get("id");
	        
	        List<IndexableField> fields =doc.getFields();
	        Vector<String> annotationFields = new Vector<String>();
	        Vector<String> expansionFields = new Vector<String>();
	        for(IndexableField f : fields) {
	        	//System.err.println("fieldName:"+f.name());
	        	if(f.name().endsWith("annot")){
	        		annotationFields.add(f.name());
	        	}
	        	if(f.name().endsWith("annot_exp")){
	        		expansionFields.add(f.name());
	        	}
	        }
	        
	        if (path != null) {
	          System.out.println((i+1) + ". " + path + " score="+hits[i].score);
	          System.out.println("\tID: " + doc.get("id"));
	          
	          System.out.println("Text:");
	          String text=doc.get("text");
	          if(text!= null) System.out.println(formatTextWidth(doc.get("text"), 120));
	          
	          System.out.println("Annotations:");
	          for(String fld : annotationFields) {
	        	  String oid = fld.replaceAll("annot", "");
	        	  String oname = KnowledgeBattery.ontoForID(oid).getBaseAddr();
	        	  
	        	  System.out.println(oname+" ("+oid+") : "+doc.get(fld));
	          }
	          System.out.println("Expanded Annotations:");
	          for(String efld : expansionFields) {
	        	  String oid = efld.replaceAll("annot_exp", "");
	        	  String oname = KnowledgeBattery.ontoForID(oid).getBaseAddr();
	        	  
	        	  System.out.println(oname+" ("+oid+") : "+doc.get(efld));
	          }
	          
	        } else {
	          System.out.println((i+1) + ". " + "Unknown document");
	        }
	                  
	      }

	      if (!interactive || end == 0) {
	        break;
	      }

	      if (numTotalHits >= end) {
	        boolean quit = false;
	        while (true) {
	          System.out.print("Press ");
	          if (start - hitsPerPage >= 0) {
	            System.out.print("(p)revious page, ");  
	          }
	          if (start + hitsPerPage < numTotalHits) {
	            System.out.print("(n)ext page, ");
	          }
	          System.out.println("(q)uit or enter number to jump to a page.");
	          
	          String line = in.readLine();
	          if (line.length() == 0 || line.charAt(0)=='q') {
	            quit = true;
	            break;
	          }
	          if (line.charAt(0) == 'p') {
	            start = Math.max(0, start - hitsPerPage);
	            break;
	          } else if (line.charAt(0) == 'n') {
	            if (start + hitsPerPage < numTotalHits) {
	              start+=hitsPerPage;
	            }
	            break;
	          } else {
	            int page = Integer.parseInt(line);
	            if ((page - 1) * hitsPerPage < numTotalHits) {
	              start = (page - 1) * hitsPerPage;
	              break;
	            } else {
	              System.out.println("No such page");
	            }
	          }
	        }
	        if (quit) break;
	        end = Math.min(numTotalHits, start + hitsPerPage);
	      }
	    }
	  }
	  
	  public static String formatTextWidth(String input, int maxLineLength) {
		    String [] fragments = input.split("[\\s]+");
		    StringBuilder output = new StringBuilder(input.length());
		    int lineLen = 0;
		    for(String word: fragments) {
		       
		        if (lineLen + word.length() > maxLineLength) {
		            output.append("\n");
		            lineLen = 0;
		        }
		        output.append(word);
		        output.append(" ");
		        lineLen += (word.length()+1);
		    }
		    return output.toString();
		}
}
