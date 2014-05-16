<?xml version="1.0" encoding="UTF-8"?>
<!-- invalid queryBinding -->
<iso:schema id="Invalid"
  xmlns:iso="http://purl.oclc.org/dsdl/schematron" 
  xml:lang="en"
  queryBinding="qb">

  <iso:title>Rules for W3C SOAP Fault messages.</iso:title>
  
  <iso:ns prefix="env" uri="http://www.w3.org/2003/05/soap-envelope" />
  <iso:ns prefix="ows" uri="http://www.opengis.net/ows" />
  
  <iso:p>This ISO Schematron schema specifies rules for validating the 
  content of W3C SOAP fault messages.</iso:p>
  
  <iso:let name="fault-code" value="'Sender'" />
  <iso:let name="expected-exception-code" value="'InvalidRequest'" />

  <iso:phase id="SoapFaultPhase">
    <iso:active pattern="FaultMessagePattern" />
  </iso:phase>

  <iso:pattern id="FaultMessagePattern">
    <iso:rule context="/">
      <iso:assert test="env:Envelope" diagnostics="dmesg.root.en" see="http://www.w3.org/TR/soap12-part1/#soapfault">
	  The document element must have [local name] = "Envelope" and [namespace name] = "http://www.w3.org/2003/05/soap-envelope".
      </iso:assert>
    </iso:rule>
    <iso:rule context="/env:Envelope">
      <iso:assert test="env:Body">Missing required env:Body element.</iso:assert>
      <iso:assert test="count(env:Body/*) = 1">Body must have only 1 child element.</iso:assert>
      <iso:assert test="env:Body/env:Fault">Required env:Fault element is missing from env:Body.</iso:assert>
    </iso:rule>
    <iso:rule context="//env:Fault">
      <iso:assert test="env:Code/env:Value">Missing required fault code.</iso:assert>
      <iso:assert test="env:Reason/env:Text">Missing required reason.</iso:assert>
      <iso:assert test="env:Detail/ows:ExceptionReport">Missing detail entry (ows:ExceptionReport)</iso:assert>
    </iso:rule>
  </iso:pattern>

  <iso:diagnostics>
    <iso:diagnostic id="dmesg.root.en" xml:lang="en">
    The root element has [local name] = '"<iso:value-of select="local-name(/*[1])"/>" and [namespace name] = "<iso:value-of select="namespace-uri(/*[1])"/>".
    </iso:diagnostic>
  </iso:diagnostics>

</iso:schema>