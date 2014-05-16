<?xml version="1.0" encoding="UTF-8"?>
<iso:rule context="db:chapter"
  xmlns:iso="http://purl.oclc.org/dsdl/schematron">

  <iso:assert test="db:title">Chapter should have  a title</iso:assert>
  <iso:report test="count(db:para)"><iso:value-of select="count(db:para)"/> paragraphs</iso:report>
  <iso:assert test="count(db:para) >= 1">A chapter must have one or more paragraphs</iso:assert>
  <iso:assert test="*[1][self::db:title]">Title must be first child of chapter</iso:assert>
  <iso:assert test="@xml:id">All chapters must have an xml:id attribute</iso:assert>
</iso:rule>
