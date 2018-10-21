<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                name="main"
                version="3.0">
  <p:output port="result"/>

  <p:variable name="a" select="3 + 4"/>

  <!-- this should fail, $a isn't a document -->
  <p:identity>
    <p:with-input port="source" select="p:document-properties-document($a)/p:document-properties/a">
      <p:empty/>
    </p:with-input>
  </p:identity>

</p:declare-step>
