<?xml version="1.0" encoding="UTF-8"?>
<iso:schema id="wfs-capabilities-2.0" 
  schemaVersion="${version}"
  xmlns:iso="http://purl.oclc.org/dsdl/schematron" 
  xml:lang="en"
  queryBinding="xslt2">

  <iso:title>Constraints on WFS 2.0 service descriptions.</iso:title>

  <iso:ns prefix="ows" uri="http://www.opengis.net/ows/1.1" />
  <iso:ns prefix="wfs" uri="http://www.opengis.net/wfs/2.0" />
  <iso:ns prefix="fes" uri="http://www.opengis.net/fes/2.0" />
  <iso:ns prefix="xlink" uri="http://www.w3.org/1999/xlink" />

  <iso:p>This Schematron (ISO 19757-3) schema specifies constraints regarding 
  the content of WFS 2.0 service capabilities descriptions.</iso:p>

  <iso:phase id="EssentialCapabilitiesPhase">
    <iso:active pattern="EssentialCapabilitiesPattern"/>
    <iso:active pattern="TopLevelElementsPattern"/>
  </iso:phase>

  <iso:phase id="SimpleWFSPhase">
    <iso:active pattern="ConformanceStatementPattern"/>
    <iso:active pattern="SimpleWFSPattern"/>
  </iso:phase>

  <iso:pattern id="EssentialCapabilitiesPattern">
    <iso:rule context="/">
      <iso:assert test="wfs:WFS_Capabilities" diagnostics="dmsg.root.en">
	  The document element must have [local name] = "WFS_Capabilities" and [namespace name] = "http://www.opengis.net/wfs/2.0.
      </iso:assert>
      <iso:assert test="wfs:WFS_Capabilities/@version = '2.0.0'" diagnostics="dmsg.version.en">
	  The capabilities document must have @version = 2.0.0 .
      </iso:assert>
    </iso:rule>
  </iso:pattern>

  <iso:pattern id="TopLevelElementsPattern">
    <iso:p>Rules regarding the inclusion of common service metadata elements.</iso:p>
    <iso:rule context="/*[1]">
      <iso:assert test="ows:ServiceIdentification">The ows:ServiceIdentification element is missing.</iso:assert>
      <iso:assert test="ows:ServiceProvider">The ows:ServiceProvider element is missing.</iso:assert>
      <iso:assert test="ows:OperationsMetadata">The ows:OperationsMetadata element is missing.</iso:assert>
      <iso:assert test="wfs:FeatureTypeList">The wfs:FeatureTypeList element is missing.</iso:assert>
      <iso:assert test="fes:Filter_Capabilities">The fes:Filter_Capabilities element is missing.</iso:assert>
    </iso:rule>
  </iso:pattern>
  
  <iso:pattern id="ConformanceStatementPattern">
    <iso:p>Implementation conformance statement. See ISO 19142:2010, cl. 8.3.5.3, Table 13.</iso:p>
    <iso:rule context="ows:OperationsMetadata">
      <iso:assert test="ows:Constraint[@name='ImplementsSimpleWFS']/ows:DefaultValue">
      The service constraint 'ImplementsSimpleWFS' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsBasicWFS']/ows:DefaultValue">
      The service constraint 'ImplementsBasicWFS' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsTransactionalWFS']/ows:DefaultValue">
      The service constraint 'ImplementsTransactionalWFS' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsLockingWFS']/ows:DefaultValue">
      The service constraint 'ImplementsLockingWFS' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='KVPEncoding']/ows:DefaultValue">
      The service constraint 'KVPEncoding' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='XMLEncoding']/ows:DefaultValue">
      The service constraint 'XMLEncoding' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='SOAPEncoding']/ows:DefaultValue">
      The service constraint 'SOAPEncoding' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsRemoteResolve']/ows:DefaultValue">
      The service constraint 'ImplementsRemoteResolve' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsResultPaging']/ows:DefaultValue">
      The service constraint 'ImplementsResultPaging' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsStandardJoins']/ows:DefaultValue">
      The service constraint 'ImplementsStandardJoins' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsSpatialJoins']/ows:DefaultValue">
      The service constraint 'ImplementsSpatialJoins' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsTemporalJoins']/ows:DefaultValue">
      The service constraint 'ImplementsTemporalJoins' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsFeatureVersioning']/ows:DefaultValue">
      The service constraint 'ImplementsFeatureVersioning' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ManageStoredQueries']/ows:DefaultValue">
      The service constraint 'ManageStoredQueries' has no ows:DefaultValue child.
      </iso:assert>
    </iso:rule>
  </iso:pattern>
  
  <iso:pattern id="SimpleWFSPattern">
    <iso:p>Simple WFS conformance class. See ISO 19142:2010, cl. 2.</iso:p>
    <iso:rule context="//ows:OperationsMetadata">
      <iso:assert test="lower-case(ows:Constraint[@name='ImplementsSimpleWFS']/ows:DefaultValue) = 'true'">
      The service constraint 'ImplementsSimpleWFS' must be 'true' for all conforming WFS implementations.
      </iso:assert>
      <iso:assert test="lower-case(ows:Constraint[@name='KVPEncoding']//ows:Value) = 'true'">
      The service constraint 'KVPEncoding' must be 'true' for all conforming WFS (Simple) implementations (cl. 8.1).
      </iso:assert>
      <iso:assert test="ows:Operation[@name='GetCapabilities']//ows:Get/@xlink:href">
      The mandatory GET method endpoint for GetCapabilities is missing.
      </iso:assert>
      <iso:assert test="ows:Operation[@name='DescribeFeatureType']//ows:Post/@xlink:href">
      The mandatory POST method endpoint for DescribeFeatureType is missing.
      </iso:assert>
      <iso:assert test="ows:Operation[@name='ListStoredQueries']//ows:Get/@xlink:href">
      The mandatory GET method endpoint for ListStoredQueries is missing.
      </iso:assert>
      <iso:assert test="ows:Operation[@name='DescribeStoredQueries']//ows:Get/@xlink:href">
      The mandatory GET method endpoint for DescribeStoredQueries is missing.
      </iso:assert>
      <iso:assert test="ows:Operation[@name='GetFeature']//ows:Post/@xlink:href">
      The mandatory POST method endpoint for GetFeature is missing.
      </iso:assert>
    </iso:rule>
  </iso:pattern>

  <iso:diagnostics>
    <iso:diagnostic id="dmsg.root.en" xml:lang="en">
    The root element has [local name] = '<iso:value-of select="local-name(/*[1])"/>' and [namespace name] = '<iso:value-of select="namespace-uri(/*[1])"/>'.
    </iso:diagnostic>
    <iso:diagnostic id="dmsg.version.en" xml:lang="en">
    The reported version is <iso:value-of select="/*[1]/@version"/>.
    </iso:diagnostic>
    <iso:diagnostic id="dmsg.serviceType.en" xml:lang="en">
    The reported ServiceType is '<iso:value-of select="./ows:ServiceType"/>'.
    </iso:diagnostic>
    <iso:diagnostic id="dmsg.serviceTypeVersion.en" xml:lang="en">
    The reported ServiceTypeVersion is <iso:value-of select="./ows:ServiceTypeVersion"/>.
    </iso:diagnostic>
  </iso:diagnostics>
  
</iso:schema>