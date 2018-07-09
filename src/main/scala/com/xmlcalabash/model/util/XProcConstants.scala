package com.xmlcalabash.model.util

import net.sf.saxon.s9api.QName

object XProcConstants {
  val ns_p   = "http://www.w3.org/ns/xproc"
  val ns_c   = "http://www.w3.org/ns/xproc-step"
  val ns_err = "http://www.w3.org/ns/xproc-error"
  val ns_xs  = "http://www.w3.org/2001/XMLSchema"
  val ns_cx  = "http://xmlcalabash.com/ns/extensions"
  val ns_exf = "http://exproc.org/standard/functions"
  val ns_xsi = "http://www.w3.org/2001/XMLSchema-instance"
  val ns_xml = "http://www.w3.org/XML/1998/namespace"

  val xml_base = new QName("xml", ns_xml, "base")
  val xml_id = new QName("xml", ns_xml, "id")

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
  val p_with_input = new QName("p", ns_p, "with-input")
  val p_with_option = new QName("p", ns_p, "with-option")

  val p_with_document = new QName("p", ns_p, "with-document")
  val p_with_properties = new QName("p", ns_p, "with-properties")

  val p_injectable = new QName("p", ns_p, "injectable")
  val p_start = new QName("p", ns_p, "start")
  val p_end = new QName("p", ns_p, "end")
  val p_document_properties = new QName("p", ns_p, "document-properties")

  val p_error = new QName("p", ns_p, "error")
  val p_identity = new QName("p", ns_p, "identity")
  val p_sink = new QName("p", ns_p, "sink")

  val cx_content_type_checker = new QName("cx", ns_cx, "content-type-checker")

  // The XML Schema type names must be defined somewhere in Saxon but...
  val xs_ENTITY = new QName("xs", ns_xs, "ENTITY")
  val xs_ID = new QName("xs", ns_xs, "ID")
  val xs_IDREF = new QName("xs", ns_xs, "IDREF")
  val xs_NCName = new QName("xs", ns_xs, "NCName")
  val xs_NMTOKEN = new QName("xs", ns_xs, "NMTOKEN")
  val xs_QName = new QName("xs", ns_xs, "QName")
  val xs_anyURI = new QName("xs", ns_xs, "anyURI")
  val xs_base64Binary = new QName("xs", ns_xs, "base64Binary")
  val xs_boolean = new QName("xs", ns_xs, "boolean")
  val xs_byte = new QName("xs", ns_xs, "byte")
  val xs_date = new QName("xs", ns_xs, "date")
  val xs_dateTime = new QName("xs", ns_xs, "dateTime")
  val xs_dateTimeStamp = new QName("xs", ns_xs, "dateTimeStamp")
  val xs_dayTimeDuration = new QName("xs", ns_xs, "dayTimeDuration")
  val xs_decimal = new QName("xs", ns_xs, "decimal")
  val xs_double = new QName("xs", ns_xs, "double")
  val xs_duration = new QName("xs", ns_xs, "duration")
  val xs_float = new QName("xs", ns_xs, "float")
  val xs_gDay = new QName("xs", ns_xs, "gDay")
  val xs_gMonth = new QName("xs", ns_xs, "gMonth")
  val xs_gMonthDay = new QName("xs", ns_xs, "gMonthDay")
  val xs_gYear = new QName("xs", ns_xs, "gYear")
  val xs_gYearMonth = new QName("xs", ns_xs, "gYearMonth")
  val xs_hexBinary = new QName("xs", ns_xs, "hexBinary")
  val xs_int = new QName("xs", ns_xs, "int")
  val xs_integer = new QName("xs", ns_xs, "integer")
  val xs_long = new QName("xs", ns_xs, "long")
  val xs_name = new QName("xs", ns_xs, "name")
  val xs_negativeInteger = new QName("xs", ns_xs, "negativeInteger")
  val xs_nonNegativeInteger = new QName("xs", ns_xs, "nonNegativeInteger")
  val xs_nonPositiveInteger = new QName("xs", ns_xs, "nonPositiveInteger")
  val xs_normalizedString = new QName("xs", ns_xs, "normalizedString")
  val xs_notation = new QName("xs", ns_xs, "notation")
  val xs_positiveInteger = new QName("xs", ns_xs, "positiveInteger")
  val xs_short = new QName("xs", ns_xs, "short")
  val xs_string = new QName("xs", ns_xs, "string")
  val xs_time = new QName("xs", ns_xs, "time")
  val xs_token = new QName("xs", ns_xs, "token")
  val xs_unsignedByte = new QName("xs", ns_xs, "unsignedByte")
  val xs_unsignedInt = new QName("xs", ns_xs, "unsignedInt")
  val xs_unsignedLong = new QName("xs", ns_xs, "unsignedLong")
  val xs_unsignedShort = new QName("xs", ns_xs, "unsignedShort")
  val xs_untypedAtomic = new QName("xs", ns_xs, "untypedAtomic")
  val xs_yearMonthDuration = new QName("xs", ns_xs, "yearMonthDuration")

