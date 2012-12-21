package fr.lipn.yasemir.configuration;

import java.util.HashMap;
import java.util.Set;

import fr.lipn.yasemir.ontology.ConceptSimilarity;

public class Yasemir {
	//search mode
	private final static int CLASSIC=0;
	private final static int SEMANTIC=1;
	private final static int HYBRID=2;
	
	//annotation mode
	private final static int K_NN=0; //K_NN annotation mode (needs an annotated index)
	private final static int TRI_ANN=1; //trivial annotation mode
	
	//Concept weights mode
	private final static int FIXED=0;
	private final static int IDF=1;
	private final static int CF=2;
	
	public final static boolean CKPD_ENABLED=false; //uses n-gram search or not
	
	public final static int MAX_HITS=1000;
	
	public static int MODE=HYBRID;
	public static boolean USE_MANUAL_TAGS=true; //consider using manually annotated tags for IR or not
	public static int SIM_MEASURE=ConceptSimilarity.PROXYGENEA2;
	public static int ANNOTATION_MODE=TRI_ANN;
	public static int CONCEPT_WEIGHTS=FIXED;
	
	public static HashMap<String, String> ontoSKOSmap; //maps ontologies into the related SKOS files
	public static HashMap<String, Integer> ontoIDmap; //maps ontologies into a numeric ID (used to indicate which annotation pertains to which ontology
	
	public static Set<String> semBalises; //for a parsed document, tags that delimit text to be annotated
	
	public static String BASE_ANN_FIELD="txtannot"; //name of the Lucene document field that will contain the annotation
	//exemple: txtannot_1 will indicate the annotation related to ontology 1 (to be looked up into ontoIDmap)
	public static void init(){
		//TODO: read .xml configuration file and setup all info
	}
}
