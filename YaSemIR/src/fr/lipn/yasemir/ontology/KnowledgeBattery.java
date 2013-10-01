package fr.lipn.yasemir.ontology;

import java.io.File;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.semanticweb.owlapi.model.OWLClass;

import fr.lipn.yasemir.configuration.Yasemir;
import fr.lipn.yasemir.ontology.skos.SKOSTerminology;

public class KnowledgeBattery {
	private static Vector<Ontology> ontologies= new Vector<Ontology>();
	private static Vector<SKOSTerminology> terminologies= new Vector<SKOSTerminology>();
	
	/*
	public static void init() {
		ontologies = new Vector<Ontology>();
	}
	*/
	
	public static void addOntology(Ontology o){
		ontologies.add(o);
		terminologies.add(null); //NOTE: it means that no terminology is available for the ontology
	}
	
	public static void addOntology(Ontology o, SKOSTerminology st){
		ontologies.add(o);
		terminologies.add(st);
	}
	
	public static int countOntologies(){
		return ontologies.size();
	}
	
	public static Ontology getOntology(int i){
		return ontologies.elementAt(i);
	}
	
	public static SKOSTerminology getTerminology(int i){
		return terminologies.elementAt(i);
	}

	public static void createTermIndex() {
		try {
			String termIndexPath = Yasemir.TERM_DIR;
			
			Directory dir = FSDirectory.open(new File(termIndexPath));
		 	if(DirectoryReader.indexExists(dir)) {
		 		System.err.println("[KnowledgeBattery] term index exists, skipping");
		 		dir.close();
		 		return;
		 	}
		    Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_44); //Whitespace?
		    IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_44, analyzer);
		    iwc.setSimilarity(new BM25Similarity()); //NEW! set BM25 as default similarity for term index
		    
		    IndexWriter writer = new IndexWriter(dir, iwc);
		    
		    System.err.println("[KnowledgeBattery] indexing labels to "+termIndexPath);
		    
		    for(int i=0; i < terminologies.size(); i++){
				//Ontology o = ontologies.elementAt(i);
				SKOSTerminology t = terminologies.elementAt(i);
				
				t.resetIterator();
				while(t.hasMoreLabels()){
					Document doc=new Document();
					Vector<String> items = t.getNextLabels();
					String classIRI =items.elementAt(0);
	                String labels =items.elementAt(1);
	                
	                //System.err.println("[KnowledgeBattery] indexing "+classIRI+" labels");
	                doc.add(new Field("id", classIRI, Field.Store.YES, Field.Index.NOT_ANALYZED));
	                if(!t.isStemmed()) {
	                	doc.add(new Field("labels", labels, Field.Store.YES, Field.Index.ANALYZED));
	                } else {
	                	doc.add(new Field("labels", labels, Field.Store.YES, Field.Index.NOT_ANALYZED));
	                }
	                writer.addDocument(doc);
				}
			}
		    writer.close();
		
		} catch(Exception e){
			e.printStackTrace();
			//TODO:take action
		}		
	}
	/*
	public static OWLClass classForID(String str) {
		for(int i=0; i < ontologies.size(); i++){
			Ontology o = ontologies.elementAt(i);
			OWLClass cls = o.classForID(str);
			if(cls != null) return cls;
		}
		return null;
	}
	*/
	
	public static Ontology ontoForClassID(String str) {
		for(int i=0; i < ontologies.size(); i++){
			Ontology o = ontologies.elementAt(i);
			OWLClass cls = o.classForID(str);
			if(cls != null) return o;
		}
		return null;
	}
	
	public static Ontology ontoForID(String str) {
		for(int i=0; i < ontologies.size(); i++){
			Ontology o = ontologies.elementAt(i);
			if(o.getOntologyID().equals(str)) return o;
		}
		return null;
	}
}
