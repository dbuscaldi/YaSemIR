package fr.lipn.yasemir.configuration;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ConfigurationHandler {
	public static String TERM_FILE;
	public static String FREQ_FILE;
	public static String IDX_FILE;
	public static String TERM_IDX_FILE;
	public static String ONTOLOGY_FILE;
	
	
	public static void init(){
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();

			Document dom = db.parse("config.xml");
			
			Element rootEle = dom.getDocumentElement();
			NodeList nl = rootEle.getChildNodes();
			for(int i = 0 ; i < nl.getLength();i++) {
				Element el = (Element)nl.item(i);
				if(el.getLocalName().equals("terminology_file")){
					TERM_FILE=el.getTextContent();
				}
				if(el.getLocalName().equals("frequency_file")){
					FREQ_FILE=el.getTextContent();
				}
				if(el.getLocalName().equals("termIndex")){
					TERM_IDX_FILE=el.getTextContent();
				}
				if(el.getLocalName().equals("semIndexDir")){
					IDX_FILE=el.getTextContent();
				}
				if(el.getLocalName().equals("ontology_file")){
					ONTOLOGY_FILE=el.getTextContent();
				}
			}

		}catch(Exception pce) {
			pce.printStackTrace();
		}
		
	}

}
