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
/**
 * Implementation of a Knowledge Battery (KB): a vector of ontologies and their related SKOS terminologies
 * @author buscaldi
 *
 */
public class KnowledgeBattery {
	private static Vector<Ontology> ontologies= new Vector<Ontology>();
	private static Vector<SKOSTerminology> terminologies= new Vector<SKOSTerminology>();
	
	/**
	 * Adds an ontology to the KB, without terminology
	 * @param o
	 */
	public static void addOntology(Ontology o){
		ontologies.add(o);
		terminologies.add(null); //NOTE: it means that no terminology is available for the ontology
	}
	/**
	 * Adds an ontology to the KB, including a terminology
	 * @param o
	 * @param st
	 */
	public static void addOntology(Ontology o, SKOSTerminology st){
		ontologies.add(o);
		terminologies.add(st);
	}
	/**
	 * Returns the number of ontologies in the KB
	 * @return
	 */
	public static int countOntologies(){
		return ontologies.size();
	}
	
	/**
	 * Returns the i-th ontology added to the KB
	 * @param i
	 * @return
	 */
	public static Ontology getOntology(int i){
		return ontologies.elementAt(i);
	}
	/**
	 * Returns the terminology corresponding to the i-th ontology in the KB
	 * @param i
	 * @return
	 */
	public static SKOSTerminology getTerminology(int i){
		return terminologies.elementAt(i);
	}
	
	/**
	 * Method used to index terminology. The index is created at the position indicated in the config.xml file
	 * This method is called only if Yasemir is set in indexing mode
	 */
	public static void createTermIndex() {
		try {
			String termIndexPath = Yasemir.TERM_DIR;
			
			Directory dir = FSDirectory.open(new File(termIndexPath));
		 	/*
		 	if(DirectoryReader.indexExists(dir)) {
		 	 
		 		System.err.println("[KnowledgeBattery] term index exists, skipping");
		 		dir.close();
		 		return;
		 	}
		 	*/
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
			System.err.println("[YaSemIR] Term Index could not be created");
			System.exit(-1);
		}		
	}
	
	/**
	 * This method attempts to return the ontology corresponding to a class ID (IRI)
	 * @param str a class ID (IRI)
	 * @return the ontology corresponding to the input class ID , or null if no ontology can be found as the source of the class ID
	 */
	public static Ontology ontoForClassID(String str) {
		for(int i=0; i < ontologies.size(); i++){
			Ontology o = ontologies.elementAt(i);
			OWLClass cls = o.classForID(str);
			if(cls != null) return o;
		}
		return null;
	}
	
	/**
	 * Returns the ontology corresponding to a given ontology ID (the unique ID calculated for the indexing process)
	 * @param str: the unique ID calculated for the indexing process
	 * @return
	 */
	public static Ontology ontoForID(String str) {
		for(int i=0; i < ontologies.size(); i++){
			Ontology o = ontologies.elementAt(i);
			if(o.getOntologyID().equals(str)) return o;
		}
		return null;
	}
	
	/**
	 * Returns the ontology corresponding to the provided scheme
	 * @param scheme
	 * @return
	 */
	public static Ontology ontoForScheme(String scheme) {
		for(int i=0; i < ontologies.size(); i++){
			Ontology o = ontologies.elementAt(i);
			if(!scheme.endsWith("#")) scheme=scheme+"#";
			if(o.getBaseAddr().equals(scheme)) return o;
		}
		return null;
	}
}
