package fr.lipn.yasemir.search.ohsumed;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ca.CatalanAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.pt.PortugueseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import fr.lipn.yasemir.configuration.Yasemir;
import fr.lipn.yasemir.ontology.annotation.Annotation;
import fr.lipn.yasemir.search.RankedDocument;
import fr.lipn.yasemir.weighting.ckpd.NGramTerm;
import fr.lipn.yasemir.weighting.ckpd.TermFactory;
import fr.lipn.yasemir.weighting.ckpd.ranking.CKPDRankedDocument;

public class YasemirOHSUSearcher {
	private final static int TITLE_ONLY=0;
	private final static int TITLE_DESC=1;
	
	private final static int MAX_HITS=1000;
	
	private static int CONFIGURATION=TITLE_DESC;
	//private static int SIM_MEASURE=ConceptSimilarity.PROXYGENEA2;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			//File queryFile = new File("/home/dbuscaldi/Ubuntu One/Works/collabSIG/query.ohsu.1-63ext.xml");
			//File queryFile = new File("/users/buscaldi/Works/collabSIG/queries-updated.xml");
			
			File queryFile = new File(args[1]);
			
			XMLQueryHandler hdlr = new XMLQueryHandler(queryFile);
			Vector<OHSUQuery> queries = hdlr.getParsedQueries();
			
			Yasemir.init("config.xml");
			
		    String index = Yasemir.INDEX_DIR;
			String lang = Yasemir.COLLECTION_LANG;
			
			String basefield = "text";
			
			IndexReader reader = IndexReader.open(FSDirectory.open(new File(index)));
			IndexSearcher searcher = new IndexSearcher(reader);
			
			Analyzer analyzer = null;
		    if(lang.equals("fr")) analyzer = new FrenchAnalyzer(Version.LUCENE_44);
		    else if(lang.equals("it")) analyzer = new ItalianAnalyzer(Version.LUCENE_44);
		    else if(lang.equals("es")) analyzer = new SpanishAnalyzer(Version.LUCENE_44);
		    else if(lang.equals("de")) analyzer = new GermanAnalyzer(Version.LUCENE_44);
		    else if(lang.equals("pt")) analyzer = new PortugueseAnalyzer(Version.LUCENE_44);
		    else if(lang.equals("ca")) analyzer = new CatalanAnalyzer(Version.LUCENE_44);
		    else if(lang.equals("nl")) analyzer = new DutchAnalyzer(Version.LUCENE_44);
		    else analyzer = new EnglishAnalyzer(Version.LUCENE_44);
			
			String conf_str="run_";
			
