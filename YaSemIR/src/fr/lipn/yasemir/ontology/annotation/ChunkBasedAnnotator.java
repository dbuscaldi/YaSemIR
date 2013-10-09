package fr.lipn.yasemir.ontology.annotation;

import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ca.CatalanAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.pt.PortugueseAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.semanticweb.owlapi.model.OWLClass;
import org.tartarus.snowball.ext.EnglishStemmer;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import fr.lipn.yasemir.configuration.Yasemir;
/**
 * This class uses the Stanford NLP Parser to search terminology clues in Noun Phrases
 * @author buscaldi
 *
 */
public class ChunkBasedAnnotator implements SemanticAnnotator {
	private String termIndexPath;
	private LexicalizedParser parser;
	
	private static int MAX_ANNOTS=10;

	public ChunkBasedAnnotator(String termIndexPath) {
		this.termIndexPath=termIndexPath;
		 parser = LexicalizedParser.loadModel("lib/englishPCFG.ser.gz");
	}
	
	//TODO: finire il ChunkedAnnotator
	@Override
	public HashMap<String, Vector<Annotation>> annotate(String document) {
		HashMap<String, Vector<Annotation>> ret = new HashMap<String, Vector<Annotation>>();
		
		try {
			IndexReader reader = IndexReader.open(FSDirectory.open(new File(termIndexPath)));
			IndexSearcher searcher = new IndexSearcher(reader);
			searcher.setSimilarity(new BM25Similarity());
			
			Analyzer analyzer=null;
			String lang=Yasemir.COLLECTION_LANG;
			 if(lang.equals("fr")) analyzer = new FrenchAnalyzer(Version.LUCENE_44);
			 else if(lang.equals("it")) analyzer = new ItalianAnalyzer(Version.LUCENE_44);
			 else if(lang.equals("es")) analyzer = new SpanishAnalyzer(Version.LUCENE_44);
			 else if(lang.equals("de")) analyzer = new GermanAnalyzer(Version.LUCENE_44);
			 else if(lang.equals("pt")) analyzer = new PortugueseAnalyzer(Version.LUCENE_44);
			 else if(lang.equals("ca")) analyzer = new CatalanAnalyzer(Version.LUCENE_44);
			 else if(lang.equals("nl")) analyzer = new DutchAnalyzer(Version.LUCENE_44);
			 else analyzer = new EnglishAnalyzer(Version.LUCENE_44);
			//Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_44);
			
			Reader r = new BufferedReader(new StringReader(document));
			Vector<String> fragments = new Vector<String>();
			
			for(List<HasWord> sentence : new DocumentPreprocessor(r)) {
				Tree parse = parser.apply(sentence);
				for(Tree p : parse){
					if(p.label().value().equals("NP") && p.isPrePreTerminal()) {
						//p.pennPrint();
						StringBuffer tmpstr = new StringBuffer();
						for(Tree l : p.getLeaves()){
							
							tmpstr.append(l.label().toString());
							tmpstr.append(" ");
						}
						fragments.add(tmpstr.toString().trim());
						System.err.println("[YaSemIR - CBA] Chunk found: "+tmpstr);
					}
					
				}
			}
			
			
			for(String fragment :  fragments) {
				
				if(fragment.length()==0) continue;
				//System.err.println("Annotating: "+fragment);
						
				QueryParser parser = new QueryParser(Version.LUCENE_44, "labels", analyzer);
				Query query = parser.parse(fragment);
				System.err.println("Searching for: " + query.toString("terms"));
				
				TopDocs results = searcher.search(query, 20);
			    ScoreDoc[] hits = results.scoreDocs;
			    
			    int numTotalHits = results.totalHits;
			    //System.err.println(numTotalHits + " total matching classes");
			    
			    if(numTotalHits > 0) {
				    hits = searcher.search(query, numTotalHits).scoreDocs;
				    for(int i=0; i< Math.min(numTotalHits, MAX_ANNOTS); i++){
				    	Document doc = searcher.doc(hits[i].doc);
				    	String ptrn = "(?i)("+doc.get("labels").replaceAll(", ", "|")+")";
				    	//System.err.println("OWLClass="+doc.get("id")+" score="+hits[i].score);
				    	if(checkPattern(fragment, ptrn)){
				    		//System.err.println("OK: OWLClass="+doc.get("id")+" score="+hits[i].score);
				    		Annotation ann = new Annotation(doc.get("id"));
				    		String ontoID = ann.getRelatedOntology().getOntologyID();
				    		
				    		Vector<Annotation> annotations = ret.get(ontoID);
				    		if(annotations == null) annotations = new Vector<Annotation>();
					    	annotations.add(ann);
					    	ret.put(ontoID, annotations);
				    	}
				    }
			    }
								 
			}
			
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	@Override
	public void addSemanticAnnotation(Document doc, String text) {
		HashMap<String, Vector<Annotation>> anns = this.annotate(text);
		for(String oid : anns.keySet()){
			StringBuffer av_repr = new StringBuffer();
			for(Annotation a : anns.get(oid)){
				av_repr.append(a.getOWLClass().getIRI().getFragment());
				av_repr.append(" ");
				//String ontoID=a.getRelatedOntology().getOntologyID();
			}
			//doc.add(new Field(oid+"annot", av_repr.toString().trim(), Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new TextField(oid+"annot", av_repr.toString().trim(), Field.Store.YES));
			//we add the annotation and the supertypes to an extended index to be used during the beginning of the search process
			Set<OWLClass> expansion = new HashSet<OWLClass>();
			for(Annotation a : anns.get(oid)){
				Set<OWLClass> sup_a = a.getRelatedOntology().getAllSuperClasses(a.getOWLClass());
				Set<OWLClass> roots = a.getRelatedOntology().getOntologyRoots(); //this is needed to calculate the frequencies of root classes
				roots.retainAll(sup_a);
				expansion.addAll(sup_a);
			}
			for(OWLClass c : expansion){
				av_repr.append(c.getIRI().getFragment());
				av_repr.append(" ");
			}
			//doc.add(new Field(oid+"annot_exp", av_repr.toString().trim(), Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new TextField(oid+"annot_exp", av_repr.toString().trim(), Field.Store.YES));
		}
		
	}
	
	private boolean checkPattern(String text, String pattern){
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(text.toLowerCase());
		
		if(m.find()) {
			//System.err.println("found pattern: "+m.group());
			return true;
		}
		return false;
	}

}
