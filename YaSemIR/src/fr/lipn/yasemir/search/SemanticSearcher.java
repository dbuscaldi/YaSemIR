package fr.lipn.yasemir.search;

import java.io.File;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.semanticweb.owlapi.model.OWLClass;

import fr.lipn.yasemir.ontology.ConceptSimilarity;
import fr.lipn.yasemir.ontology.Ontology;
import fr.lipn.yasemir.ontology.annotation.Annotation;
import fr.lipn.yasemir.ontology.annotation.IndexBasedAnnotator;
import fr.lipn.yasemir.weighting.ckpd.NGramTerm;
import fr.lipn.yasemir.weighting.ckpd.TermFactory;
import fr.lipn.yasemir.weighting.ckpd.ranking.CKPDRankedDocument;

public class SemanticSearcher {
	private final static int TITLE_ONLY=0;
	private final static int TITLE_DESC=1;
	private final static int CLASSIC=0;
	private final static int SEMANTIC=1;
	private final static int HYBRID=2;
	private final static boolean CKPD_ENABLED=false;
	
	private final static int MAX_HITS=1000;
	
	private static int MODE=SEMANTIC;
	private static boolean USE_TAGS=true; //consider using manually annotated tags for IR or not
	private static int CONFIGURATION=TITLE_DESC;
	private static int SIM_MEASURE=ConceptSimilarity.PROXYGENEA2;
	
	//private static String ontologyLocation = "/home/dbuscaldi/Ubuntu One/Works/collabSIG/meshonto.owl";
	
	private static String ontologyLocation = "/users/buscaldi/Works/collabSIG/meshonto.owl";
	
