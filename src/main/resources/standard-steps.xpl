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
  <p:option name="match" cx:as="XSLTMatchPattern"/>
  <p:option name="attribute-name" required="true" as="xs:QName"/>
  <p:option name="attribute-prefix" as="xs:NCName"/>
  <p:option name="attribute-namespace" as="xs:anyURI"/>
  <p:option name="attribute-value" required="true" as="xs:string"/>
</p:declare-step>

<p:declare-step type="p:cast-content-type">
  <p:input port="source" content-types="*/*"/>
  <p:output port="result" content-types="*/*"/>
  <p:option name="content-type" as="xs:string"/>
  <p:option name="parameters" as="map(*)"/>
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

<p:declare-step type="p:error">
  <p:input port="source" sequence="true" content-types="*/*"/>
  <p:output port="result" sequence="true" content-types="*/*"/>
  <p:option name="code" required="true" as="xs:QName"/>
  <p:option name="code-prefix" as="xs:NCName"/>
  <p:option name="code-namespace" as="xs:anyURI"/>
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

<p:declare-step type="p:load">
  <p:output port="result" sequence="true" content-types="*/*"/>
  <p:option name="href" required="true" as="xs:anyURI"/>
  <p:option name="parameters" as="map(*)"/>
  <p:option name="content-type" as="xs:string"/>
  <p:option name="document-properties" as="map(*)"/>
</p:declare-step>

<p:declare-step type="p:parameters">
  <p:output port="result" content-types="application/xml"/>
  <p:option name="parameters" as="map(*)"/>
</p:declare-step>

<p:declare-step type="p:xslt">
  <p:input port="source" content-types="application/xml text/xml */*+xml" sequence="true" primary="true"/>
  <p:input port="stylesheet" content-types="application/xml text/xml */*+xml"/>
  <p:output port="result" primary="true" sequence="true" content-types="*/*"/>
  <p:output port="secondary" sequence="true"/>
  <p:option name="parameters" as="map(*)"/>
  <p:option name="initial-mode" as="xs:QName"/>
  <p:option name="template-name" as="xs:QName"/>
  <p:option name="output-base-uri" as="xs:anyURI"/>
  <p:option name="version" as="xs:string"/>
</p:declare-step>

<p:declare-step type="p:wrap-sequence">
  <p:input port="source" content-types="application/xml */*+xml text/*" sequence="true"/>
  <p:output port="result" sequence="true" content-types="application/xml"/>
  <p:option name="wrapper" required="true" as="xs:QName"/>      
  <p:option name="wrapper-prefix" as="xs:NCName"/>              
  <p:option name="wrapper-namespace" as="xs:anyURI"/>           
  <p:option name="group-adjacent" as="xs:string" cx:as="XPathExpression"/>
</p:declare-step>

<!--============================================================ -->

<p:declare-step type="cx:base64-encode">
  <p:input port="source" content-types="*/*"/>
  <p:output port="result" content-types="text/plain"/>
  <p:option name="serialization" as="map(*)"/>
  <p:option name="parameters" as="map(*)"/>
</p:declare-step>

<p:declare-step type="cx:base64-decode">
  <p:input port="source" content-types="*/*"/>
  <p:output port="result" content-types="*/*"/>
  <p:option name="parameters" as="map(*)"/>
</p:declare-step>

<p:declare-step type="cx:javascript">
  <p:input port="script"/>
  <p:output port="result"/>
  <p:option name="parameters" as="map(*)"/>
</p:declare-step>

<p:declare-step type="cx:peephole">
  <p:input port="source"/>
  <p:output port="result"/>
</p:declare-step>

<p:declare-step type="cx:option-value">
  <p:output port="result"/>
  <p:option name="option"/>
</p:declare-step>

<p:declare-step type="cx:property-extract">
  <p:input port="source"/>
  <p:output port="result" primary="true"/>
  <p:output port="properties"/>
  <p:option name=""/>
</p:declare-step>

<p:declare-step type="cx:property-merge">
  <p:input port="source" primary="true"/>
  <p:input port="properties"/>
  <p:output port="result"/>
</p:declare-step>

<p:declare-step type="cx:content-type-checker">
  <p:input port="source" sequence="true"/>
  <p:output port="result" sequence="true"/>
</p:declare-step>

</p:library>
