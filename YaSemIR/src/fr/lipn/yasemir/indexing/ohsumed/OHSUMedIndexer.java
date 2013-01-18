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
import fr.lipn.yasemir.ontology.annotation.KNNAnnotator;
import fr.lipn.yasemir.ontology.annotation.SemanticAnnotator;

public class OHSUMedIndexer {
	static boolean SEMANTIC_INDEXING=true;
	static boolean K_NN_ANNOTATOR=false;
	static public boolean VERBOSE=false;
	static boolean EXPAND_TAGS=true;
	
	private static void indexOHSUFile(String filename, IndexWriter writer, SemanticAnnotator ann){
		try {
			
			BufferedReader reader;
			if(filename.endsWith("gz")){
				reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename))));
			} else reader = new BufferedReader(new FileReader(filename));
		    
			String line;
		    Document currDoc=new Document();
		    while ((line = reader.readLine()) != null)
		    {
		      if(line.startsWith(".I")) {
		    	  System.err.println(line);
		    	  writer.addDocument(currDoc);
		    	  currDoc=new Document();
		      }
		      if(line.startsWith(".U")){
		    	  String id = reader.readLine();
		    	  currDoc.add(new Field("id", id, Field.Store.YES, Field.Index.NOT_ANALYZED));
		      }
		      if(line.startsWith(".S")){
		    	  String journal = reader.readLine();
		    	  currDoc.add(new Field("source", journal, Field.Store.YES, Field.Index.NOT_ANALYZED));
		      }
		      if(line.startsWith(".M")){
		    	  String tags = reader.readLine();
		    	  tags=tags.replaceAll("/(\\*)?.*?(;|\\.)", ";");
		    	  currDoc.add(new Field("tag", tags, Field.Store.YES, Field.Index.ANALYZED));
		    	  if(VERBOSE) System.err.println(tags);
		    	  if(EXPAND_TAGS){
		    		  ann.addSemanticAnnotation(currDoc, tags, "tag");
		    	  }
		      }
		      if(line.startsWith(".T")){
		    	  String abst = reader.readLine();
		    	  currDoc.add(new Field("title", abst, Field.Store.YES, Field.Index.ANALYZED));
		    	  if(SEMANTIC_INDEXING){
		    		  ann.addSemanticAnnotation(currDoc, abst, "title");
		    	  }
		      }
		      if(line.startsWith(".P")){
		    	  String docType = reader.readLine();
		    	  currDoc.add(new Field("type", docType, Field.Store.YES, Field.Index.ANALYZED));
		      }
		      if(line.startsWith(".W")){
		    	  String text = reader.readLine();
		    	  currDoc.add(new Field("text", text, Field.Store.YES, Field.Index.ANALYZED));
		    	  /*if(SEMANTIC_INDEXING){
		    		  ann.addSemanticAnnotation(currDoc, text, "text");
		    	  }*/
		      }
		      if(line.startsWith(".A")){
		    	  String authors = reader.readLine();
		    	  currDoc.add(new Field("author", authors, Field.Store.YES, Field.Index.ANALYZED));
		      }
		    }
		    reader.close();
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
		File baseDir = new File(folder);
		String [] inputFiles = baseDir.list();
		String indexPath = "indexOHSUMed";
		if(SEMANTIC_INDEXING) indexPath = "indexOHSUMed_sem_trivi";
		if(SEMANTIC_INDEXING && K_NN_ANNOTATOR) indexPath = "/tempo/indexOHSUMed_sem_knn";
		String termLabelIndex= "termIndex_trivial";
		
		Date start = new Date();
		
		ClassFrequencyCollector.init();
		
		Directory dir = FSDirectory.open(new File(indexPath));
		
		Map<String, Analyzer> analyzerPerField = new HashMap<String, Analyzer>();
		analyzerPerField.put("titleannot", new SimpleAnalyzer(Version.LUCENE_31));
		analyzerPerField.put("titleannot_exp", new SimpleAnalyzer(Version.LUCENE_31));
		//note: if we convert the tags to the BiKE format, then we should use also SimpleAnalyzer in such a case
		
	    PerFieldAnalyzerWrapper analyzer =
	    	      new PerFieldAnalyzerWrapper(new EnglishAnalyzer(Version.LUCENE_31), analyzerPerField);
	    
	    IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_31, analyzer);

	    IndexWriter writer = new IndexWriter(dir, iwc);
	    
	    SemanticAnnotator ann;
	    if(SEMANTIC_INDEXING){
	    	//Ontology.init("/users/buscaldi/Dropbox/Work/collabSIG/meshonto.owl");
	    	//Ontology.init("/home/dbuscaldi/Ubuntu One/Works/collabSIG/meshonto.owl");
	    	Ontology.init("/users/buscaldi/Works/collabSIG/meshonto.owl");
	    	if(K_NN_ANNOTATOR) ann = new KNNAnnotator("indexOHSUMed_train", termLabelIndex);
	    	else ann = new IndexBasedAnnotator(termLabelIndex);
	    } else {
	    	ann=null;
	    }

	    
		for(String filename : inputFiles){
			System.err.println("indexing "+filename);
			indexOHSUFile((folder+filename), writer, ann);
		}
		
		
		writer.close();
		
		ClassFrequencyCollector.dump("freqs.dat");
		
	    Date end = new Date();
	    System.out.println(end.getTime() - start.getTime() + " total milliseconds");

	}

}
