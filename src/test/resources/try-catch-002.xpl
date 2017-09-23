<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:ex="http://xmlcalabash.com/ext/foo"
                xmlns:exf="http://exproc.org/standard/functions"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                name="main"
                version="1.0">
  <p:option name="error"/>
  <p:output port="result">
    <p:pipe step="try" port="finally"/>
  </p:output>

  <p:try name="try">
    <p:group>
      <p:choose>
        <p:when test="$error = 0">
          <p:identity>
            <p:with-input port="source">
              <p:inline><doc>try succeeded</doc></p:inline>
            </p:with-input>
          </p:identity>
        </p:when>
        <p:when test="$error = 1">
          <p:error code="cx:error"/>
        </p:when>
        <p:otherwise>
          <p:error code="cx:error2"/>
        </p:otherwise>
      </p:choose>
    </p:group>
    <p:catch code="cx:error">
      <p:identity>
        <p:with-input port="source">
          <p:inline><doc>caught cx:error</doc></p:inline>
        </p:with-input>
      </p:identity>
    </p:catch>
    <p:catch>
      <p:identity>
        <p:with-input port="source">
          <p:inline><doc>caught any</doc></p:inline>
        </p:with-input>
      </p:identity>
    </p:catch>
    <p:finally>
      <p:output port="finally" primary="false"/>
      <p:identity>
        <p:with-input port="source">
          <p:inline><doc>finally</doc></p:inline>
        </p:with-input>
      </p:identity>
    </p:finally>
  </p:try>

</p:declare-step>
