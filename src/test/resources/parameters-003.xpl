<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:ex="http://xmlcalabash.com/ext/foo"
                xmlns:exf="http://exproc.org/standard/functions"
                name="main"
                version="1.0">
  <p:output port="result"/>

  <p:identity name="fred">
    <p:input port="source">
      <p:inline>
        <doc><p/><p/><p/><p/></doc>
      </p:inline>
    </p:input>
  </p:identity>

  <p:parameters>
    <p:with-option name="parameters" select="map { 'p': count(//p) }">
      <p:pipe step="fred" port="result"/>
    </p:with-option>
  </p:parameters>

</p:declare-step>
