package fr.lipn.yasemir.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import fr.lipn.yasemir.ontology.ConceptSimilarity;
import fr.lipn.yasemir.ontology.annotation.Annotation;
import fr.lipn.yasemir.ontology.annotation.IndexBasedAnnotator;

public class InteractiveSearch {
	private final static int TITLE_ONLY=0;
	private final static int TITLE_DESC=1;
	private final static int CLASSIC=0;
	private final static int SEMANTIC=1;
	private final static int HYBRID=2;
	private final static boolean CKPD_ENABLED=false;
	
	private final static int MAX_HITS=1000;
	
	private static int MODE=SEMANTIC;
	private static boolean USE_TAGS=true; //consider using manually annotated tags for IR or not
	
	private static int SIM_MEASURE=ConceptSimilarity.PROXYGENEA2;
	
	 /** Simple command-line based on lucene search demo. */
	  public static void main(String[] args) throws Exception {
	    String usage =
	      "Usage:\tjava fr.irit.moano.search.InteractiveSearch [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/java/4_0/demo.html for details.";
	    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
	      System.out.println(usage);
	      System.exit(0);
	    }

	    String index = "indexOHSUMed_sem";
		String labelIndex = "termIndex_0.6";
		
	    String field = "text";
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
	        field = args[i+1];
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
	    //Analyzer analyzer = new FrenchAnalyzer(Version.LUCENE_31);
	    Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_31);
	    
	    IndexBasedAnnotator sa = new IndexBasedAnnotator("termIndex_0.6"); 
		
		
	    BufferedReader in = null;
	    if (queries != null) {
	      in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), "UTF-8"));
	    } else {
	      in = new BufferedReader(new InputStreamReader(System.in, "cp1252"));
	    }
	    QueryParser parser = new QueryParser(Version.LUCENE_31, field, analyzer);
	    while (true) {
	      if (queries == null && queryString == null) {                        // prompt the user
	        System.out.println("Enter query: ");
	      }

	      String line = queryString != null ? queryString : in.readLine();

	      if (line == null || line.length() == -1) {
	        break;
	      }

	      line = line.trim();
	      if (line.length() == 0) {
	        break;
	      }
	      
	      System.out.println("Annotating: " + line);
	      Vector<Annotation> ann = new Vector<Annotation>();
	      ann.addAll(sa.annotate(line));
	      
	      System.out.println("Annotation:");
	      for(Annotation a : ann){
	    	  System.out.println(a.getOWLClass().getIRI().getFragment());
	      }
	      System.out.println("---------------------------");
	      
	      Query query = parser.parse(line);
	      if(MODE==CLASSIC){
	    	  System.out.println("Searching for: " + query.toString(field));
	            
		      if (repeat > 0) {                           // repeat & time as benchmark
		        Date start = new Date();
		        for (int i = 0; i < repeat; i++) {
		          searcher.search(query, null, 100);
		        }
		        Date end = new Date();
		        System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
		      }
	      } else {
	    	  if(MODE==SEMANTIC){
	    		  //TODO: integrare tutto quello che si fa sul SemanticSearcher
	    	  }
	      }
	      

	      doPagingSearch(in, searcher, query, hitsPerPage, raw, queries == null && queryString == null);

	      if (queryString != null) {
	        break;
	      }
	    }
	    searcher.close();
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
	        if (path != null) {
	          System.out.println((i+1) + ". " + path + " score="+hits[i].score);
	          System.out.println("\tID: " + doc.get("id"));
	          System.out.println("\tTags: " + doc.get("tag"));
	          System.out.println("Titre: " + doc.get("title"));
	          String text=doc.get("text");
	          if(text!= null) System.out.println(formatTextWidth(doc.get("text"), 120));
	        } else {
	          System.out.println((i+1) + ". " + "No title for this document");
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
