<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs"
                version="3.0">

<!-- Reads the test-suite-report document and produces a list of passing tests. -->

<xsl:output method="text" encoding="utf-8"/>

<xsl:template match="/">
  <xsl:variable name="passing-tests" as="xs:string*">
    <xsl:apply-templates select="//testcase[not(failure) and not(skipped) and not(error)]"/>
  </xsl:variable>
  <xsl:for-each select="$passing-tests">
    <xsl:sort select="."/>
    <xsl:sequence select=". || '&#10;'"/>
  </xsl:for-each>
</xsl:template>

<xsl:template match="testcase" as="xs:string">
  <xsl:sequence select="string(@name)"/>
</xsl:template>

<xsl:template match="attribute()|text()|comment()|processing-instruction()"/>

</xsl:stylesheet>
