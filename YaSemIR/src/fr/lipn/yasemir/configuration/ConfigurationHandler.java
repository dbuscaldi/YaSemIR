package fr.lipn.yasemir.configuration;
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

import java.util.HashMap;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ConfigurationHandler {
	public static String INDEXDIR;
	public static String YASEMIR_HOME;
	public static String CORPUSDIR;
	public static String CORPUSLANG;
	public static String TERMIDXDIR;
	
	private static Vector<String> semanticallyIndexedFields;
	private static Vector<String> classicallyIndexedFields;
	
	public static String CONCEPTWEIGHT;
	public static String SIM_MEASURE;
	public static boolean NGRAMS_ENABLED;
	public static String SEARCH_MODE;
	public static String ANNOTENGINE;
	public static String DOCIDFIELD;
	public static String DOC_DELIM;
	public static boolean IDFIELD_ASATTR;
	
	private static HashMap<String, String> ontoSKOSmap;
	private static HashMap<String, String> ontoRootmap;
	
	
	
	public static Vector<String> getSemanticFields(){
		return semanticallyIndexedFields;
	}
	
	public static Vector<String> getClassicFields(){
		return classicallyIndexedFields;
	}
	
	public static HashMap<String, String> getOntologySKOSMap(){
		return ontoSKOSmap;
	}
	
	public static HashMap<String, String> getOntologyRootMap(){
		return ontoRootmap;
	}
	
	public static void init(String configFileLocation){
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringComments(true);
		dbf.setIgnoringElementContentWhitespace(true);
		dbf.setNamespaceAware(true);
		//dbf.setCoalescing(true);
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();

			Document dom = db.parse(configFileLocation); //"config.xml"
			
			semanticallyIndexedFields = new Vector<String>();
			classicallyIndexedFields = new Vector<String>();
			
			ontoSKOSmap = new HashMap<String, String>();
			ontoRootmap = new HashMap<String, String>();
			
			Element rootEle = dom.getDocumentElement();
			NodeList nl = rootEle.getChildNodes();
			for(int i = 0 ; i < nl.getLength();i++) {
				try {
					Node n=nl.item(i);
					if(n.getNodeType()==Node.ELEMENT_NODE) {
						Element el = (Element)nl.item(i);
						//System.err.println(el.getNodeName()+" : "+n.getTextContent());
						
						if(el.getNodeName().equals("indexdir")){
							INDEXDIR=el.getTextContent();
						}
						if(el.getNodeName().equals("basedir")){
							YASEMIR_HOME=el.getTextContent();
						}
						if(el.getNodeName().equals("collection")){
							CORPUSDIR=el.getTextContent();
							CORPUSLANG=el.getAttribute("lang");
						}
						if(el.getNodeName().equals("termdir")){
							TERMIDXDIR=el.getTextContent();
						}
						if(el.getNodeName().equals("annotator")){
							ANNOTENGINE=el.getTextContent();
						}
						if(el.getNodeName().equals("idfield")){
							DOCIDFIELD = el.getTextContent();
							IDFIELD_ASATTR=Boolean.parseBoolean(el.getAttribute("isattr"));
						}
						if(el.getNodeName().equals("docdelim")){
							DOC_DELIM = el.getTextContent();
						}
						if(el.getNodeName().equals("ontologies")){
							NodeList ol = el.getChildNodes();
							for(int j=0; j< ol.getLength(); j++){
								Node nn = ol.item(j);
								if(nn.getNodeType()==Node.ELEMENT_NODE){
									Element ole = (Element)ol.item(j);
									String ontoroot = ole.getAttribute("root");
									String ontoloc = ole.getAttribute("ofile");
									String ontoterm = ole.getAttribute("tfile");
									ontoSKOSmap.put(ontoloc, ontoterm);
									ontoRootmap.put(ontoloc, ontoroot);
								}
							}
						}
						if(el.getNodeName().equals("params")){
							NodeList ol = el.getChildNodes();
							for(int j=0; j< ol.getLength(); j++){
								Node nn = ol.item(j);
								if(nn.getNodeType()==Node.ELEMENT_NODE){
									Element ole = (Element)ol.item(j);
									if(ole.getNodeName().equals("weight")){
										CONCEPTWEIGHT=ole.getAttribute("val");
									}
									if(ole.getNodeName().equals("distance")){
										SIM_MEASURE=ole.getAttribute("val");
									}
									if(ole.getNodeName().equals("ngrams")){
										NGRAMS_ENABLED=Boolean.parseBoolean(ole.getAttribute("val"));
									}
									if(ole.getNodeName().equals("search_mode")){
										SEARCH_MODE=ole.getAttribute("val");
									}
								}
							}
						}
						if(el.getNodeName().equals("semanticfields")){
							NodeList ol = el.getChildNodes();
							for(int j=0; j< ol.getLength(); j++){
								Node nn = ol.item(j);
								if(nn.getNodeType()==Node.ELEMENT_NODE){
									if(nn.getNodeName().equals("field")){
										semanticallyIndexedFields.add(nn.getTextContent());
									}
								}
							}
						}
						if(el.getNodeName().equals("classicfields")){
							NodeList ol = el.getChildNodes();
							for(int j=0; j< ol.getLength(); j++){
								Node nn = ol.item(j);
								if(nn.getNodeType()==Node.ELEMENT_NODE){
									if(nn.getNodeName().equals("field")){
										classicallyIndexedFields.add(nn.getTextContent());
									}
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}catch(Exception pce) {
			pce.printStackTrace();
		}
		
	}
	
	

}
