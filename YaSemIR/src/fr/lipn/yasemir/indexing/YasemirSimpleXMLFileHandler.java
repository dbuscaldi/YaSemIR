package fr.lipn.yasemir.indexing;
/*
 * Copyright (C) 2013, Université Paris Nord
 *
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fr.lipn.yasemir.configuration.Yasemir;
/**
 * This class provides an handler for XML files
 * @author buscaldi
 *
 */
public class YasemirSimpleXMLFileHandler extends DefaultHandler {
  protected HashMap<String, StringBuffer> fieldBuffers = new HashMap<String, StringBuffer>();
  protected StringBuffer docIDBuffer = new StringBuffer();
  
  protected Stack<String> elemStack;
  protected Document currDoc;
  protected Vector<Document> parsedDocuments;
  
  public YasemirSimpleXMLFileHandler(File xmlFile) 
  	throws ParserConfigurationException, SAXException, IOException {
    
    SAXParserFactory spf = SAXParserFactory.newInstance();

    SAXParser parser = spf.newSAXParser();
    
    parsedDocuments=new Vector<Document>();
    currDoc=new Document();
    
    //init fieldBuffers
    HashSet<String> balises = new HashSet<String>();
    balises.addAll(Yasemir.semBalises);
    balises.addAll(Yasemir.clsBalises);
    for(String sf : balises){
    	StringBuffer buf = new StringBuffer();
    	fieldBuffers.put(sf, buf);
    }
    
    if(Yasemir.idField==null) this.docIDBuffer.append(xmlFile.getName()); // we use file name if a field ID is not given
    
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
     if(eName.equals(Yasemir.DOC_DELIM)){
    	 currDoc=new Document();
    	 for(String key : fieldBuffers.keySet()){
    		 StringBuffer buf = fieldBuffers.get(key);
    		 buf.setLength(0);
    		 fieldBuffers.put(key, buf);
    	 }
    	 docIDBuffer.setLength(0);
    	 if(Yasemir.ID_ASATTR && !(Yasemir.idField==null)){
    		 if (attrs != null) {
    			 docIDBuffer.append(attrs.getValue(Yasemir.idField));
    		 }
    	 }
     }
     
  }

  // call when cdata found
  public void characters(char[] text, int start, int length)
    throws SAXException {
	  String topElement=elemStack.peek();
	  StringBuffer buf;
	  if(Yasemir.isIDTag(topElement)) buf = docIDBuffer;
	  else buf = fieldBuffers.get(topElement);
	  
	  if(buf!= null){
		  buf.append(text, start, length);
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
    
    if (eName.equals(Yasemir.DOC_DELIM)){
    	//ID
    	System.err.println("[YaSemIR] indexing document "+this.docIDBuffer.toString());
    	//currDoc.add(new Field("id", this.docIDBuffer.toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
    	currDoc.add(new StringField("id", this.docIDBuffer.toString(), Field.Store.YES));
    	//classicText
    	StringBuffer plainText = new StringBuffer();
    	StringBuffer semText = new StringBuffer(); //text to be labelled semantically
    	for(String field : fieldBuffers.keySet()){
    		if(Yasemir.clsBalises.contains(field)) plainText.append(fieldBuffers.get(field));
    		if(Yasemir.semBalises.contains(field)) semText.append(fieldBuffers.get(field));
    	}
    	//base text
    	currDoc.add(new TextField("text", plainText.toString(), Field.Store.YES));
    	
    	if(Yasemir.DEBUG) System.err.println("[YaSemIR] adding annotations to document "+this.docIDBuffer);
    	//annotation of "semantic" content with each ontology and store one index per ontology
    	Yasemir.annotator.addSemanticAnnotation(currDoc, semText.toString());
    	
    	//now store the parsed document 
    	parsedDocuments.add(currDoc);
    }
  }
  
  public Document getLastParsedDocument() {
	  return this.currDoc;
  }
  
  public Vector<Document> getParsedDocuments() {
	  return this.parsedDocuments;
  }
	
}