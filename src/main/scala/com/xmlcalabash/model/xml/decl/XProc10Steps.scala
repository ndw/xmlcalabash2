package com.xmlcalabash.model.xml.decl

import com.xmlcalabash.core.XProcConstants
import net.sf.saxon.s9api.QName

/**
  * Created by ndw on 10/4/16.
  */
class XProc10Steps extends StepLibrary {
  addDecl(XProcConstants.p_add_attribute,
    List(new InputDecl("source")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "match"), required = true),
      new OptionDecl(new QName("", "attribute-name"), required = true),
      new OptionDecl(new QName("", "attribute-prefix")),
      new OptionDecl(new QName("", "attribute-namespace")),
      new OptionDecl(new QName("", "attribute-value"), required = true)))

  addDecl(XProcConstants.p_add_xml_base,
    List(new InputDecl("source")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "all"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "relative"), required = false, select = "'true'")))

  addDecl(XProcConstants.p_compare,
    List(new InputDecl("source"),
      new InputDecl("alternate")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "fail-if-not-equal"), required = false, select = "'false'")))
  steps(XProcConstants.p_compare).inputs("source").primary = true
  steps(XProcConstants.p_compare).outputs("result").primary = false

  addDecl(XProcConstants.p_count,
    List(new InputDecl("source", sequence = true)),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "limit"), select = "0")))

  addDecl(XProcConstants.p_delete,
    List(new InputDecl("source")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "match"), required = true)))

  addDecl(XProcConstants.p_directory_list,
    List.empty[InputDecl],
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "path"), required = true),
      new OptionDecl(new QName("", "include-filter")),
      new OptionDecl(new QName("", "exclude-filter"))))

  addDecl(XProcConstants.p_error,
    List(new InputDecl("source")),
    List(new OutputDecl("result", sequence = true)),
    List(new OptionDecl(new QName("", "code"), required = true),
      new OptionDecl(new QName("", "code-prefix")),
      new OptionDecl(new QName("", "code-namespace"))))
  steps(XProcConstants.p_error).inputs("source").primary = true

  addDecl(XProcConstants.p_escape_markup,
    List(new InputDecl("source")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "cdata-section-elements"), required = false, select = "''"),
      new OptionDecl(new QName("", "doctype-public")),
      new OptionDecl(new QName("", "doctype-system")),
      new OptionDecl(new QName("", "escape-uri-attributes"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "include-content-type"), required = false, select = "'true'"),
      new OptionDecl(new QName("", "indent"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "media-type")),
      new OptionDecl(new QName("", "method"), required = false, select = "'xml'"),
      new OptionDecl(new QName("", "omit-xml-declaration"), required = false, select = "'true'"),
      new OptionDecl(new QName("", "standalone"), required = false, select = "'omit'"),
      new OptionDecl(new QName("", "undeclare-prefixes")),
      new OptionDecl(new QName("", "version"), required = false, select = "'1.0'")))

  addDecl(XProcConstants.p_filter,
    List(new InputDecl("source")),
    List(new OutputDecl("result", sequence = true)),
    List(new OptionDecl(new QName("", "select"), required = true)))

  addDecl(XProcConstants.p_http_request,
    List(new InputDecl("source")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "byte-order-mark")),
      new OptionDecl(new QName("", "cdata-section-elements"), required = false, select = "''"),
      new OptionDecl(new QName("", "doctype-public")),
      new OptionDecl(new QName("", "doctype-system")),
      new OptionDecl(new QName("", "encoding")),
      new OptionDecl(new QName("", "escape-uri-attributes"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "include-content-type"), required = false, select = "'true'"),
      new OptionDecl(new QName("", "indent"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "media-type")),
      new OptionDecl(new QName("", "method"), required = false, select = "'xml'"),
      new OptionDecl(new QName("", "normalization-form"), required = false, select = "'none'"),
      new OptionDecl(new QName("", "omit-xml-declaration"), required = false, select = "'true'"),
      new OptionDecl(new QName("", "standalone"), required = false, select = "'omit'"),
      new OptionDecl(new QName("", "undeclare-prefixes")),
      new OptionDecl(new QName("", "version"), required = false, select = "'1.0'")))

  addDecl(XProcConstants.p_identity,
    List(new InputDecl("source", sequence = true)),
    List(new OutputDecl("result", sequence = true)))

  addDecl(XProcConstants.p_insert,
    List(new InputDecl("source"),
      new InputDecl("insertion", sequence = true)),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "match"), required = false, select = "'/*'"),
      new OptionDecl(new QName("", "position"), required = true)))
  steps(XProcConstants.p_insert).inputs("source").primary = true

  addDecl(XProcConstants.p_label_elements,
    List(new InputDecl("source")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "attribute"), required = false, select = "'xml:id'"),
      new OptionDecl(new QName("", "attribute-prefix")),
      new OptionDecl(new QName("", "attribute-namespace")),
      new OptionDecl(new QName("", "label"), required = false, select = "'concat(\"_\",$p:index)'"),
      new OptionDecl(new QName("", "match"), required = false, select = "'*'"),
      new OptionDecl(new QName("", "replace"), required = false, select = "'true'")))

  addDecl(XProcConstants.p_load,
    List.empty[InputDecl],
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "href"), required = true),
      new OptionDecl(new QName("", "dtd-validate"), required = false, select = "'false'")))

  addDecl(XProcConstants.p_make_absolute_uris,
    List(new InputDecl("source")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "match"), required = true),
      new OptionDecl(new QName("", "base-uri"))))

  addDecl(XProcConstants.p_namespace_rename,
    List(new InputDecl("source")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "from")),
      new OptionDecl(new QName("", "to")),
      new OptionDecl(new QName("", "apply-to"), required = false, select = "'all'")))

  addDecl(XProcConstants.p_pack,
    List(new InputDecl("source", sequence = true),
      new InputDecl("alternate", sequence = true)),
    List(new OutputDecl("result", sequence = true)),
    List(new OptionDecl(new QName("", "wrapper"), required = true),
      new OptionDecl(new QName("", "wrapper-prefix")),
      new OptionDecl(new QName("", "wrapper-namespace"))))
  steps(XProcConstants.p_pack).inputs("source").primary = true

  addDecl(XProcConstants.p_parameters,
    List(new InputDecl("parameters")),
    List(new OutputDecl("result")))
  steps(XProcConstants.p_parameters).inputs("parameters").kind = "parameter"
  steps(XProcConstants.p_parameters).inputs("parameters").primary = false
  steps(XProcConstants.p_parameters).outputs("result").primary = false

  addDecl(XProcConstants.p_rename,
    List(new InputDecl("source")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "match"), required = true),
      new OptionDecl(new QName("", "new-name"), required = true),
      new OptionDecl(new QName("", "new-prefix")),
      new OptionDecl(new QName("", "new-namespace"))))

  addDecl(XProcConstants.p_replace,
    List(new InputDecl("source"),
      new InputDecl("replacement")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "match"), required = true)))
  steps(XProcConstants.p_replace).inputs("source").primary = true

  addDecl(XProcConstants.p_set_attributes,
    List(new InputDecl("source"),
      new InputDecl("attributes")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "match"), required = true)))
  steps(XProcConstants.p_set_attributes).inputs("source").primary = true

  addDecl(XProcConstants.p_sink,
    List(new InputDecl("source", sequence = true)),
    List.empty[OutputDecl])

  addDecl(XProcConstants.p_split_sequence,
    List(new InputDecl("source", sequence = true)),
    List(new OutputDecl("matched", sequence = true),
      new OutputDecl("not-matched", sequence = true)),
    List(new OptionDecl(new QName("", "initial-only"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "test"), required = true)))
  steps(XProcConstants.p_split_sequence).outputs("matched").primary = true

  addDecl(XProcConstants.p_store,
    List(new InputDecl("source")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "href"), required = true),
      new OptionDecl(new QName("", "byte-order-mark")),
      new OptionDecl(new QName("", "cdata-section-elements"), required = false, select = "''"),
      new OptionDecl(new QName("", "doctype-public")),
      new OptionDecl(new QName("", "doctype-system")),
      new OptionDecl(new QName("", "encoding")),
      new OptionDecl(new QName("", "escape-uri-attributes"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "include-content-type"), required = false, select = "'true'"),
      new OptionDecl(new QName("", "indent"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "media-type")),
      new OptionDecl(new QName("", "method"), required = false, select = "'xml'"),
      new OptionDecl(new QName("", "normalization-form"), required = false, select = "'none'"),
      new OptionDecl(new QName("", "omit-xml-declaration"), required = false, select = "'true'"),
      new OptionDecl(new QName("", "standalone"), required = false, select = "'omit'"),
      new OptionDecl(new QName("", "undeclare-prefixes")),
      new OptionDecl(new QName("", "version"), required = false, select = "'1.0'")))
  steps(XProcConstants.p_store).outputs("result").primary = false

  addDecl(XProcConstants.p_string_replace,
    List(new InputDecl("source")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "match"), required = true),
      new OptionDecl(new QName("", "replace"), required = true)))

  addDecl(XProcConstants.p_unescape_markup,
    List(new InputDecl("source")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "namespace")),
      new OptionDecl(new QName("", "content-type"), required = false, select = "'application/xml'"),
      new OptionDecl(new QName("", "encoding")),
      new OptionDecl(new QName("", "charset"))))

  addDecl(XProcConstants.p_unwrap,
    List(new InputDecl("source")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "match"), required = true)))

  addDecl(XProcConstants.p_wrap,
    List(new InputDecl("source")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "wrapper"), required = true),
      new OptionDecl(new QName("", "wrapper-prefix")),
      new OptionDecl(new QName("", "wrapper-namespace")),
      new OptionDecl(new QName("", "match"), required = true),
      new OptionDecl(new QName("", "group-adjacent"))))

  addDecl(XProcConstants.p_wrap_sequence,
    List(new InputDecl("source", sequence = true)),
    List(new OutputDecl("result", sequence = true)),
    List(new OptionDecl(new QName("", "wrapper"), required = true),
      new OptionDecl(new QName("", "wrapper-prefix")),
      new OptionDecl(new QName("", "wrapper-namespace")),
      new OptionDecl(new QName("", "group-adjacent"))))

  addDecl(XProcConstants.p_xinclude,
    List(new InputDecl("source")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "fixup-xml-base"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "fixup-xml-lang"), required = false, select = "'false'")))

  addDecl(XProcConstants.p_xslt,
    List(new InputDecl("source", sequence = true),
      new InputDecl("stylesheet"),
      new InputDecl("parameters")),
    List(new OutputDecl("result"),
      new OutputDecl("secondary", sequence = true)),
    List(new OptionDecl(new QName("", "initial-mode")),
      new OptionDecl(new QName("", "template-name")),
      new OptionDecl(new QName("", "output-base-uri")),
      new OptionDecl(new QName("", "version"))))
  steps(XProcConstants.p_xslt).inputs("source").primary = true
  steps(XProcConstants.p_xslt).inputs("parameters").kind = "parameter"
  steps(XProcConstants.p_xslt).outputs("result").primary = true

  addDecl(XProcConstants.p_exec,
    List(new InputDecl("source", sequence = true)),
    List(new OutputDecl("result"),
      new OutputDecl("errors"),
      new OutputDecl("exit-status")),
    List(new OptionDecl(new QName("", "command"), required = true),
      new OptionDecl(new QName("", "args"), required = false, select = "''"),
      new OptionDecl(new QName("", "cwd")),
      new OptionDecl(new QName("", "source-is-xml"), required = false, select = "'true'"),
      new OptionDecl(new QName("", "result-is-xml"), required = false, select = "'true'"),
      new OptionDecl(new QName("", "wrap-result-lines"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "errors-is-xml"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "wrap-error-lines"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "path-separator")),
      new OptionDecl(new QName("", "failure-threshold")),
      new OptionDecl(new QName("", "arg-separator"), required = false, select = "' '"),
      new OptionDecl(new QName("", "byte-order-mark")),
      new OptionDecl(new QName("", "cdata-section-elements"), required = false, select = "''"),
      new OptionDecl(new QName("", "doctype-public")),
      new OptionDecl(new QName("", "doctype-system")),
      new OptionDecl(new QName("", "encoding")),
      new OptionDecl(new QName("", "escape-uri-attributes"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "include-content-type"), required = false, select = "'true'"),
      new OptionDecl(new QName("", "indent"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "media-type")),
      new OptionDecl(new QName("", "method"), required = false, select = "'xml'"),
      new OptionDecl(new QName("", "normalization-form"), required = false, select = "'none'"),
      new OptionDecl(new QName("", "omit-xml-declaration"), required = false, select = "'true'"),
      new OptionDecl(new QName("", "standalone"), required = false, select = "'omit'"),
      new OptionDecl(new QName("", "undeclare-prefixes")),
      new OptionDecl(new QName("", "version"), required = false, select = "'1.0'")))
  steps(XProcConstants.p_exec).outputs("result").primary = true

  addDecl(XProcConstants.p_hash,
    List(new InputDecl("source"),
      new InputDecl("parameters")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "value"), required = true),
      new OptionDecl(new QName("", "algorithm"), required = true),
      new OptionDecl(new QName("", "match"), required = true),
      new OptionDecl(new QName("", "version"))))
  steps(XProcConstants.p_xslt).inputs("parameters").kind = "parameter"

  addDecl(XProcConstants.p_uuid,
    List(new InputDecl("source")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "match"), required = true),
      new OptionDecl(new QName("", "version"))))

  addDecl(XProcConstants.p_validate_with_relax_ng,
    List(new InputDecl("source"),
      new InputDecl("schema")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "dtd-attribute-values"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "dtd-id-idref-warnings"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "assert-valid"), required = false, select = "'true'")))
  steps(XProcConstants.p_validate_with_relax_ng).inputs("source").primary = true

  addDecl(XProcConstants.p_validate_with_schematron,
    List(new InputDecl("source"),
      new InputDecl("schema"),
      new InputDecl("parameters")),
    List(new OutputDecl("result"),
      new OutputDecl("report", sequence = true)),
    List(new OptionDecl(new QName("", "phase"), required = false, select = "'#ALL'"),
      new OptionDecl(new QName("", "assert-valid"), required = false, select = "'true'")))
  steps(XProcConstants.p_validate_with_schematron).inputs("source").primary = true
  steps(XProcConstants.p_validate_with_schematron).inputs("parameters").kind = "parameter"
  steps(XProcConstants.p_validate_with_schematron).outputs("result").primary = true

  addDecl(XProcConstants.p_validate_with_xml_schema,
    List(new InputDecl("source"),
      new InputDecl("schema", sequence = true)),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "use-location-hints"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "try-namespaces"), required = false, select = "'false'"),
      new OptionDecl(new QName("", "assert-valid"), required = false, select = "'true'"),
      new OptionDecl(new QName("", "mode"), required = false, select = "'strict'")))
  steps(XProcConstants.p_validate_with_xml_schema).inputs("source").primary = true

  addDecl(XProcConstants.p_www_form_urldecode,
    List.empty[InputDecl],
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "value"), required = true)))

  addDecl(XProcConstants.p_www_form_urlencode,
    List(new InputDecl("source"),
      new InputDecl("parameters")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "match"), required = true)))
  steps(XProcConstants.p_www_form_urlencode).inputs("parameters").kind = "parameter"

  addDecl(XProcConstants.p_xquery,
    List(new InputDecl("source", sequence = true),
      new InputDecl("query"),
      new InputDecl("parameters")),
    List(new OutputDecl("result", sequence = true)))
  steps(XProcConstants.p_xquery).inputs("source").primary = true
  steps(XProcConstants.p_xquery).inputs("parameters").kind = "parameter"

  addDecl(XProcConstants.p_xsl_formatter,
    List(new InputDecl("source"),
      new InputDecl("parameters")),
    List(new OutputDecl("result")),
    List(new OptionDecl(new QName("", "href"), required = true),
      new OptionDecl(new QName("", "content-type"))))
  steps(XProcConstants.p_xsl_formatter).inputs("parameters").kind = "parameter"
  steps(XProcConstants.p_xsl_formatter).outputs("result").primary = true

  // FIXME: this one is fake
  addDecl(XProcConstants.p_interleave,
    List(new InputDecl("left"),
      new InputDecl("right")),
    List(new OutputDecl("result")))
  steps(XProcConstants.p_interleave).inputs("left").primary = true
}
