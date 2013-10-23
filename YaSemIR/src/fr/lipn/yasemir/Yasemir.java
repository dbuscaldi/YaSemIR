package fr.lipn.yasemir;
/*
 * Copyright (C) 2013, Université Paris Nord
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
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ca.CatalanAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.pt.PortugueseAnalyzer;
import org.apache.lucene.util.Version;

import fr.lipn.yasemir.configuration.ConfigurationHandler;
import fr.lipn.yasemir.ontology.ClassWeightHandler;
import fr.lipn.yasemir.ontology.ConceptSimilarity;
import fr.lipn.yasemir.ontology.KnowledgeBattery;
import fr.lipn.yasemir.ontology.Ontology;
import fr.lipn.yasemir.ontology.annotation.SentenceBasedAnnotator;
import fr.lipn.yasemir.ontology.annotation.KNNAnnotator;
import fr.lipn.yasemir.ontology.annotation.SemanticAnnotator;
import fr.lipn.yasemir.ontology.skos.SKOSTerminology;
/**
 * This class provides all the parameters required by the modules
 * @author buscaldi
 *
 */
public class Yasemir {
	//debug mode
	public static boolean DEBUG=true;
	
	//search mode
	public final static int CLASSIC=0;
	public final static int SEMANTIC=1;
	public final static int HYBRID=2;
	
	public final static int MAX_HITS=1000;
	
	public static int MODE=CLASSIC;
	public static int SIM_MEASURE=ConceptSimilarity.WU;
	public static String ANNOTATOR="fr.lipn.yasemir.ontology.annotation.IndexBasedAnnotator";
	public static SemanticAnnotator annotator;
	
	public static int CONCEPT_WEIGHTS=ClassWeightHandler.FIXED; //by default, all concepts weigh the same 
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

	private static boolean INDEXING_MODE=false;
	
	//common Analyzer
	public static Analyzer analyzer;
	
	/**
	 * Initialisation method to be called before every action
	 * @param configFile
	 */
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
		
		if(COLLECTION_LANG.equals("fr")) analyzer = new FrenchAnalyzer(Version.LUCENE_44);
	    else if(COLLECTION_LANG.equals("it")) analyzer = new ItalianAnalyzer(Version.LUCENE_44);
	    else if(COLLECTION_LANG.equals("es")) analyzer = new SpanishAnalyzer(Version.LUCENE_44);
	    else if(COLLECTION_LANG.equals("de")) analyzer = new GermanAnalyzer(Version.LUCENE_44);
	    else if(COLLECTION_LANG.equals("pt")) analyzer = new PortugueseAnalyzer(Version.LUCENE_44);
	    else if(COLLECTION_LANG.equals("ca")) analyzer = new CatalanAnalyzer(Version.LUCENE_44);
	    else if(COLLECTION_LANG.equals("nl")) analyzer = new DutchAnalyzer(Version.LUCENE_44);
	    else analyzer = new EnglishAnalyzer(Version.LUCENE_44);
		
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
			if(cw.equalsIgnoreCase("fixed")) CONCEPT_WEIGHTS=ClassWeightHandler.FIXED;
			else if(cw.equalsIgnoreCase("idf")) CONCEPT_WEIGHTS=ClassWeightHandler.IDF;
			else if(cw.equalsIgnoreCase("prob")) CONCEPT_WEIGHTS=ClassWeightHandler.PROB;
			else if(cw.equalsIgnoreCase("gauss")) CONCEPT_WEIGHTS=ClassWeightHandler.GAUSSPROB;
		}
		
		//setting annotator
		ANNOTATOR=ConfigurationHandler.ANNOTENGINE;
		annotator=new SentenceBasedAnnotator(TERM_DIR);
		//annotator=new KNNAnnotator(TERM_DIR); //TODO: not finished (select annotator depending on configuration file)
		try{
			Class<?> cls = Class.forName(ANNOTATOR);
			Constructor<?> constructor = cls.getConstructor(String.class);
			annotator = (SemanticAnnotator) constructor.newInstance(TERM_DIR);
			//Object instance = constructor.newInstance("stringparam");
		} catch (Exception e){
			e.printStackTrace();
			System.err.println("[YaSemIR]: failed to load the specified annotator, falling back to IndexBasedAnnotator");
			annotator=annotator=new SentenceBasedAnnotator(TERM_DIR);
		}
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
				t = new SKOSTerminology(o.getOntologyID(), termPath);
			}
			else {
				System.err.println("[YaSemIR]: no terminology provided: generating trivial terminology from "+o.getBaseAddr()+"...");
				t = o.generateTerminology();
			}
			System.err.println("[YaSemIR]: loaded terminology: "+t.getTerminologyID());
			KnowledgeBattery.addOntology(o, t);
			
		}
		if(INDEXING_MODE) KnowledgeBattery.createTermIndex();
		System.err.println("[YaSemIR]: Done.");
		
		
		
	}
	/**
	 * Tells whether the content of the documents tagged by the argument XML tag is processed by the semantic annotator or not
	 * @param tag
	 * @return
	 */
	public static boolean isSemanticTag(String tag){
		return semBalises.contains(tag);
	}
	/**
	 * Tells whether the content of the documents tagged by the argument XML tag is indexed or not
	 * @param tag
	 * @return
	 */
	public static boolean isClassicTag(String tag){
		return clsBalises.contains(tag);
	}
	/**
	 * Tells whether the argument XML tag represents an ID tag or not
	 * @param tag
	 * @return
	 */
	public static boolean isIDTag(String tag){
		return tag.equalsIgnoreCase(idField);
	}
	
	/**
	 * This method specifies if indexing mode should be enabled or not.
	 * Indexing mode creates the index and the terminology, while default mode (search) only reads the index and the terminology
	 * @param b
	 */
	public static void setIndexing(boolean b) {
		INDEXING_MODE=b;
	}
}
