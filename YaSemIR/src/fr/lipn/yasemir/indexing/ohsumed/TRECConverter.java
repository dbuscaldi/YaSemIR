package fr.lipn.yasemir.indexing.ohsumed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.semanticweb.owlapi.model.OWLClass;

import fr.lipn.yasemir.ontology.Ontology;
import fr.lipn.yasemir.ontology.annotation.Annotation;
import fr.lipn.yasemir.ontology.annotation.IndexBasedAnnotator;

public class TRECConverter {
	static boolean K_NN_ANNOTATOR=false;
		
	private static void parseOHSUFile(String filename, IndexBasedAnnotator ann){
		try {
			BufferedReader reader;
			if(filename.endsWith("gz")){
				reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename))));
			} else reader = new BufferedReader(new FileReader(filename));
		    
			String line;
		    
			String id = "",journal="",tags="",abst="",docType="",text="", authors="";
		    Vector<Annotation> tagAnnot = new Vector<Annotation>();
		      
		    while ((line = reader.readLine()) != null)
		    {
		      
		      
		      if(line.startsWith(".I")) {
		    	  //System.err.println(line);
		    	  if(!id.equals("")){
		    		  System.out.println("<DOC>");
		    		  System.out.println("<DOCNO>"+id+"</DOCNO>");
		    		  System.out.println("<DOCID>"+id+"</DOCID>");
		    		  System.out.println("<SOURCE>"+journal+"</SOURCE>");
		    		  System.out.println("<ANNOTATION>");
				      HashSet<Annotation> annotations = new HashSet<Annotation>();
				      annotations.addAll(tagAnnot);
				      for(Annotation a : annotations){
				    	  System.out.println("<CONCEPT>"+a.getOWLClass().getIRI().toString()+"</CONCEPT>");
				      }
				      System.out.println("</ANNOTATION>");
				      System.out.println("<TITLE>");
				      System.out.println(abst);
				      System.out.println("</TITLE>");
				      System.out.println("<TEXT>");
				      System.out.println(text);
				      System.out.println("</TEXT>");
				      System.out.println("<TYPE>"+docType+"</TYPE>");
				      System.out.println("</DOC>");
		    	  }
		    	  tagAnnot.clear();
		    	  text="";
		      }
		      if(line.startsWith(".U")){
		    	  id = reader.readLine();
		    	  
		      }
		      if(line.startsWith(".S")){
		    	  journal = reader.readLine();
		    	  
		      }
		      if(line.startsWith(".M")){
		    	  tags = reader.readLine();
		    	  tags=tags.replaceAll("/(\\*)?.*?(;|\\.)", ";");
		    	  tagAnnot=ann.annotate(tags);
		      }
		      if(line.startsWith(".T")){
		    	  abst = reader.readLine();
		      }
		      if(line.startsWith(".P")){
		    	  docType = reader.readLine();
		      }
		      if(line.startsWith(".W")){
		    	  text = reader.readLine();
		    	  /*if(SEMANTIC_INDEXING){
		    		  ann.addSemanticAnnotation(currDoc, text, "text");
		    	  }*/
		      }
		      if(line.startsWith(".A")){
		    	  authors = reader.readLine();
		      }
		      
		    }
		    reader.close();
		    
		    System.out.println("</annotations>");
		} catch (Exception e) {
		   System.err.format("Exception occurred trying to read '%s'.", filename);
		   e.printStackTrace();
		}
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		String folder = "/users/buscaldi/Works/collabSIG/ohsumedTestColl/";
		String labelIndex = "termIndex";
		File baseDir = new File(folder);
		String [] inputFiles = baseDir.list();
		
		//Date start = new Date();
			    
	    IndexBasedAnnotator ann;
	    Ontology.init("/users/buscaldi/Works/collabSIG/meshonto.owl");
	    if(K_NN_ANNOTATOR) ann = new IndexBasedAnnotator("indexOHSUMed_train", labelIndex);
	    else ann = new IndexBasedAnnotator(labelIndex);

	    
		for(String filename : inputFiles){
			parseOHSUFile((folder+filename), ann);
		}
		
	    //Date end = new Date();
	    //System.out.println(end.getTime() - start.getTime() + " total milliseconds");

	}

}
