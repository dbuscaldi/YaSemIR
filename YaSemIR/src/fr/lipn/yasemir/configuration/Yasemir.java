package fr.lipn.yasemir.configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;

import fr.lipn.yasemir.ontology.ConceptSimilarity;
import fr.lipn.yasemir.ontology.KnowledgeBattery;
import fr.lipn.yasemir.ontology.Ontology;
import fr.lipn.yasemir.ontology.annotation.IndexBasedAnnotator;
import fr.lipn.yasemir.ontology.annotation.KNNAnnotator;
import fr.lipn.yasemir.ontology.annotation.SemanticAnnotator;
import fr.lipn.yasemir.ontology.skos.SKOSTerminology;

public class Yasemir {
	//debug mode
	public static boolean DEBUG=true;
	
	//search mode
	public final static int CLASSIC=0;
	public final static int SEMANTIC=1;
	public final static int HYBRID=2;
	
	//Concept weights mode
	private final static int FIXED=0;
	private final static int IDF=1;
	private final static int CF=2;
	
	public final static int MAX_HITS=1000;
	public final static String BASE_ANN_FIELD="txtannot"; //name of the Lucene document field that will contain the annotation
	//exemple: txtannot_1 will indicate the annotation related to ontology 1 (to be looked up into ontoIDmap)
	
	public static int MODE=CLASSIC;
	//public static boolean USE_MANUAL_TAGS=true; //consider using manually annotated tags for IR or not
	public static int SIM_MEASURE=ConceptSimilarity.WU;
	public static String ANNOTATOR="fr.lipn.yasemir.ontology.annotation.IndexBasedAnnotator";
	public static SemanticAnnotator annotator;
	
	public static int CONCEPT_WEIGHTS=FIXED;
	public static boolean CKPD_ENABLED=false; //uses n-gram search or not
	
	public static Set<String> semBalises; //for a parsed document, tags that delimit text to be annotated and semantically indexed
	public static Set<String> clsBalises; //for a parsed document, tags that delimit text to be indexed classically
	public static String idField;
	public static boolean ID_ASATTR=false;
	public static String DOC_DELIM;
	
	public static String YASEMIR_HOME;
	public static String INDEX_DIR;
	public static String TERM_DIR;
	public static String COLLECTION_DIR;
	public static String COLLECTION_LANG;
	
	public static String SCORE;
	
	public static void init(String configFile){
		System.err.println("Reading config file...");
		ConfigurationHandler.init(configFile);
		
		//setting paths
		YASEMIR_HOME=ConfigurationHandler.YASEMIR_HOME;
		INDEX_DIR=YASEMIR_HOME+System.getProperty("file.separator")+ConfigurationHandler.INDEXDIR;
		TERM_DIR=YASEMIR_HOME+System.getProperty("file.separator")+ConfigurationHandler.TERMIDXDIR;
		//TERM_DIR=INDEX_DIR+System.getProperty("file.separator")+ConfigurationHandler.TERMIDXDIR;
		COLLECTION_DIR=ConfigurationHandler.CORPUSDIR;
		idField=ConfigurationHandler.DOCIDFIELD;
		ID_ASATTR=ConfigurationHandler.IDFIELD_ASATTR;
		DOC_DELIM=ConfigurationHandler.DOC_DELIM;
		COLLECTION_LANG=ConfigurationHandler.CORPUSLANG;
		
		//setting search mode
		String sm = ConfigurationHandler.SEARCH_MODE;
		if(sm!=null){
			if(sm.equalsIgnoreCase("semantic")) MODE=SEMANTIC;
			else if(sm.equalsIgnoreCase("hybrid")) MODE=HYBRID;
			else MODE=CLASSIC;
		}
		
		//setting concept similarity measure
		String smm = ConfigurationHandler.SIM_MEASURE;
		if(smm!=null){
			if(smm.equalsIgnoreCase("pg1")) SIM_MEASURE=ConceptSimilarity.PROXYGENEA1;
			else if(smm.equalsIgnoreCase("pg2")) SIM_MEASURE=ConceptSimilarity.PROXYGENEA2;
			else if(smm.equalsIgnoreCase("pg3")) SIM_MEASURE=ConceptSimilarity.PROXYGENEA3;
			else SIM_MEASURE=ConceptSimilarity.WU;
		}
		
		//setting concept weights
		String cw = ConfigurationHandler.CONCEPTWEIGHT;
		if(cw!=null){
			if(cw.equalsIgnoreCase("fixed")) CONCEPT_WEIGHTS=FIXED;
			else if(cw.equalsIgnoreCase("idf")) CONCEPT_WEIGHTS=IDF;
			else if(cw.equalsIgnoreCase("cf")) CONCEPT_WEIGHTS=CF;
		}
		
		//setting annotator
		ANNOTATOR=ConfigurationHandler.ANNOTENGINE;
		annotator=new IndexBasedAnnotator(TERM_DIR);
		//annotator=new KNNAnnotator(TERM_DIR); //TODO: not finished (select annotator depending on configuration file)
		
		//setting ngrams enabled or not
		CKPD_ENABLED=ConfigurationHandler.NGRAMS_ENABLED;
		
		//setting semantic fields
		semBalises=new HashSet<String>();
		semBalises.addAll(ConfigurationHandler.getSemanticFields());
		
		//setting classic fields
		clsBalises=new HashSet<String>();
		clsBalises.addAll(ConfigurationHandler.getClassicFields());
		
		//setting score type
		SCORE=ConfigurationHandler.SCORE;
		
		//setting ontologies and terminologies
		System.err.println("[YaSemIR]: Loading Knowledge Battery...");
		
		HashMap<String, String> ontoSKOSconf=ConfigurationHandler.getOntologySKOSMap();
		HashMap<String, String> ontoRootconf = ConfigurationHandler.getOntologyRootMap();		

		for(String ontoLoc : ontoSKOSconf.keySet()){
			String ontoRoot = ontoRootconf.get(ontoLoc);
			Ontology o = null;
			if(ontoRoot.trim().isEmpty()) o = new Ontology(ontoLoc);
			else o = new Ontology(ontoLoc, ontoRoot);
			System.err.println("[YaSemIR]: loaded ontology: "+o.getBaseAddr()+" at "+ontoLoc);
			String termPath=ontoSKOSconf.get(ontoLoc);
			SKOSTerminology t=null;
			if(!termPath.trim().isEmpty()) {
				System.err.println("[YaSemIR]: loading terminology from "+termPath);
				t = new SKOSTerminology(termPath);
			}
			else {
				System.err.println("[YaSemIR]: no terminology provided: generating trivial terminology...");
				t = o.generateTerminology();
			}
			System.err.println("[YaSemIR]: loaded terminology: "+t.getTerminologyID());
			KnowledgeBattery.addOntology(o, t);
			KnowledgeBattery.createTermIndex();
		}
		System.err.println("[YaSemIR]: Done.");
		
		
		
	}
	
	public static boolean isSemanticTag(String tag){
		return semBalises.contains(tag);
	}
	
	public static boolean isClassicTag(String tag){
		return clsBalises.contains(tag);
	}
	
	public static boolean isIDTag(String tag){
		return tag.equalsIgnoreCase(idField);
	}
}
