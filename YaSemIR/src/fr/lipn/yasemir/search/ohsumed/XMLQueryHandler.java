package fr.lipn.yasemir.search.ohsumed;

import java.io.File;
import java.io.IOException;
import java.util.Stack;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class XMLQueryHandler extends DefaultHandler {
  /* A buffer for each XML element */
  protected StringBuffer categBuffer = new StringBuffer();
  protected StringBuffer descBuffer = new StringBuffer();
  protected StringBuffer titleBuffer = new StringBuffer();
  protected StringBuffer idBuffer = new StringBuffer();
  protected Vector<String> catVec = new Vector<String>();
  
  protected Stack<String> elemStack;
  protected Vector<OHSUQuery> parsedQueries;
  protected OHSUQuery cur_query;
  
  protected boolean NEW_FORMAT=false;
  
  public XMLQueryHandler(File xmlFile) 
  	throws ParserConfigurationException, SAXException, IOException {
	
	SAXParserFactory spf = SAXParserFactory.newInstance();
    SAXParser parser = spf.newSAXParser();
    try {
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
     if(eName=="top") {
    	cur_query= new OHSUQuery();
     	categBuffer.setLength(0);
     	titleBuffer.setLength(0);
     	descBuffer.setLength(0);
     	idBuffer.setLength(0);
     	catVec.clear();
     }
     
     if(eName=="concept") {
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
  	if(elemStack.peek().equalsIgnoreCase("title")){
  		titleBuffer.append(text, start, length);
  	} else if (elemStack.peek().equalsIgnoreCase("desc")) {
  		descBuffer.append(text, start, length);
  	} else if (elemStack.peek().equalsIgnoreCase("num")) {
  		idBuffer.append(text, start, length);
  	} else if (elemStack.peek().equalsIgnoreCase("categ")) {
  		categBuffer.append(text, start, length);
  	} else if (elemStack.peek().equalsIgnoreCase("concept")){
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
    if (eName.equals("top")){
    	cur_query.setID(idBuffer.toString());
    	cur_query.setTitle(titleBuffer.toString());
    	cur_query.setDescription(descBuffer.toString());
    	if(!NEW_FORMAT) cur_query.setCategories(categBuffer.toString());
    	else cur_query.setCategoryVector(catVec);
    	
    	parsedQueries.add(cur_query);
    }
    if (eName.equals("concept")){
    	NEW_FORMAT=true;
    	catVec.addElement(categBuffer.toString());
    }
  }
  
  public Vector<OHSUQuery> getParsedQueries() {
	  return this.parsedQueries;
  }
	
}