<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                name="main"
                version="3.0">
  <p:output port="result"/>

  <p:identity>
    <p:with-input port="source">
      <doc><p/><p/><p/></doc>
    </p:with-input>
  </p:identity>

  <p:parameters parameters="map { 'p': count(//p) }"/>

</p:declare-step>