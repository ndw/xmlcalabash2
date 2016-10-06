<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:px="http://xmlcalabash.com/ns/parsed"
                version="2.0">

<xsl:output method="text" encoding="utf-8"/>

<xsl:template match="/*">
  <xsl:text>digraph px_pipeline {&#10;</xsl:text>
  <xsl:apply-templates/>
  <xsl:apply-templates select="//px:pipe" mode="links"/>
  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="px:declare-step|px:pipeline">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="px:atomic-step">
  <xsl:text>subgraph "cluster</xsl:text>
  <xsl:value-of select="@px:id"/>
  <xsl:text>" {&#10;</xsl:text>
  <xsl:text>label = "</xsl:text>
  <xsl:value-of select="concat(@px:type, '/', @name)"/>
  <xsl:text>";&#10;</xsl:text>

  <xsl:apply-templates/>

  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="px:expression-step">
  <xsl:variable name="norm" select="replace(normalize-space(@select), '&quot;', '\\&quot;')"/>
  <xsl:variable name="expr"
                select="if (string-length($norm) &gt; 15)
                        then concat(substring($norm, 1, 15), '…')
                        else $norm"/>
  <xsl:text>subgraph "cluster</xsl:text>
  <xsl:value-of select="@px:id"/>
  <xsl:text>" {&#10;</xsl:text>
  <xsl:text>label = "$</xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text>\nselect=</xsl:text>
  <xsl:value-of select="$expr"/>
  <xsl:text>";&#10;</xsl:text>

  <xsl:apply-templates/>

  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="px:input-edge">
  <xsl:text>subgraph "cluster</xsl:text>
  <xsl:value-of select="@px:id"/>
  <xsl:text>" {&#10;</xsl:text>
  <xsl:text>label = "</xsl:text>
  <xsl:value-of select="@port"/>
  <xsl:text> edge";&#10;</xsl:text>

  <xsl:apply-templates/>

  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="px:output-edge">
  <xsl:text>subgraph "cluster</xsl:text>
  <xsl:value-of select="@px:id"/>
  <xsl:text>" {&#10;</xsl:text>
  <xsl:text>label = "</xsl:text>
  <xsl:value-of select="@port"/>
  <xsl:text> edge";&#10;</xsl:text>

  <xsl:apply-templates/>

  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="px:input">
  <xsl:text>subgraph "cluster</xsl:text>
  <xsl:value-of select="@px:id"/>
  <xsl:text>" {&#10;</xsl:text>
  <xsl:text>label = "input </xsl:text>
  <xsl:value-of select="@port"/>
  <xsl:text>";&#10;</xsl:text>

  <xsl:apply-templates/>

  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="px:output">
  <xsl:text>subgraph "cluster</xsl:text>
  <xsl:value-of select="@px:id"/>
  <xsl:text>" {&#10;</xsl:text>
  <xsl:text>label = "output </xsl:text>
  <xsl:value-of select="@port"/>
  <xsl:text>";&#10;</xsl:text>

  <xsl:text>"</xsl:text>
  <xsl:value-of select="@px:id"/>
  <xsl:text>";&#10;</xsl:text>

  <xsl:apply-templates/>
  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="px:pipe">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="@px:id"/>
  <xsl:text>";&#10;</xsl:text>

  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="element()">
  <xsl:message>Unmatched: <xsl:value-of select="node-name(.)"/></xsl:message>
  <xsl:apply-templates select="@*,node()"/>
</xsl:template>

<xsl:template match="attribute()|text()|comment()|processing-instruction()"/>

<!-- ============================================================ -->

<xsl:template match="px:pipe" mode="links">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="@px:port"/>
  <xsl:text>" -&gt; </xsl:text>

  <xsl:text>"</xsl:text>
  <xsl:value-of select="@px:id"/>
  <xsl:text>";&#10;</xsl:text>
</xsl:template>


</xsl:stylesheet>
