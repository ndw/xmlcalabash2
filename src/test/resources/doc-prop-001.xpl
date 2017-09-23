<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                name="main"
                version="1.0">
  <p:output port="result"/>

  <p:identity name="id">
    <p:with-input port="source">
      <p:inline document-properties="map { 'a': 1 }">
        <doc/>
      </p:inline>
    </p:with-input>
  </p:identity>

  <cx:option-value option="{p:document-properties(., 'a')}"/>

</p:declare-step>
