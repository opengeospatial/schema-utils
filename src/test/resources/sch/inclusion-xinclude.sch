<?xml version="1.0" encoding="UTF-8"?>
<iso:schema xmlns="http://purl.oclc.org/dsdl/schematron" 
  xmlns:iso="http://purl.oclc.org/dsdl/schematron" 
  xmlns:xi="http://www.w3.org/2001/XInclude"
  queryBinding='xslt2'
  schemaVersion="ISO-19757-3">

  <iso:ns prefix="db" uri="http://docbook.org/ns/docbook" />

  <iso:pattern id="doc.checks">
    <iso:title>Checking a DocBook v5 document</iso:title>
    <iso:rule context="db:book">
      <iso:report test="db:chapter">
	  Report date: <iso:value-of select="current-dateTime()"/>
	  </iso:report>
    </iso:rule>
  </iso:pattern>

  <iso:pattern id="chapter.checks">
    <iso:title>Basic Chapter checks</iso:title>
    <iso:p>All chapter level checks.</iso:p>
    <xi:include href="rule-chapter.sch" />
  </iso:pattern>
</iso:schema>
