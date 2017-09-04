<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:ex="http://xmlcalabash.com/ext/foo"
                xmlns:exf="http://exproc.org/standard/functions"
                name="main"
                version="1.0">
  <p:output port="result"/>

  <p:identity>
    <p:input port="source">
      <doc><p/><p/><p/></doc>
    </p:input>
  </p:identity>

  <p:parameters parameters="{ map {{ 'p': count(//p) }} }"/>

</p:declare-step>
