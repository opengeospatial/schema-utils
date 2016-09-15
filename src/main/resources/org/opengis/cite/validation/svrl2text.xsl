<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:dc="http://purl.org/dc/terms/"  
  xmlns:svrl="http://purl.oclc.org/dsdl/svrl">

  <dc:title>SVRL to Markdown</dc:title>
  <dc:description>Generates a plain text summary of an SVRL report using Markdown syntax.</dc:description>
  <dc:date>2016-09-15</dc:date>

  <xsl:output method="text" />

  <xsl:template match="/svrl:schematron-output">
    <xsl:value-of select="concat('# ', @title, '&#xA;&#xA;')" />
    <xsl:for-each select="svrl:failed-assert | svrl:successful-report">
      <xsl:call-template name="rule-violation" />
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="rule-violation">
    <xsl:variable name="severity">
      <xsl:choose>
        <xsl:when test="./@flag">
          <xsl:value-of select="./@flag" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>Error</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:text>_____&#xA;</xsl:text>
    <!-- add two trailing spaces to force line break -->
    <xsl:value-of select="concat('Severity: ', $severity, '  &#xA;')" />
    <xsl:value-of select="concat('Message: ', normalize-space(svrl:text), '  &#xA;')" />
    <xsl:value-of select="concat('Test: ', ./@test, '  &#xA;')" />
    <xsl:value-of select="concat('Location: ', ./@location, '  &#xA;')" />
  </xsl:template>

</xsl:transform>
