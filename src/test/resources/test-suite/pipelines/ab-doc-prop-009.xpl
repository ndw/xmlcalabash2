<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                name="main"
                version="3.0">
  <p:output port="result"/>
  
  <p:variable static="true" name="var" select="/doc/@att">
    <doc att='1' />
  </p:variable>
  
  <p:input port="source">
    <p:inline document-properties="map { 'a': $var }">
      <doc/>
    </p:inline>
  </p:input>
  
  <p:identity>
    <p:with-input port="source" select="p:document-properties-document(.)/c:document-properties/a"/>
  </p:identity>

</p:declare-step>
