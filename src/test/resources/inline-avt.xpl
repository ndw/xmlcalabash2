<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:ex="http://xmlcalabash.com/ext/foo"
                name="main"
                version="1.0">
  <p:output port="result"/>

  <p:identity name="one">
    <p:input port="source">
      <p:document href="pipe.xpl"/>
    </p:input>
  </p:identity>

  <p:variable name="bar" select="count(//p:*)"/>

  <p:identity name="two">
    <p:input port="source">
      <p:inline>
        <doc count="{$bar}">Counts the number of nodes.
        The number of the nodes is <number>{$bar}</number>.
        <toggle class="{$bar}" p:expand-text="false">{$bar}</toggle>
        </doc>
      </p:inline>
    </p:input>
  </p:identity>

</p:declare-step>
