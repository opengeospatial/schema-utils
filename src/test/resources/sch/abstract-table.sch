<?xml version="1.0" encoding="UTF-8"?>
<iso:schema xmlns="http://purl.oclc.org/dsdl/schematron" 
  xmlns:iso="http://purl.oclc.org/dsdl/schematron" 
  queryBinding='xslt2'
  schemaVersion="ISO-19757-3">

  <iso:ns prefix="db" uri="http://docbook.org/ns/docbook" />

  <!-- Abstract pattern definition -->
  <iso:pattern  abstract='true' id='table'>
    <iso:rule context='$table'>
      <iso:assert test='$row'>
	    <iso:name/> element [<value-of select="@xml:id"/>] should contain rows
	  </iso:assert>
   </iso:rule>
   <iso:rule context='$row'>
     <iso:assert test='$entry'><iso:name/> element should contain entries</iso:assert>
   </iso:rule>
  </iso:pattern>

  <!-- instantiate the pattern for an html table  -->
  <iso:pattern is-a='table' id='html-table'>
    <iso:param name='table' value='db:table[db:caption]'/>
    <iso:param name='row'   value='//db:tr'/>
    <iso:param name='entry' value='db:td|db:th'/>
  </iso:pattern>

  <!-- And re-use it for a CALS table, same structure, different elements  -->
  <iso:pattern is-a='table' id='CALS-table'>
    <iso:param name='table' value='db:table[db:title]'/>
    <iso:param name='row'   value='//db:row'/>
    <iso:param name='entry' value='db:entry'/>
  </iso:pattern>
</iso:schema>