	private static String simCode(){
		switch (SIM_MEASURE) {
		case ConceptSimilarity.PROXYGENEA1: return "pg1";
		case ConceptSimilarity.PROXYGENEA2: return "pg2";
		case ConceptSimilarity.PROXYGENEA3: return "pg3";
		default: return "wu";
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			//File queryFile = new File("/home/dbuscaldi/Ubuntu One/Works/collabSIG/query.ohsu.1-63ext.xml");
			File queryFile = new File("/users/buscaldi/Works/collabSIG/queries-updated.xml");
			XMLQueryHandler hdlr = new XMLQueryHandler(queryFile);
			Vector<OHSUQuery> queries = hdlr.getParsedQueries();
			
			String index = "indexOHSUMed_sem";
			String labelIndex = "termIndex_0.6";
			//String index = "/tempo/indexOHSUMed_sem_knn";
			String field = "title";
			if(USE_TAGS) field = "tag";
			else field = "title";
			
			IndexReader reader = IndexReader.open(FSDirectory.open(new File(index)));
			IndexSearcher searcher = new IndexSearcher(reader);
			Analyzer analyzer;
			
			String conf_str="run_";
			
			if(MODE==SEMANTIC){
				Ontology.init(ontologyLocation);
				ClassWeightHandler.init();
				
				conf_str+="s"+simCode(); if(CONFIGURATION==TITLE_DESC) conf_str+="td";
				IndexBasedAnnotator sa = new IndexBasedAnnotator(labelIndex);
				analyzer =  new SimpleAnalyzer(Version.LUCENE_31);
				
				for(OHSUQuery oq : queries){
					//System.err.println("Processing Query "+oq.getID());
					StringBuffer query = new StringBuffer();
					Vector<Annotation> ann = new Vector<Annotation>();
					if(USE_TAGS) ann.addAll(sa.extractCategories(oq.getCategoryList()));
					else {
						ann.addAll(sa.annotate(oq.getTitle()));
						if(CONFIGURATION==TITLE_DESC) ann.addAll(sa.annotate(oq.getDescription()));	
					}
					
					for(Annotation a : ann){
						//Set<OWLClass> sup_a = Ontology.getAllSuperClasses(a.getOWLClass());
						query.append(a.getOWLClass().getIRI().getFragment());
						query.append(" ");
						/*for(OWLClass ac : sup_a){
							query.append(ac.getIRI().getFragment());
							query.append(" ");
						}*/
					}
					/*
					System.err.println(oq.getTitle());
					System.err.println(oq.getDescription());
					System.err.println(ann);
					*/
					
					QueryParser parser = new QueryParser(Version.LUCENE_31, field+"annot_exp", analyzer);
					if(!query.toString().isEmpty()){
						Query parsedQuery = parser.parse(query.toString().trim());
						
						System.err.println("Searching for: " + parsedQuery.toString(field));
						TopDocs results = searcher.search(parsedQuery, MAX_HITS);
						ScoreDoc[] hits = results.scoreDocs;
					    
					    int numTotalHits = results.totalHits;
					    System.err.println(numTotalHits + " total matching documents");
					    //FIXME:QUERY10???
					    if(numTotalHits > 0) {
					    	Vector<SemanticallyRankedDocument> srDocs= new Vector<SemanticallyRankedDocument>();
					    	for (int i = 0; i < Math.min(numTotalHits, MAX_HITS); i++) {
						        Document doc = searcher.doc(hits[i].doc);
						        String id = doc.get("id");
						        String concepts = doc.get(field+"annot"); //here we have the annotation
						        //System.err.println(concepts);
						        SemanticallyRankedDocument srDoc = new SemanticallyRankedDocument(id, concepts, ann);
						        srDoc.setWeight(SIM_MEASURE);
						        srDocs.add(srDoc);
						        //System.out.println(oq.getID()+"\tQ0\t"+id+"\t"+i+"\t"+hits[i].score+"\t"+conf_str);
						    }
					    	
					    	Collections.sort(srDocs);
					    	int rank=0;
					    	for(SemanticallyRankedDocument srd : srDocs){
					    		System.out.println(oq.getID()+"\tQ0\t"+srd.getID()+"\t"+rank+"\t"+String.format(Locale.US, "%.4f",srd.getScore())+"\t"+conf_str);
					    		rank++;
					    	}
					    }
						//break; //uncomment this for test only the first query
					}
				}
				
				
				
			}
			else if(MODE==CLASSIC){ //ignores USE_TAGS
				conf_str+="n"; if(CONFIGURATION==TITLE_DESC) conf_str+="td";
				analyzer =  new EnglishAnalyzer(Version.LUCENE_31);
				if(CKPD_ENABLED) conf_str+="ckpd"; TermFactory.init(searcher, analyzer);
				for(OHSUQuery oq : queries){
					//System.err.println("Processing Query "+oq.getID());
					StringBuffer query = new StringBuffer();
					query.append(oq.getTitle());
					if(CONFIGURATION==TITLE_DESC) {
						query.append(" ");
						query.append(oq.getDescription());	
					}
					
					QueryParser parser = new QueryParser(Version.LUCENE_31, "title", analyzer);
					Query parsedQuery = parser.parse(query.toString().trim());
					
					//System.err.println("Searching for: " + parsedQuery.toString(field));
					TopDocs results = searcher.search(parsedQuery, MAX_HITS);
					ScoreDoc[] hits = results.scoreDocs;
				    
				    int numTotalHits = results.totalHits;
				    //System.err.println(numTotalHits + " total matching documents");
				    if(numTotalHits > 0) {
				    	if(!CKPD_ENABLED){
					    	for (int i = 0; i < Math.min(numTotalHits, MAX_HITS); i++) {
						        Document doc = searcher.doc(hits[i].doc);
						        String id = doc.get("id");
						        System.out.println(oq.getID()+"\tQ0\t"+id+"\t"+i+"\t"+String.format(Locale.US, "%.4f",hits[i].score)+"\t"+conf_str);
						    }
				    	} else {
				    		Vector<CKPDRankedDocument> ckpdDocs= new Vector<CKPDRankedDocument>();
				    		Vector<NGramTerm> queryNGT = TermFactory.makeTermSequence(query.toString());
				    		for (int i = 0; i < Math.min(numTotalHits, MAX_HITS*2); i++) {
						        Document doc = searcher.doc(hits[i].doc);
						        String id = doc.get("id");
						        String text = doc.get("title") + " "+doc.get("text");
						        CKPDRankedDocument cd = new CKPDRankedDocument(id, text, queryNGT);
						        ckpdDocs.add(cd);
				    		}
				    		Collections.sort(ckpdDocs);
				    		int rank=0;
					    	for(CKPDRankedDocument crd : ckpdDocs){
					    		System.out.println(oq.getID()+"\tQ0\t"+crd.getID()+"\t"+rank+"\t"+String.format(Locale.US, "%.4f",crd.getScore())+"\t"+conf_str);
					    		rank++;
					    		if(rank==(MAX_HITS-1)) break;
					    	}
				    	}
				    }
				    //break; //uncomment this for test only the first query
				}
				
			} else {
				//HYBRID MODE (search classic and rank with concepts score * tfidf score)
				Ontology.init(ontologyLocation);
				ClassWeightHandler.init();
				IndexBasedAnnotator sa = new IndexBasedAnnotator(labelIndex);
				
				conf_str+="h"; if(CONFIGURATION==TITLE_DESC) conf_str+="td";
				analyzer =  new EnglishAnalyzer(Version.LUCENE_31);
				if(CKPD_ENABLED) conf_str+="ckpd"; TermFactory.init(searcher, analyzer);
				for(OHSUQuery oq : queries){
					//System.err.println("Processing Query "+oq.getID());
					StringBuffer query = new StringBuffer();
					
					query.append(oq.getTitle());
					if(CONFIGURATION==TITLE_DESC) {
						query.append(" ");
						query.append(oq.getDescription());	
					}
					
					Vector<Annotation> ann = new Vector<Annotation>();
					if(USE_TAGS) {
						ann.addAll(sa.extractCategories(oq.getCategoryList()));
					} else {
						ann.addAll(sa.annotate(oq.getTitle()));
						if(CONFIGURATION==TITLE_DESC) ann.addAll(sa.annotate(oq.getDescription()));
					}
					for(Annotation a : ann){
						query.append(" ");
						query.append(field+"annot_exp:"+a.getOWLClass().getIRI().getFragment());
						//query.append("^0.25");
					}
					
					QueryParser parser = new QueryParser(Version.LUCENE_31, "title", analyzer); //since it's hybrid it looks always into the standard index
					Query parsedQuery = parser.parse(query.toString().trim());
					
					//System.err.println("Searching for: " + parsedQuery.toString(field));
					TopDocs results = searcher.search(parsedQuery, MAX_HITS*2);
					ScoreDoc[] hits = results.scoreDocs;
				    
				    int numTotalHits = results.totalHits;
				    //System.err.println(numTotalHits + " total matching documents");
				    if(numTotalHits > 0) {
				    	Vector<NGramTerm> queryNGT = null;
				    	if(CKPD_ENABLED) {
				    		queryNGT = TermFactory.makeTermSequence(query.toString());
				    	}
				    	Vector<SemanticallyRankedDocument> srDocs= new Vector<SemanticallyRankedDocument>();
				    	for (int i = 0; i < Math.min(numTotalHits, MAX_HITS*2); i++) {
					        Document doc = searcher.doc(hits[i].doc);
					        String id = doc.get("id");
					        String concepts = doc.get(field+"annot");
					        if(concepts==null) continue;
					        SemanticallyRankedDocument srDoc = new SemanticallyRankedDocument(id, concepts, ann);
					        srDoc.setWeight(SIM_MEASURE);
					        srDoc.includeClassicWeight(hits[i].score); //model with both classic and CKPD weights
					        if(CKPD_ENABLED) {
					        	String text = doc.get("title") + " "+doc.get("text");
					        	CKPDRankedDocument crDoc = new CKPDRankedDocument(id, text, queryNGT);
					        	srDoc.includeCKPDWeight(crDoc.getScore());
					        } /*else {
					        	srDoc.includeClassicWeight(hits[i].score);
					        	//we could add both ClassicWeight and CKPD weight also => another model???
					        }*/
					        srDocs.add(srDoc);
					        //System.out.println(oq.getID()+"\tQ0\t"+id+"\t"+i+"\t"+hits[i].score+"\t"+conf_str);
					    }
				    	
				    	Collections.sort(srDocs);
				    	int rank=0;
				    	for(SemanticallyRankedDocument srd : srDocs){
				    		System.out.println(oq.getID()+"\tQ0\t"+srd.getID()+"\t"+rank+"\t"+String.format(Locale.US, "%.4f",srd.getScore())+"\t"+conf_str);
				    		rank++;
				    		if(rank==(MAX_HITS-1)) break;
				    	}
				    }
				    //break; //uncomment this for test only the first query
				}
			}
			
			searcher.close();
		    reader.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
