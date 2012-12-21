package fr.lipn.yasemir.ontology.annotation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fr.lipn.yasemir.search.OHSUQuery;

public class XMLMTITestHandler extends DefaultHandler {
	/* A buffer for each XML element */
	  protected StringBuffer categBuffer = new StringBuffer();
	  protected StringBuffer descBuffer = new StringBuffer();
	  protected StringBuffer titleBuffer = new StringBuffer();
	  protected StringBuffer idBuffer = new StringBuffer();
	  protected Vector<String> catVec = new Vector<String>();
	  
	  protected Stack<String> elemStack;
	  protected Vector<OHSUQuery> parsedQueries;
	  protected OHSUQuery cur_query;
	  
	  private HashMap<String, String> convert = new HashMap<String, String>();
		
	  private String convertCommas(String str){
			String [] parts = str.split(",");
			String head = parts[1].trim();
			String ret=head+" "+parts[0];
			return ret;
	  }
		
		private void initMap(){
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
		
		private String normBIKE(String c){
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
			String bikeCat = "http://org.snu.bike/MeSH#"+c;
			
			return bikeCat;
		}
		
	  public XMLMTITestHandler(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
		
		SAXParserFactory spf = SAXParserFactory.newInstance();
	    SAXParser parser = spf.newSAXParser();
	    try {
	    	initMap();
	      parser.parse(xmlFile, this);
	    } catch (org.xml.sax.SAXParseException spe) {
	      System.out.println("SAXParser caught SAXParseException at line: " +
	        spe.getLineNumber() + " column " +
	        spe.getColumnNumber() + " details: " +
			spe.getMessage());
	    }
	  }

	  // call at document start
	  public void startDocument() throws SAXException {
		  parsedQueries=new Vector<OHSUQuery>();
		  elemStack=new Stack<String>();
	  }

	  // call at element start
	  public void startElement(String namespaceURI, String localName,
	    String qualifiedName, Attributes attrs) throws SAXException {

	    String eName = localName;
	     if ("".equals(eName)) {
	       eName = qualifiedName; // namespaceAware = false
	     }
	     
	     elemStack.addElement(eName);
	     if(eName=="MedlineCitation") {
	    	cur_query= new OHSUQuery();
	     	categBuffer.setLength(0);
	     	titleBuffer.setLength(0);
	     	descBuffer.setLength(0);
	     	idBuffer.setLength(0);
	     	catVec.clear();
	     }
	     
	     if(eName=="DescriptorName") {
	     	categBuffer.setLength(0);
	     }
	     
	     // list the attribute(s)
	     if (attrs != null) {
	       for (int i = 0; i < attrs.getLength(); i++) {
	         String aName = attrs.getLocalName(i); // Attr name
	         if ("".equals(aName)) { aName = attrs.getQName(i); }
	         // perform application specific action on attribute(s)
	         // for now just dump out attribute name and value
	         //System.out.println("attr " + aName+"="+attrs.getValue(i));
	       }
	     }
	  }

	  // call when cdata found
	  public void characters(char[] text, int start, int length)
	    throws SAXException {
	  	if(elemStack.peek().equalsIgnoreCase("ArticleTitle")){
	  		titleBuffer.append(text, start, length);
	  	} else if (elemStack.peek().startsWith("Abstract")) { //Abstract or AbstractText
	  		descBuffer.append(text, start, length);
	  	} else if (elemStack.peek().equalsIgnoreCase("PMID")) {
	  		idBuffer.append(text, start, length);
	  	} else if (elemStack.peek().equalsIgnoreCase("DescriptorName")){
	  		categBuffer.append(text, start, length);
	  	}
	  }

	  // call at element end
	  public void endElement(String namespaceURI, String simpleName,
	    String qualifiedName)  throws SAXException {

	    String eName = simpleName;
	    if ("".equals(eName)) {
	      eName = qualifiedName; // namespaceAware = false
	    }
	    
	    elemStack.pop();
	    if (eName.equals("MedlineCitation")){
	    	cur_query.setID(idBuffer.toString());
	    	cur_query.setTitle(titleBuffer.toString());
	    	cur_query.setDescription(descBuffer.toString());
	    	cur_query.setCategoryVector(catVec);
	    	
	    	parsedQueries.add(cur_query);
	    }
	    if (eName.equals("DescriptorName")){
	    	String bikeSTR=normBIKE(categBuffer.toString().toLowerCase());
	    	catVec.addElement(bikeSTR);
	    }
	  }
	  
	  public Vector<OHSUQuery> getParsedQueries() {
		  return this.parsedQueries;
	  }
}
