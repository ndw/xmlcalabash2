<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           version="3.0">

<p:function name="p:system-property"/>
<p:function name="p:document-properties"/>
<p:function name="cx:cwd"/>

<p:declare-step type="p:add-attribute">
  <p:input port="source" content-types="application/xml"/>
  <p:output port="result" content-types="application/xml"/>
  <p:option name="match" cx:as="XSLTMatchPattern" select="'/*'"/>
  <p:option name="attribute-name" required="true" as="xs:QName"/>
  <p:option name="attribute-value" required="true" as="xs:string"/>
</p:declare-step>

<p:declare-step type="p:cast-content-type">
  <p:input port="source" content-types="*/*"/>
  <p:output port="result" content-types="*/*"/>
  <p:option name="content-type" as="xs:string"/>
  <p:option name="parameters" as="map(xs:QName,item()*)?"/>
</p:declare-step>

<p:declare-step type="p:count">
  <p:input port="source" content-types="*/*" sequence="true"/>
  <p:output port="result" content-types="application/xml"/>
  <p:option name="limit" select="0" as="xs:integer"/>
</p:declare-step>

<p:declare-step type="p:directory-list">
  <p:output port="result" content-types="application/xml"/>
  <p:option name="path" required="true" as="xs:anyURI"/>
  <p:option name="detailed" select="false()" as="xs:boolean"/>
  <p:option name="include-filter" as="xs:string*" cx:as="RegularExpression"/>
  <p:option name="exclude-filter" as="xs:string*" cx:as="RegularExpression"/>
</p:declare-step>

<p:declare-step type="p:delete">
  <p:input port="source" sequence="true" content-types="*/*"/>
  <p:output port="result" sequence="true" content-types="*/*"/>
  <p:option name="match" select="'/*'" as="xs:string" cx:as="XSLTSelectionPattern"/>
</p:declare-step>

<p:declare-step type="p:error">
  <p:input port="source" sequence="true" content-types="*/*"/>
  <p:output port="result" sequence="true" content-types="*/*"/>
  <p:option name="code" required="true" as="xs:QName"/>
</p:declare-step>

<p:declare-step type="p:escape-markup">
  <p:input port="source" content-types="xml html"/>
  <p:output port="result" content-types="xml html"/>
  <p:option name="serialization" as="map(xs:QName,item()*)?"/>
</p:declare-step>

<p:declare-step type="p:hash">
  <p:input port="source" primary="true" content-types="xml html"/>
  <p:output port="result" content-types="text xml html"/>
  <p:option name="parameters" as="map(xs:QName,item()*)?"/>
  <p:option name="value" required="true" as="xs:string"/>
  <p:option name="algorithm" required="true" as="xs:QName"/>
  <p:option name="match" as="xs:string" select="'/*/node()'" cx:as="XSLTSelectionPattern"/>
  <p:option name="version" as="xs:string"/>
</p:declare-step>

<p:declare-step type="p:identity">
  <p:input port="source" content-types="*/*" sequence="true"/>
  <p:output port="result" content-types="*/*" sequence="true"/>
</p:declare-step>

<p:declare-step type="p:insert">
<p:input port="source" primary="true" content-types="application/xml text/xml */*+xml"/>
<p:input port="insertion" sequence="true" content-types="application/xml text/* */*+xml"/>
<p:output port="result" content-types="application/xml"/>
<p:option name="match" select="'/*'" as="xs:string" cx:as="XSLTSelectionPattern"/>
<p:option name="position" required="true" as="xs:token"
          cx:as="first-child|last-child|before|after"/>
</p:declare-step>

<p:declare-step type="p:json-join">
  <p:input port="source" sequence="true" content-types="json"/>
  <p:output port="result" content-types="application/json"/>
  <p:option name="flatten-arrays" as="xs:boolean" select="false()"/>
</p:declare-step>

