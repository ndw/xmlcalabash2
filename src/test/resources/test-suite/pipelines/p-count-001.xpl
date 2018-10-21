<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                name="main"
                version="3.0">
  <p:option name="limit" select="0"/>
  <p:output port="result"/>

  <p:identity name="id1">
    <p:with-input port="source">
      <p:inline>
        <doc/>
      </p:inline>
    </p:with-input>
  </p:identity>

  <p:identity name="id2">
    <p:with-input port="source">
      <p:inline>
        <doc/>
      </p:inline>
    </p:with-input>
  </p:identity>

  <p:identity name="id3">
    <p:with-input port="source">
      <p:inline>
        <doc/>
      </p:inline>
    </p:with-input>
  </p:identity>

  <p:count limit="{$limit}">
    <p:with-input port="source" pipe="result@id1 result@id2 result@id3"/>
  </p:count>

</p:declare-step>
