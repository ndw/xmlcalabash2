<?xml version="1.0" encoding="UTF-8"?>
<s:schema xmlns:s="http://purl.oclc.org/dsdl/schematron"
          xmlns:p="http://www.w3.org/ns/xproc"
          xmlns:rng="http://relaxng.org/ns/structure/1.0">
   <s:ns prefix="p" uri="http://www.w3.org/ns/xproc"/>

   <s:pattern>
     <s:rule context="/*">
       <s:assert test="self::wrapped">The pipeline root is not wrapped.</s:assert>
       <s:assert test="name(self::wrapped/child::*[1])='doc'">The first child is not 'doc'.</s:assert>
       <s:assert test="name(self::wrapped/child::*[2])='doc2'">The second child is not 'doc2'.</s:assert>
       <s:assert test="count(self::wrapped//*)=2">'wrapped' does not have exactly two child elements.</s:assert>
     </s:rule>
   </s:pattern>
</s:schema>
