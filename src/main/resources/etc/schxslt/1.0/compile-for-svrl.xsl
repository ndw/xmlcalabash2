<xsl:transform version="1.0"
               xmlns="http://www.w3.org/1999/XSL/TransformAlias"
               xmlns:sch="http://purl.oclc.org/dsdl/schematron"
               xmlns:schxslt="https://doi.org/10.5281/zenodo.1495494"
               xmlns:schxslt-api="https://doi.org/10.5281/zenodo.1495494#api"
               xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
               xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:import href="compile/compile-1.0.xsl"/>

  <xsl:template name="schxslt-api:report">
    <xsl:param name="schema"/>
    <xsl:param name="phase"/>

    <svrl:schematron-output>
      <xsl:copy-of select="$schema/@schemaVersion"/>
      <xsl:if test="$phase != '#ALL'">
        <xsl:attribute name="phase"><xsl:value-of select="$phase"/></xsl:attribute>
      </xsl:if>
      <xsl:if test="$schema/sch:title">
        <xsl:attribute name="title"><xsl:value-of select="$schema/sch:title"/></xsl:attribute>
      </xsl:if>
      <xsl:for-each select="$schema/sch:p">
        <svrl:text>
          <xsl:copy-of select="@id | @class | @icon"/>
          <xsl:apply-templates select="node()" mode="schxslt:compile"/>
        </svrl:text>
      </xsl:for-each>

      <xsl:for-each select="$schema/sch:ns">
        <svrl:ns-prefix-in-attribute-values>
          <xsl:copy-of select="@prefix | @uri"/>
        </svrl:ns-prefix-in-attribute-values>
      </xsl:for-each>

      <copy-of select="$schxslt:report"/>

    </svrl:schematron-output>

  </xsl:template>

  <xsl:template name="schxslt-api:active-pattern">
    <xsl:param name="pattern"/>

    <svrl:active-pattern>
      <xsl:copy-of select="$pattern/@id | $pattern/@role"/>
      <xsl:if test="$pattern/@documents">
        <attribute name="documents"><value-of select="normalize-space()"/></attribute>
      </xsl:if>
    </svrl:active-pattern>

  </xsl:template>

  <xsl:template name="schxslt-api:fired-rule">
    <xsl:param name="rule"/>

    <svrl:fired-rule>
      <xsl:copy-of select="$rule/@id | $rule/@role | $rule/@flag"/>
      <attribute name="context">
        <xsl:value-of select="$rule/@context"/>
      </attribute>
    </svrl:fired-rule>
  </xsl:template>

  <xsl:template name="schxslt-api:failed-assert">
    <xsl:param name="assert"/>

    <variable name="location">
      <call-template name="schxslt:location">
        <xsl:choose>
          <xsl:when test="$assert/@subject">
            <with-param name="node" select="{$assert/@subject}"/>
          </xsl:when>
          <xsl:when test="$assert/../@subject">
            <with-param name="node" select="{$assert/../@subject}"/>
          </xsl:when>
          <xsl:otherwise>
            <with-param name="node" select="."/>
          </xsl:otherwise>
        </xsl:choose>
      </call-template>
    </variable>
    <svrl:failed-assert location="{{normalize-space($location)}}">
      <xsl:copy-of select="$assert/@role | $assert/@flag | $assert/@id"/>
      <attribute name="test">
        <xsl:value-of select="$assert/@test"/>
      </attribute>
      <xsl:call-template name="schxslt:detailed-report"/>
    </svrl:failed-assert>
  </xsl:template>

  <xsl:template name="schxslt-api:successful-report">
    <xsl:param name="report"/>

    <variable name="location">
      <call-template name="schxslt:location">
        <xsl:choose>
          <xsl:when test="$report/@subject">
            <with-param name="node" select="{$report/@subject}"/>
          </xsl:when>
          <xsl:when test="$report/../@subject">
            <with-param name="node" select="{$report/../@subject}"/>
          </xsl:when>
          <xsl:otherwise>
            <with-param name="node" select="."/>
          </xsl:otherwise>
        </xsl:choose>
      </call-template>
    </variable>
    <svrl:successful-report location="{{normalize-space($location)}}">
      <xsl:copy-of select="$report/@role | $report/@flag | $report/@id"/>
      <attribute name="test">
        <xsl:value-of select="$report/@test"/>
      </attribute>
      <xsl:call-template name="schxslt:detailed-report"/>
    </svrl:successful-report>
  </xsl:template>

  <xsl:template name="schxslt-api:validation-stylesheet-body-bottom-hook">
    <xsl:param name="schema"/>
    <xsl:copy-of select="document('')/xsl:transform/xsl:template[@name = 'schxslt:location']"/>
  </xsl:template>

  <xsl:template name="schxslt-api:metadata">
    <xsl:param name="schema"/>
    <xsl:param name="source"/>
    <svrl:metadata xmlns:dct="http://purl.org/dc/terms/">
      <dct:source>
        <xsl:copy-of select="$source"/>
      </dct:source>
    </svrl:metadata>
  </xsl:template>

  <xsl:template name="schxslt:detailed-report">
    <xsl:if test="@diagnostics">
      <xsl:call-template name="schxslt:copy-diagnostics"/>
    </xsl:if>
    <xsl:if test="@properties">
      <xsl:call-template name="schxslt:copy-properties"/>
    </xsl:if>
    <xsl:if test="text() | *">
      <svrl:text>
        <xsl:apply-templates select="node()" mode="schxslt:compile"/>
      </svrl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template name="schxslt:copy-diagnostics">
    <xsl:param name="sequence" select="normalize-space(@diagnostics)"/>

    <xsl:variable name="head">
      <xsl:choose>
        <xsl:when test="contains($sequence, ' ')">
          <xsl:value-of select="substring-before($sequence, ' ')"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$sequence"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <svrl:diagnostic-reference diagnostic="{$head}">
      <svrl:text>
        <xsl:copy-of select="key('schxslt:diagnostics', $head)/@*"/>
        <xsl:apply-templates select="key('schxslt:diagnostics', $head)/node()" mode="schxslt:compile"/>
      </svrl:text>
    </svrl:diagnostic-reference>

    <xsl:choose>
      <xsl:when test="contains($sequence, ' ')">
        <xsl:call-template name="schxslt:copy-diagnostics">
          <xsl:with-param name="sequence" select="substring-after($sequence, ' ')"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise/>
    </xsl:choose>

  </xsl:template>

  <xsl:template name="schxslt:copy-properties">
    <xsl:param name="sequence" select="normalize-space(@properties)"/>

    <xsl:variable name="head">
      <xsl:choose>
        <xsl:when test="contains($sequence, ' ')">
          <xsl:value-of select="substring-before($sequence, ' ')"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$sequence"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <svrl:property-reference property="{$head}">
      <xsl:copy-of select="key('schxslt:properties', $head)/@role"/>
      <xsl:copy-of select="key('schxslt:properties', $head)/@schema"/>
      <xsl:for-each select="key('schxslt:properties', $head)/node()">
        <xsl:choose>
          <xsl:when test="self::text() and normalize-space(.)">
            <svrl:text>
              <xsl:apply-templates select="." mode="schxslt:compile"/>
            </svrl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:copy-of select="."/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:for-each>
    </svrl:property-reference>

    <xsl:choose>
      <xsl:when test="contains($sequence, ' ')">
        <xsl:call-template name="schxslt:copy-properties">
          <xsl:with-param name="sequence" select="substring-after($sequence, ' ')"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise/>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="schxslt:location">
    <xsl:param name="node"/>

    <xsl:variable name="path">
      <xsl:for-each select="$node/ancestor::*">
        <xsl:variable name="position">
          <xsl:number level="single"/>
        </xsl:variable>
        <xsl:text>/</xsl:text>
        <xsl:value-of select="concat('Q{', namespace-uri(.), '}', local-name(.), '[', $position, ']')"/>
      </xsl:for-each>
      <xsl:text>/</xsl:text>
      <xsl:variable name="position">
        <xsl:number level="single"/>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="$node/self::*">
          <xsl:value-of select="concat('Q{', namespace-uri($node), '}', local-name($node), '[', $position, ']')"/>
        </xsl:when>
        <xsl:when test="count($node/../@*) = count($node|$node/../@*)">
          <xsl:value-of select="concat('@Q{', namespace-uri($node), '}', local-name($node))"/>
        </xsl:when>
        <xsl:when test="$node/self::processing-instruction()">
          <xsl:value-of select="concat('processing-instruction(&quot;', name(.), '&quot;)', '[', $position, ']')"/>
        </xsl:when>
        <xsl:when test="$node/self::comment()">
          <xsl:value-of select="concat('comment()', '[', $position, ']')"/>
        </xsl:when>
        <xsl:when test="$node/self::text()">
          <xsl:value-of select="concat('text()', '[', $position, ']')"/>
        </xsl:when>
      </xsl:choose>
    </xsl:variable>

    <xsl:value-of select="$path"/>
  </xsl:template>

</xsl:transform>
