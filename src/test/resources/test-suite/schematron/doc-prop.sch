<?xml version="1.0" encoding="UTF-8"?>
<s:schema xmlns:s="http://purl.oclc.org/dsdl/schematron">
   <s:ns prefix="c" uri="http://www.w3.org/ns/xproc-step"/>

   <s:pattern>
     <s:rule context="/*">
       <s:assert test="self::a">The pipeline root is not “a”.</s:assert>
       <s:assert test="string(.) = '1'">The property value is not 1</s:assert>
     </s:rule>
   </s:pattern>
</s:schema>
