package fr.lipn.yasemir.ontology.annotation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class IndexRTO {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String terminologyFile="/users/buscaldi/Works/collabSIG/terminology/terminology_0.5.dat";
		//String terminologyFile="terminology.dat";
		String indexPath = "termIndex_0.5";
		
		Directory dir = FSDirectory.open(new File(indexPath));
	    Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_31); //Whitespace?
	    IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_31, analyzer);
	    
	    IndexWriter writer = new IndexWriter(dir, iwc);
	    
		File termFile = new File(terminologyFile);
		
        try {
        	 
            Scanner scanner = new Scanner(termFile);
 
            while (scanner.hasNextLine()) {
            	Document doc=new Document();
            	
                String line = scanner.nextLine();
                String [] items = line.split("\t");
                
                System.err.println("indexing "+items[0]);
                String classIRI =items[0];
                String terms =items[1];
                doc.add(new Field("id", classIRI, Field.Store.YES, Field.Index.NOT_ANALYZED));
                doc.add(new Field("terms", terms, Field.Store.YES, Field.Index.ANALYZED));
                
                writer.addDocument(doc);
            }
            scanner.close();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
	}

}
