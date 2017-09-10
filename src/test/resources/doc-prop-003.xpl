<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                name="main"
                version="1.0">
  <p:output port="result"/>

  <p:identity name="id">
    <p:input port="source">
      <p:document href="src/test/resources/calabash.png"
                  document-properties="{ map {{ 'a': 1 }} }"/>
    </p:input>
  </p:identity>

  <cx:option-value option="{p:document-properties(., 'a')}"/>

</p:declare-step>
