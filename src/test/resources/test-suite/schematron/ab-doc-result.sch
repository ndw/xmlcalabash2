<?xml version="1.0" encoding="UTF-8"?>
<s:schema xmlns:s="http://purl.oclc.org/dsdl/schematron"
          xmlns:p="http://www.w3.org/ns/xproc"
          xmlns:rng="http://relaxng.org/ns/structure/1.0">
   <s:ns prefix="p" uri="http://www.w3.org/ns/xproc"/>

   <s:pattern>
     <s:rule context="/*">
       <s:assert test="self::doc">The pipeline root is not doc.</s:assert>
       <s:assert test="count(self::doc//*) = 0">No children of doc expected.</s:assert>
       <s:assert test="count(self::doc//@*) = 0">No attributes of doc expected.</s:assert>
     </s:rule>
   </s:pattern>
</s:schema>