<p:declare-step type="p:json-merge">
  <p:input port="source" sequence="true" content-types="json"/>
  <p:output port="result" content-types="application/json"/>
</p:declare-step>

<p:declare-step type="p:label-elements">
  <p:input port="source" content-types="xml html"/>
  <p:output port="result" content-types="xml html"/>
  <p:option name="attribute" as="xs:QName" select="'xml:id'"/>
  <p:option name="label" as="xs:string" select="'concat(''_'',$p:index)'"/>
  <p:option name="match" as="xs:string" select="'*'"/>
  <p:option name="replace" as="xs:boolean" select="true()"/>
</p:declare-step>

<p:declare-step type="p:load">
  <p:output port="result" sequence="true" content-types="*/*"/>
  <p:option name="href" required="true" as="xs:anyURI"/>
  <p:option name="parameters" as="map(xs:QName,item()*)?"/>
  <p:option name="content-type" as="xs:string"/>
  <p:option name="document-properties" as="map(xs:QName,item()*)?"/>
</p:declare-step>

<p:declare-step type="p:namespace-rename">
  <p:input port="source" content-types="xml html"/>
  <p:output port="result" content-types="xml html"/>
  <p:option name="from" as="xs:anyURI"/>
  <p:option name="to" as="xs:anyURI"/>
  <p:option name="apply-to" as="xs:token" select="'all'"
            values="('all','elements','attributes')"/>
</p:declare-step>

<p:declare-step type="p:pack">
  <p:input port="source" content-types="text xml html" sequence="true" primary="true"/>
  <p:input port="alternate" sequence="true" content-types="text xml html"/>
  <p:output port="result" sequence="true" content-types="application/xml"/>
  <p:option name="wrapper" required="true" as="xs:QName"/>
</p:declare-step>

<p:declare-step type="p:parameters">
  <p:output port="result" content-types="application/xml"/>
  <p:option name="parameters" as="map(xs:QName,item()*)?"/>
</p:declare-step>

<p:declare-step type="p:rename">
   <p:input port="source" content-types="xml html"/>
   <p:output port="result" content-types="xml html"/>
   <p:option name="match" as="xs:string" select="'/*'"/>
   <p:option name="new-name" required="true" as="xs:QName"/>
</p:declare-step>

<p:declare-step type="p:set-attributes">
  <p:input port="source" primary="true" content-types="xml html"/>
  <p:output port="result" content-types="xml html"/>
  <p:option name="match" as="xs:string" select="'/*'"/>
  <p:option name="attributes" required="true" as="map(xs:QName, xs:anyAtomicType)"/>
</p:declare-step>

<p:declare-step type="p:set-properties">
  <p:input port="source" content-types="any"/>
  <p:output port="result" content-types="any"/>
  <p:option name="properties" required="true" as="map(xs:QName,item()*)"/>
  <p:option name="merge" select="false()" as="xs:boolean"/>
</p:declare-step>

<p:declare-step type="p:sink">
  <p:input port="source" content-types="any" sequence="true"/>
</p:declare-step>

<p:declare-step type="p:store">
     <p:input port="source" content-types="any"/>
     <p:output port="result" content-types="any" primary="true"/>
     <p:output port="result-uri" content-types="application/xml"/>
     <p:option name="href" required="true" as="xs:anyURI"/>
     <p:option name="serialization" as="map(xs:QName,item()*)?"/>
</p:declare-step>

<p:declare-step type="p:text-count">
  <p:input port="source" content-types="text"/>
  <p:output port="result" content-types="application/xml"/>
</p:declare-step>

<p:declare-step type="p:text-head">
  <p:input port="source" primary="true" sequence="false" content-types="text"/>
  <p:output port="result" primary="true" sequence="false" content-types="text"/>
  <p:option name="count" required="true" as="xs:integer"/>
</p:declare-step>

