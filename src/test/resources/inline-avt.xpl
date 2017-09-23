<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:ex="http://xmlcalabash.com/ext/foo"
                name="main"
                version="1.0">
  <p:output port="result"/>

  <p:identity name="one">
    <p:with-input port="source">
      <p:inline>
        <doc>There are <t>elements</t> in here. A couple of <t>elements</t>.</doc>
      </p:inline>
    </p:with-input>
  </p:identity>

  <p:variable name="bar" select="count(//*)"/>

  <p:identity name="two">
    <p:with-input port="source">
      <p:inline>
        <doc count="{$bar}">Counts the number of nodes.
        The number of the nodes is <number>{$bar}</number>.
        <toggle class="{$bar}" p:expand-text="false">{$bar}</toggle>
        </doc>
      </p:inline>
    </p:with-input>
  </p:identity>

</p:declare-step>
