<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                version="3.0">
  <p:output port="result" sequence="true"/>
  <p:option name="blank" select="''"/>
  <p:option name="class" select="concat($blank, 'b')"/>

  <p:identity>
    <p:with-input port="source" select="//c:chapter[@class=$class]">
      <p:inline>
        <document xmlns="http://www.w3.org/ns/xproc-step">
          <chapter class="a">one</chapter>
          <chapter class="a">two</chapter>
          <chapter class="b">three</chapter>
        </document>
      </p:inline>
    </p:with-input>
  </p:identity>

  <p:wrap-sequence wrapper="c:result"/>
</p:declare-step>
