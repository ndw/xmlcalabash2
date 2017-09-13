<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:ex="http://xmlcalabash.com/ext/foo"
                xmlns:exf="http://exproc.org/standard/functions"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                name="main"
                version="1.0">
  <p:option name="match"/>
  <p:output port="result"/>

  <p:identity name="id">
    <p:input port="source">
      <p:inline document-properties="{ map {{ 'a': '1', 'b': '2' }} }">
        <doc/>
      </p:inline>
    </p:input>
  </p:identity>

  <p:choose name="choose">
    <p:when test="p:document-properties(., 'a') = $match">
      <p:identity>
        <p:input port="source">
          <p:inline><doc>one</doc></p:inline>
        </p:input>
      </p:identity>
    </p:when>
    <p:when test="p:document-properties(., 'b') = $match">
      <p:identity>
        <p:input port="source">
          <p:inline><doc>two</doc></p:inline>
        </p:input>
      </p:identity>
    </p:when>
  </p:choose>

</p:declare-step>
