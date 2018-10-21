<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                name="main"
                version="3.0">
  <p:option name="match"/>
  <p:output port="result"/>

  <p:identity name="id">
    <p:with-input port="source">
      <p:inline document-properties="map { 'a': '1', 'b': '2' }">
        <doc/>
      </p:inline>
    </p:with-input>
  </p:identity>

  <p:choose name="choose">
    <p:when test="p:document-property(., 'a') = $match">
      <p:identity>
        <p:with-input port="source">
          <p:inline><doc>one</doc></p:inline>
        </p:with-input>
      </p:identity>
    </p:when>
    <p:when test="p:document-property(., 'b') = $match">
      <p:identity>
        <p:with-input port="source">
          <p:inline><doc>two</doc></p:inline>
        </p:with-input>
      </p:identity>
    </p:when>
  </p:choose>

</p:declare-step>
