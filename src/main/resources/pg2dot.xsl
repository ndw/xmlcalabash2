<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:pg="http://xmlcalabash.com/ns/graph"
                version="2.0">

<xsl:output method="text" encoding="utf-8"/>

<xsl:template match="pg:graph">
  <xsl:text>digraph pg_graph {&#10;</xsl:text>
  <xsl:apply-templates/>
  <xsl:apply-templates select="//pg:in-edge|//pg:out-edge" mode="links"/>
  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="pg:node">
  <xsl:text>subgraph "cluster</xsl:text>
  <xsl:value-of select="generate-id()"/>
  <xsl:text>" {&#10;</xsl:text>
  <xsl:text>label = "</xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text>";&#10;</xsl:text>

  <xsl:apply-templates/>

  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="pg:inputs">
  <xsl:text>subgraph "cluster</xsl:text>
  <xsl:value-of select="generate-id()"/>
  <xsl:text>" {&#10;</xsl:text>
  <xsl:text>label = "inputs";&#10;</xsl:text>
  <xsl:text>fontcolor = "gray";&#10;</xsl:text>
  <xsl:text>style = "rounded";&#10;</xsl:text>
  <xsl:text>color = "gray";&#10;</xsl:text>

  <xsl:apply-templates/>

  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="pg:outputs">
  <xsl:text>subgraph "cluster</xsl:text>
  <xsl:value-of select="generate-id()"/>
  <xsl:text>" {&#10;</xsl:text>
  <xsl:text>label = "outputs";&#10;</xsl:text>
  <xsl:text>fontcolor = "gray";&#10;</xsl:text>
  <xsl:text>style = "rounded";&#10;</xsl:text>
  <xsl:text>color = "gray";&#10;</xsl:text>

  <xsl:apply-templates/>
  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="pg:in-edge">
  <xsl:variable name="nid" select="generate-id(ancestor::pg:node[1])"/>
  <xsl:text>"</xsl:text>
  <xsl:value-of select="concat($nid, '.', @input-port)"/>
  <xsl:text>" [label="</xsl:text>
  <xsl:value-of select="@input-port"/>
  <xsl:text>"];</xsl:text>
  <xsl:text>&#10;</xsl:text>

  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="pg:out-edge">
  <xsl:variable name="nid" select="generate-id(ancestor::pg:node[1])"/>
  <xsl:text>"</xsl:text>
  <xsl:value-of select="concat($nid, '.', @output-port)"/>
  <xsl:text>" [label="</xsl:text>
  <xsl:value-of select="@output-port"/>
  <xsl:text>"];</xsl:text>
  <xsl:text>&#10;</xsl:text>

  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="element()">
  <xsl:message>Unmatched: <xsl:value-of select="node-name(.)"/></xsl:message>
  <xsl:apply-templates select="@*,node()"/>
</xsl:template>

<xsl:template match="attribute()|text()|comment()|processing-instruction()"/>

<!-- ============================================================ -->

<xsl:template match="pg:in-edge" mode="links">
  <xsl:variable name="source" select="//pg:node[@uid = current()/@source]"/>
  <xsl:variable name="dest" select="../.."/>

  <xsl:text>"</xsl:text>
  <xsl:value-of select="concat(generate-id($source), '.', @output-port)"/>
  <xsl:text>" -&gt; </xsl:text>

  <xsl:text>"</xsl:text>
  <xsl:value-of select="concat(generate-id($dest), '.', @input-port)"/>
  <xsl:text>";&#10;</xsl:text>
</xsl:template>

<xsl:template match="pg:out-edge" mode="links"/>

<xsl:template match="pg:pipe" mode="links">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="@pg:port"/>
  <xsl:text>" -&gt; </xsl:text>

  <xsl:text>"</xsl:text>
  <xsl:value-of select="@pg:id"/>
  <xsl:text>";&#10;</xsl:text>
</xsl:template>

</xsl:stylesheet>
