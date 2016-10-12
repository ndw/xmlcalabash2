<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:px="http://xmlcalabash.com/ns/parsed"
                xmlns:f="http://nwalsh.com/xslt/functions"
                version="2.0">

<xsl:output method="text" encoding="utf-8"/>

<xsl:template match="/*">
  <xsl:text>digraph px_pipeline {&#10;</xsl:text>
  <xsl:apply-templates/>
  <xsl:apply-templates mode="links"/>
  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="px:declare-step|px:pipeline">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="px:document">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="@px:id"/>
  <xsl:text>";&#10;</xsl:text>
</xsl:template>

<xsl:template match="px:atomic-step|px:for-each">
  <xsl:text>subgraph "cluster</xsl:text>
  <xsl:value-of select="@px:id"/>
  <xsl:text>" {&#10;</xsl:text>
  <xsl:text>label = "</xsl:text>
  <xsl:value-of select="concat(@px:type, '/', @name)"/>
  <xsl:text>";&#10;</xsl:text>

  <xsl:apply-templates/>

  <xsl:text>}&#10;</xsl:text>
</xsl:template>

<xsl:template match="px:variable|px:option">
  <xsl:value-of select="concat('subgraph ', f:cluster(.), ' {&#10;')"/>
  <xsl:text>label = "</xsl:text>
  <xsl:value-of select="local-name(.)"/>
  <xsl:text>";&#10;</xsl:text>

  <xsl:value-of select="f:cluster(., 'name')"/>
  <xsl:value-of select="concat(' [label=&quot;$', @name, '&quot;]&#10;')"/>
  <xsl:value-of select="f:cluster(., 'select')"/>
  <xsl:value-of select="concat(' [label=&quot;', @select, '&quot;]&#10;')"/>

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

<xsl:template match="px:iteration-source">
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

<xsl:template match="px:xpath-context|px:name-pipe">
  <xsl:apply-templates/>
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

<xsl:template match="element()" mode="links">
  <xsl:apply-templates mode="links"/>
</xsl:template>

<xsl:template match="attribute()|text()|comment()|processing-instruction()"
              mode="links"/>

<xsl:template match="px:pipe" mode="links">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="@px:port"/>
  <xsl:text>" -&gt; </xsl:text>

  <xsl:text>"</xsl:text>
  <xsl:value-of select="@px:id"/>
  <xsl:text>";&#10;</xsl:text>
</xsl:template>

<xsl:template match="px:variable/px:xpath-context/px:pipe" mode="links" priority="10">
  <xsl:text>"</xsl:text>
  <xsl:value-of select="@px:port"/>
  <xsl:text>" -&gt; </xsl:text>

  <xsl:value-of select="f:cluster(../.., 'select')"/>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="px:name-pipe" mode="links">
  <xsl:variable name="decl" select="@px:decl"/>

  <xsl:value-of select="f:cluster(//*[@px:id=$decl], 'name')"/>

  <xsl:text> -&gt; </xsl:text>

  <xsl:value-of select="f:cluster(../.., 'select')"/>

  <xsl:text>&#10;</xsl:text>
</xsl:template>

<!-- ============================================================ -->

<xsl:function name="f:cluster">
  <xsl:param name="node"/>
  <xsl:value-of select="f:cluster($node, '')"/>
</xsl:function>

<xsl:function name="f:cluster">
  <xsl:param name="node"/>
  <xsl:param name="suffix"/>
  <xsl:text>"cluster</xsl:text>
  <xsl:value-of select="$node/@px:id"/>
  <xsl:if test="$suffix != ''">
    <xsl:value-of select="concat('_', $suffix)"/>
  </xsl:if>
  <xsl:text>"</xsl:text>
</xsl:function>

</xsl:stylesheet>
