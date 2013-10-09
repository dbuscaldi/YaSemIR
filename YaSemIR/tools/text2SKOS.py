#!/usr/bin/env python
import sys

#This program converts a plain text terminology file into a SKOS file

#Each line of the terminology file is in the format:
#<concept_id>\tab<term_list>
#where <concept_id> is the full concept IRI and <term_list> is a list t1,t2,...tN of N terms that denote the concept_id
#The input parameters are the base ontology address, and the plain text terminology file name

try:
	base_onto_addr = sys.argv[1]
	term_file = sys.argv[2]
except:
	print "input: text2SKOS.py <base_ontology_address> <terminology_file>"

f=open(term_file, "r")

print '<rdf:RDF xml:base="'+base_onto_addr+'" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:skos="http://www.w3.org/TR/2009/REC-skos-reference-20090818/" xmlns:dc="http://purl.org/dc/elements/1.1/" xml:lang="en">'
	
print '<skos:ConceptScheme rdf:about="'+base_onto_addr+'">'
print '\t<dc:type>stemmed</dc:type>'
print '</skos:ConceptScheme>'
for line in f.xreadlines():
	line=line.strip()
	head, tail = line.split('\t')
	print '<skos:Concept rdf:about="'+head+'">'
	els=tail.split(',')
	pref=els[0].strip()
	print '\t<skos:prefLabel xml:lang="en">'+pref+'</skos:prefLabel>'
	if len(els) > 1:
		alt_labels=els[1:]
		for label in alt_labels:
			print'\t<skos:altLabel xml:lang="en">'+label.strip()+'</skos:altLabel>'
	print '\t<skos:inScheme rdf:resource="'+base_onto_addr+'"/>'
	print '</skos:Concept>'
print '</rdf:RDF>'

f.close()