  val xsi_type = new QName("xsi", ns_xsi, "type")

  // Extras for testing
  val p_producer = new QName("p", ns_p, "producer")

  val c_data = new QName("c", ns_c, "data")
  val c_error = new QName("c", ns_c, "error")
  val c_param_set = new QName("c", ns_c, "param-set")
  val c_param = new QName("c", ns_c, "param")
  val c_document_properties = new QName("c", ns_c, "document-properties")
  val c_property = new QName("c", ns_c, "property")
  val c_result = new QName("c", ns_c, "result")

  // Extensions
  val cx_document = new QName("cx", ns_cx, "document")
  val cx_property_extract = new QName("cx", ns_cx, "property-extract")
  val cx_property_merge = new QName("cx", ns_cx, "property-merge")
  val cx_unknown = new QName("cx", ns_cx, "unknown")

  val _as = new QName("", "as")
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
  val _html_version = new QName("", "html-version")
  val _include_content_type = new QName("", "include-content-type")
  val _indent = new QName("", "indent")
  val _item_separator = new QName("", "item-separator")
  val _media_type = new QName("", "media-type")
  val _method = new QName("", "method")
  val _name = new QName("", "name")
  val _namespace = new QName("", "namespace")
  val _normalization_form = new QName("", "normalization-form")
  val _omit_xml_declaration = new QName("", "omit-xml-declaration")
  val _override_content_type = new QName("", "override-content-type")
  val _required = new QName("", "required")
  val _parameters = new QName("", "parameters")
  val _pipe = new QName("", "pipe")
  val _port = new QName("", "port")
  val _primary = new QName("", "primary")
  val _psvi_required = new QName("", "psvi-required")
  val _select = new QName("", "select")
  val _sequence = new QName("", "sequence")
  val _serialization = new QName("", "serialization")
  val _standalone = new QName("", "standalone")
  val _step = new QName("", "step")
  val _test = new QName("", "test")
  val _type = new QName("", "type")
  val _undeclare_prefixes = new QName("", "undeclare-prefixes")
  val _value = new QName("", "value")
  val _version = new QName("", "version")
  val _xpath_version = new QName("", "xpath-version")

  val _base_uri = new QName("", "base-uri")
  val _content_type = new QName("", "content-type")
  val _content_length = new QName("", "content-length")
  val _content_types = new QName("", "content-types")
  val _collection = new QName("", "collection")
  val _message = new QName("", "message")
  val _condition = new QName("", "condition")

  val _saxon_attribute_order = new QName("", "saxon-attribute-order")
  val _saxon_character_representation = new QName("", "saxon-character-representation")
  val _saxon_double_space = new QName("", "saxon-double-space")
  val _saxon_implicit_result_document = new QName("", "saxon-implicit-result-document")
  val _saxon_indent_spaces = new QName("", "saxon-indent-spaces")
  val _saxon_line_length = new QName("", "saxon-line-length")
  val _saxon_newline = new QName("", "saxon-newline")
  val _saxon_recognize_binary = new QName("", "saxon-recognize-binary")
  val _saxon_supply_source_locator = new QName("", "saxon-supply-source-locator")
  val _saxon_suppress_indentation = new QName("", "saxon-suppress-indentation")
  val _saxon_wrap = new QName("", "saxon-wrap")

  // For non-XProc namespaced places
  val p_expand_text = new QName("p", ns_p, "expand-text")
}
