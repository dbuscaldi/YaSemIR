package fr.lipn.yasemir.indexing;


import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import java.io.File;
import java.io.IOException;
import java.util.Stack;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLVilmorinFileHandler extends DefaultHandler {
  /* A buffer for each XML element */
  protected StringBuffer textBuffer = new StringBuffer();
  protected StringBuffer titleBuffer = new StringBuffer();
  protected StringBuffer parentBuffer = new StringBuffer();
  protected String docID = new String();
  
  protected Stack<String> elemStack;
  protected Document parsedDocument;
  
  public XMLVilmorinFileHandler(File xmlFile) 
  	throws ParserConfigurationException, SAXException, IOException {
    
	// Now let's move to the parsing stuff
    SAXParserFactory spf = SAXParserFactory.newInstance();
    
    // use validating parser?
    //spf.setValidating(false);
    // make parser name space aware?
    //spf.setNamespaceAware(true);

    SAXParser parser = spf.newSAXParser();
    this.docID=xmlFile.getName();
    //System.out.println("parser is validating: " + parser.isValidating());
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
	  parsedDocument=new Document();
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
     if(eName=="fiche") {
     	textBuffer.setLength(0);
     	titleBuffer.setLength(0);
     	parentBuffer.setLength(0);
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
  	if(elemStack.peek().equalsIgnoreCase("taxon")){
  		titleBuffer.append(text, start, length);
  	} else if (elemStack.peek().equalsIgnoreCase("parent")) {
  		parentBuffer.append(text, start, length);
  	} else if (elemStack.peek().equalsIgnoreCase("titre") || elemStack.peek().equalsIgnoreCase("alinea") || elemStack.peek().equalsIgnoreCase("enonce")) {
  		textBuffer.append(text, start, length);
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
    if (eName.equals("fiche")){
    	String fullText=titleBuffer.toString()+" "+parentBuffer.toString()+" "+textBuffer.toString();
    	parsedDocument.add(new Field("titre", titleBuffer.toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
    	parsedDocument.add(new Field("parent", parentBuffer.toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
    	parsedDocument.add(new Field("contenu", fullText, Field.Store.YES, Field.Index.ANALYZED));
    	parsedDocument.add(new Field("name", this.docID, Field.Store.YES, Field.Index.NOT_ANALYZED));

    }
  }
  
  public Document getParsedDocument() {
	  return this.parsedDocument;
  }
	
}