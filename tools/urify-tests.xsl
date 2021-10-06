<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="yes"/>
<xsl:strip-space elements="*"/>

<xsl:template match="cases">
  <xsl:apply-templates select="case" mode="windows"/>
  <xsl:apply-templates select="case" mode="non-windows"/>
</xsl:template>

<xsl:template match="case[@feature='windows']" priority="10" mode="non-windows"/>

<xsl:template match="case[@feature='windows']" priority="10" mode="windows">
  <xsl:call-template name="test-case">
    <xsl:with-param name="feature" select="'urify-windows'"/>
    <xsl:with-param name="name" select="'nw-urify-windows-'"/>
    <xsl:with-param name="num"
                    select="count(preceding::case[not(@feature='non-windows')])+1"/>
  </xsl:call-template>
</xsl:template>

<xsl:template match="case[@feature='non-windows']" priority="10" mode="non-windows">
  <xsl:call-template name="test-case">
    <xsl:with-param name="feature" select="'urify-non-windows'"/>
    <xsl:with-param name="name" select="'nw-urify-non-windows-'"/>
    <xsl:with-param name="num"
                    select="count(preceding::case[not(@feature='windows')])+1"/>
  </xsl:call-template>
</xsl:template>

<xsl:template match="case[@feature='non-windows']" priority="10" mode="windows"/>

<xsl:template match="case[@feature]">
  <xsl:message terminate="yes" select="'Unknown feature: ' || @feature"/>
</xsl:template>

<xsl:template match="case" mode="non-windows">
  <xsl:call-template name="test-case">
    <xsl:with-param name="feature" select="'urify-non-windows'"/>
    <xsl:with-param name="name" select="'nw-urify-non-windows-'"/>
    <xsl:with-param name="num"
                    select="count(preceding::case[not(@feature='windows')])+1"/>
  </xsl:call-template>
</xsl:template>

<xsl:template match="case" mode="windows">
  <xsl:call-template name="test-case">
    <xsl:with-param name="feature" select="'urify-windows'"/>
    <xsl:with-param name="name" select="'nw-urify-windows-'"/>
    <xsl:with-param name="num"
                    select="count(preceding::case[not(@feature='non-windows')])+1"/>
  </xsl:call-template>
</xsl:template>

<xsl:template name="test-case">
  <xsl:param name="name" as="xs:string" required="true"/>
  <xsl:param name="num" as="xs:integer" required="true"/>
  <xsl:param name="feature" as="xs:string" required="true"/>

  <xsl:result-document href="{$name}{format-number($num, '001')}.xml"
                       expand-text="yes">
    <t:test xmlns:t="http://xproc.org/ns/testsuite/3.0"
            features="{$feature}">
      <xsl:choose>
        <xsl:when test="@err">
          <xsl:namespace name="err" select="'http://www.w3.org/ns/xproc-error'"/>
          <xsl:attribute name="expected" select="'fail'"/>
          <xsl:attribute name="code" select="@err"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="expected" select="'pass'"/>
        </xsl:otherwise>
      </xsl:choose>
      <t:info>
        <t:title>{$name}{format-number($num, '001')}</t:title>
        <t:revision-history>
          <t:revision>
            <t:date>{string(@date)}</t:date>
            <t:author>
              <t:name>Norman Walsh</t:name>
            </t:author>
            <t:description xmlns="http://www.w3.org/1999/xhtml">
              <xsl:choose>
                <xsl:when test="$feature = 'urify-non-windows'">
                  <p>Test for the <code>p:urify()</code> function not on Windows.</p>
                </xsl:when>
                <xsl:otherwise>
                  <p>Test for the <code>p:urify()</code> function on Windows.</p>
                </xsl:otherwise>
              </xsl:choose>
            </t:description>
          </t:revision>
        </t:revision-history>
      </t:info>
      <t:description xmlns="http://www.w3.org/1999/xhtml">
        <p><xsl:sequence select="desc/node()"/></p>
      </t:description>
      <t:pipeline>
        <p:declare-step name="pipeline"
                        version="3.0"
                        xmlns:p="http://www.w3.org/ns/xproc"
                        xmlns:xs="http://www.w3.org/2001/XMLSchema">
          <p:output port="result"/>
          <p:identity>
            <p:with-input>
              <result>{{p:urify("{str}",
                                "{base}")}}</result>
            </p:with-input>
          </p:identity>
        </p:declare-step>
      </t:pipeline>
      <xsl:if test="empty(@err)">
        <t:schematron>
          <s:schema queryBinding="xslt2"
                    xmlns:s="http://purl.oclc.org/dsdl/schematron"
                    xmlns="http://www.w3.org/1999/xhtml">
            <s:pattern>
              <s:rule context="/">
                <s:assert test="result">Root element is not 'result'.</s:assert>
                <s:assert test="string(result)= '{result}'"
                          >Incorrect URI in result.</s:assert>
              </s:rule>
            </s:pattern>
          </s:schema>
        </t:schematron>
      </xsl:if>
    </t:test>
  </xsl:result-document>
</xsl:template>

</xsl:stylesheet>