			QueryParser parser = new QueryParser(Version.LUCENE_44, basefield, analyzer);
			for(OHSUQuery oq : queries){
				StringBuffer query = new StringBuffer();
				query.append(oq.getTitle());
				if(CONFIGURATION==TITLE_DESC) {
					query.append(" ");
					query.append(oq.getDescription());	
				}
				
				if(Yasemir.MODE==Yasemir.CLASSIC){
					conf_str+="n"; if(CONFIGURATION==TITLE_DESC) conf_str+="td";
					if(Yasemir.CKPD_ENABLED) conf_str+="ckpd"; TermFactory.init(reader, analyzer);

						Query parsedQuery = parser.parse(query.toString().trim());
						
						System.out.println("[YaSemIr - CLASSIC] Searching for: " + parsedQuery.toString(basefield));
						//System.err.println("Searching for: " + parsedQuery.toString(field));
						TopDocs results = searcher.search(parsedQuery, MAX_HITS);
						ScoreDoc[] hits = results.scoreDocs;
					    
					    int numTotalHits = results.totalHits;
					    //System.err.println(numTotalHits + " total matching documents");
					    if(numTotalHits > 0) {
					    	if(!Yasemir.CKPD_ENABLED){
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
		    	} else {
		    		HashMap<String, Vector<Annotation>> queryAnnotation = null;
					System.err.println("[YaSemIr] Annotating: " + query.toString());
					queryAnnotation=Yasemir.annotator.annotate(query.toString().trim());
					  
					System.err.println("[YaSemIr] Annotations:");
					for(String oid : queryAnnotation.keySet()) {
						Vector<Annotation> ann = queryAnnotation.get(oid);
						for(Annotation a : ann){
							System.err.println(a.getOWLClass().getIRI());
					    }
					}
					
					if(Yasemir.MODE==Yasemir.SEMANTIC){
						StringBuffer extQueryText = new StringBuffer();
						
						for(String oid : queryAnnotation.keySet()) {
				    	  Vector<Annotation> ann = queryAnnotation.get(oid);
				    	  for(Annotation a : ann){
				    		  extQueryText.append(oid+"annot_exp:\""+a.getOWLClass().getIRI().getFragment()+"\"");
				    		  extQueryText.append(" ");
					      }
						}
			    	 
						Query parsedQuery=parser.parse(extQueryText.toString().trim());
						
						System.err.println("[YaSemIr - SEMANTIC] Searching for: " + parsedQuery.toString());
			    	  
				    	  
				    	TopDocs results = searcher.search(parsedQuery, MAX_HITS);
						ScoreDoc[] hits = results.scoreDocs;
						    
					    int numTotalHits = results.totalHits;
					    //System.err.println("[YaSemIr] "+numTotalHits + " total matching documents");
					    if(numTotalHits > 0) {
				    	  Vector<RankedDocument> srDocs= new Vector<RankedDocument>();
				    	  for (int i = 0; i < Math.min(numTotalHits, MAX_HITS); i++) {
					          Document doc = searcher.doc(hits[i].doc);
					          String id = doc.get("id");
					          String textAbst = doc.get("text");
					          //textAbst=textAbst.substring(0, Math.min(1024, textAbst.length()-1));
					          List<IndexableField> docFields =doc.getFields();
					          StringBuffer concepts = new StringBuffer();
					          for(IndexableField f : docFields){
					        	  String fname=f.name();
					        	  if(fname.endsWith("annot") || fname.endsWith("annot_exp")) {
					        		  concepts.append(fname+":"+doc.get(fname));
					        		  concepts.append(" ");
					        	  }
					          }
					        
					        RankedDocument srDoc = new RankedDocument(id, textAbst, concepts.toString().trim(), queryAnnotation);
					        srDoc.setWeight(Yasemir.SIM_MEASURE);
					        srDocs.add(srDoc);
					        
					    }
				    	
				    	Collections.sort(srDocs);
				    	for (int i = 0; i < Math.min(srDocs.size(), MAX_HITS); i++) {
				    		RankedDocument srd = srDocs.elementAt(i);
				    		System.out.println(oq.getID()+"\tQ0\t"+srd.getID()+"\t"+i+"\t"+String.format(Locale.US, "%.4f",srd.getScore())+"\t"+conf_str);
						}	
				}
			} else {
				//HYBRID MODE (search classic and rank with concepts score * tfidf score)
				conf_str+="h"; if(CONFIGURATION==TITLE_DESC) conf_str+="td";
				analyzer =  new EnglishAnalyzer(Version.LUCENE_44);
				if(Yasemir.CKPD_ENABLED) conf_str+="ckpd"; TermFactory.init(reader, analyzer);
				//System.err.println("Processing Query "+oq.getID());
				StringBuffer extQueryText = new StringBuffer();
				
				for(String oid : queryAnnotation.keySet()) {
		    	  Vector<Annotation> ann = queryAnnotation.get(oid);
		    	  for(Annotation a : ann){
		    		  extQueryText.append(oid+"annot_exp:\""+a.getOWLClass().getIRI().getFragment()+"\"");
		    		  extQueryText.append(" ");
			      }
				}
	
				Query parsedQuery = parser.parse(query.toString().trim());
					
				//System.err.println("Searching for: " + parsedQuery.toString(field));
				TopDocs results = searcher.search(parsedQuery, MAX_HITS*2);
				ScoreDoc[] hits = results.scoreDocs;
				    
				int numTotalHits = results.totalHits;
				//System.err.println(numTotalHits + " total matching documents");
				if(numTotalHits > 0) {
					Vector<NGramTerm> queryNGT = null;
				    if(Yasemir.CKPD_ENABLED) {
				    	queryNGT = TermFactory.makeTermSequence(query.toString());
				    }
				    Vector<RankedDocument> srDocs= new Vector<RankedDocument>();
				    for (int i = 0; i < Math.min(numTotalHits, MAX_HITS*2); i++) {
				    	Document doc = searcher.doc(hits[i].doc);
				        String id = doc.get("id");
				        String textAbst = doc.get("text");
				        List<IndexableField> docFields =doc.getFields();
				        StringBuffer concepts = new StringBuffer();
				        for(IndexableField f : docFields){
				        	String fname=f.name();
				        	if(fname.endsWith("annot") || fname.endsWith("annot_exp")) {
				        		concepts.append(fname+":"+doc.get(fname));
				        		concepts.append(" ");
				        	}
				        }
				        
				        //if(concepts==null) continue;
				        RankedDocument srDoc = new RankedDocument(id, textAbst, concepts.toString().trim(), queryAnnotation);
				        srDoc.setWeight(Yasemir.SIM_MEASURE);
				        srDoc.includeClassicWeight(hits[i].score); //model with both classic and CKPD weights
				        srDocs.add(srDoc);
				        if(Yasemir.CKPD_ENABLED) {
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
				    for(RankedDocument srd : srDocs){
				    	System.out.println(oq.getID()+"\tQ0\t"+srd.getID()+"\t"+rank+"\t"+String.format(Locale.US, "%.4f",srd.getScore())+"\t"+conf_str);
				    	rank++;
				    	if(rank==(MAX_HITS-1)) break;
				    }
				 }
				    //break; //uncomment this for test only the first query
				}
		    }
		}
			
		    reader.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
