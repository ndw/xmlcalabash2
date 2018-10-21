<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                name="main"
                version="3.0">
  <p:output port="result"/>
  
  <p:identity name="producer">
    <p:with-input>
      <doc a='1' />
    </p:with-input>
  </p:identity>
  
  <p:identity name="id">
    <p:with-input port="source">
      <p:inline document-properties="map { 'a': /doc/@a }">
        <doc/>
      </p:inline>
    </p:with-input>
  </p:identity>

  <p:identity>
    <p:with-input port="source" select="p:document-properties-document(.)/p:document-properties/a"/>
  </p:identity>

</p:declare-step>
