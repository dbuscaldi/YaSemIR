package fr.lipn.yasemir.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import fr.lipn.yasemir.configuration.Yasemir;
import fr.lipn.yasemir.search.RankedDocument;
import fr.lipn.yasemir.search.SemanticSearcher;


public class YasemirDemoSearch {
	
	 /** Simple command-line based on lucene search demo. */
	  public static void main(String[] args) throws Exception {
	    String usage =
	      "Usage:\tjava fr.lipn.yasemir.search.YasemirInteractiveSearch [-index dir] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/java/4_0/demo.html for details.";
	    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
	      System.out.println(usage);
	      System.exit(0);
	    }
		
		Yasemir.init("config.xml");
		
	    String index = Yasemir.INDEX_DIR; //it can be overridden by the command line -index parameter
		String lang = Yasemir.COLLECTION_LANG;
		
	    String queries = null;
	    int repeat = 0;
	    boolean raw = false;
	    String queryString = null;
	    int hitsPerPage = 5;
	    
	    for(int i = 0;i < args.length;i++) {
	      if ("-index".equals(args[i])) {
	        index = args[i+1];
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
	    SemanticSearcher ssearcher = new SemanticSearcher(lang, reader);
		
	    BufferedReader in = null;
	    if (queries != null) {
	      in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), "UTF-8"));
	    } else {
	      String osName = System.getProperty("os.name");
	      if(osName.startsWith("Win")) in = new BufferedReader(new InputStreamReader(System.in, "cp1252"));
	      else in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
	    }
	    
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
	      
	      Vector<RankedDocument> results = ssearcher.search(line);
	      
	      doPagingSearch(in, results, hitsPerPage, raw, queries == null && queryString == null);

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
	   */
	  public static void doPagingSearch(BufferedReader in, Vector<RankedDocument> rankedres, int hitsPerPage, boolean raw, boolean interactive) throws IOException {

	    int start = 0;
	    int resSize=rankedres.size();
	    int end = Math.min(resSize, hitsPerPage);
	        
	    while (true) {
	      for (int i = start; i < end; i++) {
	    	RankedDocument doc = rankedres.elementAt(i);
	    	  if (raw) {                              // output raw format
	          System.out.println("doc="+doc.getID()+" score="+doc.getScore());
	          continue;
	        }
	        
	        String path = doc.getID();
	        
	        if (path != null) {
	          System.out.println((i+1) + ". " + path + " score="+doc.getScore());
	          System.out.println("\tID: " + doc.getID());
	          
	          System.out.println("Annotations:");
	          String baseAnnot=doc.getBaseAnnotations();
	          if(baseAnnot!= null) System.out.println(formatTextWidth(baseAnnot, 120));
	          
	          System.out.println("Text:");
	          String text=doc.getText();
	          if(text!= null) System.out.println(formatTextWidth(text, 120));
	          
	          //remove comments below to print also subsumers
	          /*
	          System.out.println("Expanded Annotations (subsumers):");
	          String subsAnnot=doc.getSubsumerAnnotations();
	          if(subsAnnot!= null) System.out.println(formatTextWidth(subsAnnot, 120));
	          */
	          
	        } else {
	          System.out.println((i+1) + ". " + "Unknown document");
	        }
	                  
	      }

	      if (!interactive || end == 0) {
	        break;
	      }

	      if (resSize >= end) {
	        boolean quit = false;
	        while (true) {
	          System.out.print("Press ");
	          if (start - hitsPerPage >= 0) {
	            System.out.print("(p)revious page, ");  
	          }
	          if (start + hitsPerPage < resSize) {
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
	            if (start + hitsPerPage < resSize) {
	              start+=hitsPerPage;
	            }
	            break;
	          } else {
	            int page = Integer.parseInt(line);
	            if ((page - 1) * hitsPerPage < resSize) {
	              start = (page - 1) * hitsPerPage;
	              break;
	            } else {
	              System.out.println("No such page");
	            }
	          }
	        }
	        if (quit) break;
	        end = Math.min(resSize, start + hitsPerPage);
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
