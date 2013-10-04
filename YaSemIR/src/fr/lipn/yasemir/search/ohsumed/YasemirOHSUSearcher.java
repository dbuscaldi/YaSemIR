package fr.lipn.yasemir.search.ohsumed;

import java.io.File;
import java.util.Locale;
import java.util.Vector;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import fr.lipn.yasemir.configuration.Yasemir;
import fr.lipn.yasemir.search.RankedDocument;
import fr.lipn.yasemir.search.SemanticSearcher;

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
			
			String basefield = "text";
			
			SemanticSearcher ssearcher = new SemanticSearcher();
			
			String conf_str="run_";
			if(Yasemir.MODE==Yasemir.CLASSIC){
				conf_str+="n"; 
			} else if(Yasemir.MODE==Yasemir.HYBRID){
				conf_str+="h";
			}
			if(CONFIGURATION==TITLE_DESC) conf_str+="td";
			if(Yasemir.CKPD_ENABLED) conf_str+="ckpd";
			
			for(OHSUQuery oq : queries){
				StringBuffer query = new StringBuffer();
				query.append(oq.getTitle());
				if(CONFIGURATION==TITLE_DESC) {
					query.append(" ");
					query.append(oq.getDescription());	
				}
				Vector<RankedDocument> srDocs = ssearcher.search(query.toString());
				
				for (int i = 0; i < Math.min(srDocs.size(), MAX_HITS); i++) {
					RankedDocument srd = srDocs.elementAt(i);
					System.out.println(oq.getID()+"\tQ0\t"+srd.getID()+"\t"+i+"\t"+String.format(Locale.US, "%.4f",srd.getScore())+"\t"+conf_str);
			    		
				}
			}
			
		    ssearcher.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
