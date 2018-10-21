<?xml version="1.0" encoding="UTF-8"?>
<s:schema xmlns:s="http://purl.oclc.org/dsdl/schematron">
   <s:ns prefix="c" uri="http://www.w3.org/ns/xproc-step"/>

   <s:pattern>
     <s:rule context="/*">
       <s:assert test="self::doc">The pipeline root is not doc.</s:assert>
       <s:assert test=". = 'none'">The result isn't "none"</s:assert>
     </s:rule>
   </s:pattern>
</s:schema>
