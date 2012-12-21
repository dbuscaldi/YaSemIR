package fr.lipn.yasemir.ontology.annotation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.semanticweb.owlapi.model.OWLClass;
import org.xml.sax.SAXException;

import fr.lipn.yasemir.ontology.Ontology;
import fr.lipn.yasemir.search.OHSUQuery;
import fr.lipn.yasemir.search.XMLQueryHandler;

public class QueryConverter {
	private static String ontologyLocation = "/users/buscaldi/Works/collabSIG/meshonto.owl";
	private static HashMap<String, String> convert = new HashMap<String, String>();
	
	private static String convertCommas(String str){
		String [] parts = str.split(",");
		String head = parts[1].trim();
		String ret=head+" "+parts[0];
		return ret;
	}
	
	private static void initMap(){
		convert.put("female", "women");
		convert.put("male", "men");
		convert.put("hormones", "hormone");
		convert.put("sex_hormone", "gonadal_hormone");
		convert.put("lupus", "cutaneous_lupus_erythematosus");
		convert.put("etidronate_disodium", "etidronic_acid");
		convert.put("menaupause", "menopause");
		convert.put("white", "caucasian");
		convert.put("black", "african_american");
		convert.put("hemiballismus", "dyskinesia");
		convert.put("carotid_arterie", "carotid_artery");
		convert.put("middle_age", "middle_aged");
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		File queryFile = new File("/users/buscaldi/Works/collabSIG/query.ohsu.1-63ext.xml");
		XMLQueryHandler hdlr = new XMLQueryHandler(queryFile);
		Vector<OHSUQuery> queries = hdlr.getParsedQueries();
		
		Ontology.init(ontologyLocation);
		initMap();
		
		StringBuffer mqfb = new StringBuffer();
		mqfb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		mqfb.append("\n");
		mqfb.append("<topics>\n");
		for(OHSUQuery oq : queries){
			mqfb.append("<top>\n");
			mqfb.append("<num>"+oq.getID().trim()+"</num>\n");
			mqfb.append("<title>"+oq.getTitle().trim()+"</title>\n");
			mqfb.append("<desc>"+oq.getDescription().trim()+"</desc>\n");
			mqfb.append("<annotation>\n");
			List<String> cats = oq.getCategoryList();
			for(String c : cats){
				if(c.equals("")) continue;
				if(c.contains(",")) c=convertCommas(c);
				c=c.replace(' ', '_');
				if(c.endsWith("s") && !convert.containsKey(c) && !c.endsWith("us") && !c.endsWith("is")){
					c=c.substring(0, c.length()-1);
				}
				if(convert.containsKey(c)){
					String tmp;
					tmp = convert.get(c);
					c=tmp;
				}
				String meshCat = "http://org.snu.bike/MeSH#"+c;
				/*
				OWLClass oc = Ontology.classForID(meshCat);
				System.err.print(meshCat+"\t->\t");
				System.err.println(oc.getIRI().getFragment());
				*/
				mqfb.append("<concept>"+meshCat+"</concept>\n");
			}
			mqfb.append("</annotation>\n");
			mqfb.append("</top>\n\n");
		}
		mqfb.append("</topics>\n");
		
		System.out.println(mqfb);

	}

}
