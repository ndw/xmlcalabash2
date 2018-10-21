<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                name="main"
                version="3.0">
  <p:output port="result"/>

  <p:parameters>
    <p:with-option name="parameters" select="map { 'p': count(//p) }">
      <p:inline>
        <doc><p/><p/><p/></doc>
      </p:inline>
    </p:with-option>
  </p:parameters>

</p:declare-step>