<p:declare-step type="p:text-join">
  <p:input port="source" sequence="true" content-types="text"/>
  <p:output port="result" sequence="false" content-types="text"/>
  <p:option name="separator" required="false" as="xs:string"/>
  <p:option name="prefix" required="false" as="xs:string"/>
  <p:option name="suffix" required="false" as="xs:string"/>
  <p:option name="override-content-type" required="false" as="xs:string?"/>
</p:declare-step>

<p:declare-step type="p:text-replace">
  <p:input port="source" primary="true" sequence="false" content-types="text"/>
  <p:output port="result" primary="true" sequence="false" content-types="text"/>
  <p:option name="pattern" required="true" as="xs:string"/>
  <p:option name="replacement" required="true" as="xs:string"/>
  <p:option name="flags" required="false" as="xs:string"/>
</p:declare-step>

<p:declare-step type="p:text-sort">
  <p:input port="source" content-types="text"/>
  <p:output port="result" content-types="text"/>
  <p:option name="order" required="false" as="xs:string" select="'ascending'"
            values="('ascending', 'descending')"/>
  <p:option name="case-order" required="false" as="xs:string"
            values="('upper-first', 'lower-first')"/>
  <p:option name="lang" required="false" as="xs:language"/>
  <p:option name="data-type" required="false" as="xs:string" select="'text'"
            values="('text', 'number')"/>
  <p:option name="collation" required="false" as="xs:string"
            select="'https://www.w3.org/2005/xpath-functions/collation/codepoint'"/>
  <p:option name="stable" required="false" as="xs:boolean" select="true()"/>
</p:declare-step>

<p:declare-step type="p:text-tail">
  <p:input port="source" primary="true" sequence="false" content-types="text"/>
  <p:output port="result" primary="true" sequence="false" content-types="text"/>
  <p:option name="count" required="true" as="xs:integer"/>
</p:declare-step>

<p:declare-step type="p:unescape-markup">
  <p:input port="source" content-types="xml html"/>
  <p:output port="result" content-types="xml html"/>
  <p:option name="namespace" as="xs:anyURI?"/>
  <p:option name="content-type" as="xs:string" select="'application/xml'"/>
  <p:option name="encoding" as="xs:string?"/>
  <p:option name="charset" as="xs:string?"/>
</p:declare-step>

<p:declare-step type="p:unwrap">
  <p:input port="source" content-types="xml html"/>
  <p:output port="result" sequence="true" content-types="xml text/plain"/>
  <p:option name="match" cx:as="XSLTMatchPattern" select="'/*'"/>
</p:declare-step>

<p:declare-step type="p:uuid">
  <p:input port="source" primary="true" content-types="xml html"/>
  <p:output port="result" content-types="text xml html"/>
  <p:option name="match" as="xs:string" select="'/*'"/>
  <p:option name="version" as="xs:integer?"/>
</p:declare-step>

<p:declare-step type="p:validate-with-relax-ng">
  <p:input port="source" primary="true" content-types="xml html"/>
  <p:input port="schema" content-types="xml text"/>
  <p:output port="result" content-types="application/xml"/>
  <p:option name="dtd-attribute-values" select="false()" as="xs:boolean"/>
  <p:option name="dtd-id-idref-warnings" select="false()" as="xs:boolean"/>
  <p:option name="assert-valid" select="true()" as="xs:boolean"/>
</p:declare-step>

<p:declare-step type="p:validate-with-schematron">
  <p:input port="source" primary="true" content-types="xml html"/>
  <p:input port="schema" content-types="xml"/>
  <p:output port="result" primary="true" content-types="application/xml"/>
  <p:output port="report" sequence="true" content-types="application/xml"/>
  <p:option name="parameters" as="map(xs:QName,item())?"/>
  <p:option name="phase" select="'#ALL'" as="xs:string"/>
  <p:option name="assert-valid" select="true()" as="xs:boolean"/>
</p:declare-step>

