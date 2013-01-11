package fr.lipn.yasemir.ontology.annotation;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.semanticweb.owlapi.model.OWLClass;
import org.tartarus.snowball.ext.EnglishStemmer;

import fr.lipn.yasemir.indexing.ohsumed.ClassFrequencyCollector;
import fr.lipn.yasemir.indexing.ohsumed.OHSUMedIndexer;
import fr.lipn.yasemir.ontology.Ontology;

public class IndexBasedAnnotator implements SemanticAnnotator {
	private String termIndexPath;
	
	public IndexBasedAnnotator(String termIndexPath) {
		this.termIndexPath=termIndexPath;
	}
	
	public Vector<Annotation> annotate(String document){
		Vector<Annotation> annotations = new Vector<Annotation>();
		try {
			IndexReader reader = IndexReader.open(FSDirectory.open(new File(termIndexPath)));
			IndexSearcher searcher = new IndexSearcher(reader);
			Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_31);
			
			document=document.replaceAll("Support, .+?;", "");
			document=document.replaceAll("\\[.*?\\]", "").trim();
			//document = document.replaceAll( "\\p{Punct}", " " );
			String [] fragments = document.split("[;:\\.,]");
			
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
						
				QueryParser parser = new QueryParser(Version.LUCENE_31, "terms", analyzer);
				Query query = parser.parse(fragment);
				//System.err.println("Searching for: " + query.toString("terms"));
				
				TopDocs results = searcher.search(query, 20);
			    ScoreDoc[] hits = results.scoreDocs;
			    
			    int numTotalHits = results.totalHits;
			    //System.err.println(numTotalHits + " total matching classes");
			    
			    if(numTotalHits > 0) {
				    hits = searcher.search(query, numTotalHits).scoreDocs;
				    for(int i=0; i<numTotalHits; i++){
				    	Document doc = searcher.doc(hits[i].doc);
				    	String ptrn = "(?i)("+doc.get("terms").replaceAll(", ", "|")+")";
				    	//System.err.println("OWLClass="+doc.get("id")+" score="+hits[i].score);
				    	if(checkPattern(fragment, ptrn)){
				    		//System.err.println("OWLClass="+doc.get("id")+" score="+hits[i].score);
				    		Annotation ann = new Annotation(doc.get("id"));
					    	annotations.add(ann);
				    	}
				    }
			    }
								 
			}
			searcher.close();
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return annotations;
		 
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
	
	public void addSemanticAnnotation(Document doc, String text, String field){
		Vector<Annotation> av = this.annotate(text);
		  StringBuffer av_repr = new StringBuffer();
		  for(Annotation a : av){
			  av_repr.append(a.getOWLClass().getIRI().getFragment());
			  av_repr.append(" ");
		  }
		  if(OHSUMedIndexer.VERBOSE) System.err.println(av_repr);
		  doc.add(new Field(field+"annot", av_repr.toString().trim(), Field.Store.YES, Field.Index.ANALYZED));
		  //we add the annotation and the supertypes to an extended index to be used during the beginning of the search process
		  Set<OWLClass> expansion = new HashSet<OWLClass>();
		  for(Annotation a : av){
			  Set<OWLClass> sup_a = Ontology.getAllSuperClasses(a.getOWLClass());
			  Set<OWLClass> roots = Ontology.getOntologyRoots(); //this is needed to calculate the frequencies of root classes
			  roots.retainAll(sup_a);
			  ClassFrequencyCollector.add(roots);
			  expansion.addAll(sup_a);
		  }
		  for(OWLClass c : expansion){
			  av_repr.append(c.getIRI().getFragment());
			  av_repr.append(" ");
		  }
		  doc.add(new Field(field+"annot_exp", av_repr.toString().trim(), Field.Store.YES, Field.Index.ANALYZED));
		  if(OHSUMedIndexer.VERBOSE) System.err.println(av_repr);
		   
	}

	public Collection<? extends Annotation> extractCategories(List<String> categoryList) {
		Vector<Annotation> annotations = new Vector<Annotation>();
		for(String catID : categoryList){
			Annotation ann = new Annotation(catID);
			annotations.add(ann);
		}
		return annotations;
	}
	
}

class WeightedTag {
	String tag;
	Double w;
	
	WeightedTag(String t, double w){
		this.tag=t;
		this.w=new Double(w);
	}

	public String getName(){
		return tag;
	}
	
	public Double getWeight() {
		return w;
	}
	
	
}

class TagComparator implements Comparator<WeightedTag>{
	 
    @Override
    public int compare(WeightedTag t1, WeightedTag t2) {
    	Double w1=t1.getWeight();
    	Double w2=t2.getWeight();
    	
    	return -w1.compareTo(w2);
        
    }
}
