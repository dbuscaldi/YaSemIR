package fr.lipn.yasemir.ontology.annotation;

import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.EnglishStemmer;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
/**
 * This class uses the Stanford NLP Parser to search terminology clues in Noun Phrases
 * @author buscaldi
 *
 */
public class ChunkBasedAnnotator implements SemanticAnnotator {
	private String termIndexPath;
	private LexicalizedParser parser;

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
			
			//Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_44);
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_44);
			
			Reader r = new BufferedReader(new StringReader(document));
			for(List<HasWord> sentence : new DocumentPreprocessor(r)) {
				Tree parse = parser.apply(sentence);
				parse.pennPrint();
				for(Tree p : parse.children()) {
					System.err.println(p.label().value());
				}
			}
			/*
			TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
			List<CoreLabel> rawWords2 = tokenizerFactory.getTokenizer(new StringReader(document)).tokenize();
			parse = parser.apply(rawWords2);
		    TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
		    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
			*/    
			
			/****** code from IndexBasedAnnotator
			document=document.replaceAll("Support, .+?;", "");
			document=document.replaceAll("\\[.*?\\]", "").trim();
			String [] fragments = document.split("[;:\\.,]");
			*/
			/*
			for(String ofragment :  fragments) {
				ofragment=ofragment.replaceAll( "\\p{Punct}", " " );
				ofragment=ofragment.trim();
				String sa[] = ofragment.split("(?<=[ \\n])");
				EnglishStemmer st = new EnglishStemmer();
				StringBuffer fbuf= new StringBuffer();
				for(String s : sa){
					st.setCurrent(s.trim());
					st.stem();
					fbuf.append(st.getCurrent());
					fbuf.append(" ");
				}
				
				String fragment=fbuf.toString().trim(); //stemmed fragment
				
				if(fragment.length()==0) continue;
				//System.err.println("Annotating: "+fragment);
						
				QueryParser parser = new QueryParser(Version.LUCENE_44, "labels", analyzer);
				Query query = parser.parse(fragment);
				//System.err.println("Searching for: " + query.toString("terms"));
				
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
			*/
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	@Override
	public void addSemanticAnnotation(Document doc, String text) {
		// TODO Auto-generated method stub
		
	}

}