<p:declare-step type="p:validate-with-xml-schema">
  <p:input port="source" primary="true" content-types="xml html"/>
  <p:input port="schema" sequence="true" content-types="xml"/>
  <p:output port="result" content-types="application/xml" primary="true"/>
  <p:output port="report" content-types="application/xml"/>
  <p:option name="use-location-hints" select="false()" as="xs:boolean"/>
  <p:option name="try-namespaces" select="false()" as="xs:boolean"/>
  <p:option name="assert-valid" select="true()" as="xs:boolean"/>
  <p:option name="mode" select="'strict'" as="xs:token" cx:as="strict|lax"/>
  <p:option name="version" as="xs:string"/>
</p:declare-step>

<p:declare-step type="p:wrap-sequence">
  <p:input port="source" content-types="application/xml */*+xml text/*" sequence="true"/>
  <p:output port="result" sequence="true" content-types="application/xml"/>
  <p:option name="wrapper" required="true" as="xs:QName"/>
  <p:option name="group-adjacent" as="xs:string" cx:as="XPathExpression"/>
</p:declare-step>

<p:declare-step type="p:xslt">
  <p:input port="source" content-types="application/xml text/xml */*+xml" sequence="true" primary="true"/>
  <p:input port="stylesheet" content-types="application/xml text/xml */*+xml"/>
  <p:output port="result" primary="true" sequence="true" content-types="*/*"/>
  <p:output port="secondary" sequence="true"/>
  <p:option name="parameters" as="map(xs:QName,item()*)?"/>
  <p:option name="initial-mode" as="xs:QName"/>
  <p:option name="template-name" as="xs:QName"/>
  <p:option name="output-base-uri" as="xs:anyURI"/>
  <p:option name="version" as="xs:string"/>
</p:declare-step>

<!-- ============================================================ -->

<p:declare-step type="cx:exception-translator">
  <p:input port="source" content-types="*/*" sequence="true"/>
  <p:output port="result" content-types="*/*" sequence="true"/>
</p:declare-step>

<p:declare-step type="cx:base64-encode">
  <p:input port="source" content-types="*/*"/>
  <p:output port="result" content-types="text/plain"/>
  <p:option name="serialization" as="map(xs:QName,item()*)?"/>
  <p:option name="parameters" as="map(xs:QName,item()*)?"/>
</p:declare-step>

<p:declare-step type="cx:base64-decode">
  <p:input port="source" content-types="*/*"/>
  <p:output port="result" content-types="*/*"/>
  <p:option name="parameters" as="map(xs:QName,item()*)?"/>
</p:declare-step>

<p:declare-step type="cx:markdown">
  <p:input port="source" content-types="text/*"/>
  <p:output port="result" content-types="application/xml+html"/>
  <p:option name="parameters" as="map(xs:QName,item()*)?"/>
</p:declare-step>

<p:declare-step type="cx:javascript">
  <p:input port="script"/>
  <p:output port="result"/>
  <p:option name="parameters" as="map(xs:QName,item()*)?"/>
</p:declare-step>

<p:declare-step type="cx:peephole">
  <p:input port="source"/>
  <p:output port="result"/>
</p:declare-step>

<p:declare-step type="cx:option-value">
  <p:output port="result"/>
  <p:option name="option"/>
</p:declare-step>

<p:declare-step type="cx:content-type-checker">
  <p:input port="source" sequence="true"/>
  <p:output port="result" sequence="true"/>
</p:declare-step>

<p:declare-step type="cx:select-filter">
  <p:input port="source" sequence="true"/>
  <p:output port="result" sequence="true"/>
</p:declare-step>

<p:declare-step type="cx:document-loader">
  <p:input port="source" sequence="true"/>
  <p:output port="result" sequence="true"/>
</p:declare-step>

<p:declare-step type="cx:inline-loader">
  <p:input port="source" sequence="true"/>
  <p:output port="result" sequence="true"/>
</p:declare-step>

<p:declare-step type="cx:empty-loader">
  <p:output port="result" sequence="true"/>
</p:declare-step>

</p:library>
