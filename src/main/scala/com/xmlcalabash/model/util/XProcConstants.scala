package com.xmlcalabash.model.util

import net.sf.saxon.s9api.QName

object XProcConstants {
  val ns_p   = "http://www.w3.org/ns/xproc"
  val ns_c   = "http://www.w3.org/ns/xproc-step"
  val ns_err = "http://www.w3.org/ns/xproc-errors"
  val ns_xs  = "http://www.w3.org/2001/XMLSchema"
  val ns_cx  = "http://xmlcalabash.com/ns/extensions"
  val ns_exf = "http://exproc.org/standard/functions"

  val p_catch = new QName("p", ns_p, "catch")
  val p_choose = new QName("p", ns_p, "choose")
  val p_declare_step = new QName("p", ns_p, "declare-step")
  val p_document = new QName("p", ns_p, "document")
  val p_data = new QName("p", ns_p, "data")
  val p_documentation = new QName("p", ns_p, "documentation")
  val p_empty = new QName("p", ns_p, "empty")
  val p_finally = new QName("p", ns_p, "finally")
  val p_for_each = new QName("p", ns_p, "for-each")
  val p_group = new QName("p", ns_p, "group")
  val p_if = new QName("p", ns_p, "if")
  val p_import = new QName("p", ns_p, "import")
  val p_inline = new QName("p", ns_p, "inline")
  val p_input = new QName("p", ns_p, "input")
  val p_library = new QName("p", ns_p, "library")
  val p_log = new QName("p", ns_p, "log")
  val p_namespaces = new QName("p", ns_p, "namespaces")
  val p_option = new QName("p", ns_p, "option")
  val p_otherwise = new QName("p", ns_p, "otherwise")
  val p_output = new QName("p", ns_p, "output")
  val p_pipe = new QName("p", ns_p, "pipe")
  val p_pipeinfo = new QName("p", ns_p, "pipeinfo")
  val p_pipeline = new QName("p", ns_p, "pipeline")
  val p_serialization = new QName("p", ns_p, "serialization")
  val p_try = new QName("p", ns_p, "try")
  val p_variable = new QName("p", ns_p, "variable")
  val p_viewport = new QName("p", ns_p, "viewport")
  val p_when = new QName("p", ns_p, "when")
  val p_with_option = new QName("p", ns_p, "with-option")

  val p_error = new QName("p", ns_p, "error")
  val p_identity = new QName("p", ns_p, "identity")
  val p_sink = new QName("p", ns_p, "sink")

  // The XML Schema type names must be defined somewhere in Saxon but...
  val xs_QName = new QName("xs", ns_xs, "QName")
  val xs_string = new QName("xs", ns_xs, "string")

  // Extras for testing
  val p_producer = new QName("p", ns_p, "producer")

  val c_error = new QName("c", ns_c, "error")
  val c_param_set = new QName("c", ns_c, "param-set")
  val c_param = new QName("c", ns_c, "param")
  val c_document_properties = new QName("c", ns_c, "document-properties")
  val c_property = new QName("c", ns_c, "property")
  val c_result = new QName("c", ns_c, "result")

  // Extensions
  val cx_document = new QName("cx", ns_cx, "document")

  val _byte_order_mark = new QName("", "byte-order-mark")
  val _cdata_section_elements = new QName("", "cdata-section-elements")
  val _code = new QName("", "code")
  val _doctype_public = new QName("", "doctype-public")
  val _doctype_system = new QName("", "doctype-system")
  val _document_properties = new QName("", "document-properties")
  val _encoding = new QName("", "encoding")
  val _escape_uri_attributes = new QName("", "escape-uri-attributes")
  val _exclude_inline_prefixes = new QName("", "exclude-inline-prefixes")
  val _expand_text = new QName("", "expand-text")
  val _href = new QName("", "href")
  val _include_content_type = new QName("", "include-content-type")
  val _indent = new QName("", "indent")
  val _media_type = new QName("", "media-type")
  val _method = new QName("", "method")
  val _name = new QName("", "name")
  val _namespace = new QName("", "namespace")
  val _normalization_form = new QName("", "normalization-form")
  val _omit_xml_declaration = new QName("", "omit-xml-declaration")
  val _required = new QName("", "required")
  val _parameters = new QName("", "parameters")
  val _port = new QName("", "port")
  val _primary = new QName("", "primary")
  val _psvi_required = new QName("", "psvi-required")
  val _select = new QName("", "select")
  val _sequence = new QName("", "sequence")
  val _standalone = new QName("", "standalone")
  val _step = new QName("", "step")
  val _test = new QName("", "test")
  val _type = new QName("", "type")
  val _undeclare_prefixes = new QName("", "undeclare-prefixes")
  val _value = new QName("", "value")
  val _version = new QName("", "version")
  val _xpath_version = new QName("", "xpath-version")

  // For non-XProc namespaced places
  val p_expand_text = new QName("p", ns_p, "expand-text")
}
