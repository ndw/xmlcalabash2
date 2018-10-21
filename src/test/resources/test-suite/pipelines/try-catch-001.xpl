<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                name="main"
                version="3.0">
                <!-- we should replace cx with something better -->
  <p:option name="error"/>
  <p:output port="result"/>

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
          <p:error code="cx:error">
            <p:with-input port="source">
              <p:empty/>
            </p:with-input>
          </p:error>
        </p:when>
        <p:otherwise>
          <p:error code="cx:error2">
            <p:with-input port="source">
              <p:empty/>
            </p:with-input>
          </p:error>
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
      <p:output port="finally" primary="false">
        <p:pipe step="fident" port="result"/>
      </p:output>
      <p:identity name="fident">
        <p:with-input port="source">
          <p:inline><doc>finally</doc></p:inline>
        </p:with-input>
      </p:identity>
    </p:finally>
  </p:try>

</p:declare-step>
