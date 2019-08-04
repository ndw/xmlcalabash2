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
  val ns_xsl = "http://www.w3.org/1999/XSL/Transform"
  val ns_xml = "http://www.w3.org/XML/1998/namespace"
  val ns_xmlns = "http://www.w3.org/2000/xmlns/"
  val ns_xqt_errors = "http://www.w3.org/2005/xqt-errors"

  val UNKNOWN = new QName("", "UNKNOWN")

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
  val p_function = new QName("p", ns_p, "function")
  val p_group = new QName("p", ns_p, "group")
  val p_if = new QName("p", ns_p, "if")
  val p_import = new QName("p", ns_p, "import")
  val p_import_functions = new QName("p", ns_p, "import-functions")
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
  val p_with_output = new QName("p", ns_p, "with-output")

  val p_iteration_size = new QName("p", ns_p, "iteration-size")
  val p_iteration_position = new QName("p", ns_p, "iteration-position")

  val p_injectable = new QName("p", ns_p, "injectable")
  val p_start = new QName("p", ns_p, "start")
  val p_end = new QName("p", ns_p, "end")
  val p_document_properties = new QName("p", ns_p, "document-properties")

  val p_error = new QName("p", ns_p, "error")
  val p_identity = new QName("p", ns_p, "identity")
  val p_sink = new QName("p", ns_p, "sink")

  val cx_until = new QName("cx", ns_cx, "until")
  val cx_while = new QName("cx", ns_cx, "while")
  val cx_loop = new QName("cx", ns_cx, "loop")
  val cx_content_type_checker = new QName("cx", ns_cx, "content-type-checker")
  val cx_filter = new QName("cx", ns_cx, "filter")
  val cx_exception_translator = new QName("cx", ns_cx, "exception-translator")

  // The XML Schema type names must be defined somewhere in Saxon but...
  val xs_ENTITY = new QName("xs", ns_xs, "ENTITY")
  val xs_ID = new QName("xs", ns_xs, "ID")
  val xs_IDREF = new QName("xs", ns_xs, "IDREF")
  val xs_NCName = new QName("xs", ns_xs, "NCName")
  val xs_NMTOKEN = new QName("xs", ns_xs, "NMTOKEN")
  val xs_QName = new QName("xs", ns_xs, "QName")
  val xs_anyAtomicType = new QName("xs", ns_xs, "anyAtomicType")
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
  val xs_language = new QName("xs", ns_xs, "language")
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

  // These are psuedo-types used in this implemention.
  val pxs_XSLTMatchPattern = new QName("xs", ns_xs, "XSLTMatchPattern")

  val xsi_type = new QName("xsi", ns_xsi, "type")
  val xsi_noNamespaceSchemaLocation = new QName("xsi", ns_xsi, "noNamespaceSchemaLocation")
  val xsi_schemaLocation = new QName("xsi", ns_xsi, "schemaLocation")

  // Extras for testing
  val p_producer = new QName("p", ns_p, "producer")

  val c_data = new QName("c", ns_c, "data")
  val c_error = new QName("c", ns_c, "error")
  val c_errors = new QName("c", ns_c, "errors")
  val c_message = new QName("c", ns_c, "message")
  val c_param_set = new QName("c", ns_c, "param-set")
  val c_param = new QName("c", ns_c, "param")
  val c_document_properties = new QName("c", ns_c, "document-properties")
  val c_property = new QName("c", ns_c, "property")
  val c_result = new QName("c", ns_c, "result")
  val c_directory = new QName("c", ns_c, "directory")
  val c_file = new QName("c", ns_c, "file")
  val c_other = new QName("c", ns_c, "other")

  // Extensions
  val cx_as = new QName("cx", ns_cx, "as")
  val cx_document = new QName("cx", ns_cx, "document")
  val cx_class = new QName("cx", ns_cx, "class")
  val cx_unknown = new QName("cx", ns_cx, "unknown")
  val cx_select_filter = new QName("cx", ns_cx, "select-filter")
  val cx_inline_loader = new QName("cx", ns_cx, "inline-loader")
  val cx_document_loader = new QName("cx", ns_cx, "document-loader")
  val cx_empty_loader = new QName("cx", ns_cx, "empty-loader")
  val cx_use_default_input = new QName("cx", ns_cx, "use-default-input")

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
  val _inline_expand_text = new QName("", "inline-expand-text")
  val _item_separator = new QName("", "item-separator")
  val _match = new QName("", "match")
  val _media_type = new QName("", "media-type")
  val _method = new QName("", "method")
  val _name = new QName("", "name")
  val _namespace = new QName("", "namespace")
  val _normalization_form = new QName("", "normalization-form")
  val _omit_xml_declaration = new QName("", "omit-xml-declaration")
  val _override_content_type = new QName("", "override-content-type")
  val _required = new QName("", "required")
  val _parameters = new QName("", "parameters")
  val _properties = new QName("", "properties")
  val _pipe = new QName("", "pipe")
  val _port = new QName("", "port")
  val _primary = new QName("", "primary")
  val _psvi_required = new QName("", "psvi-required")
  val _select = new QName("", "select")
  val _static = new QName("", "static")
  val _sequence = new QName("", "sequence")
  val _serialization = new QName("", "serialization")
  val _standalone = new QName("", "standalone")
  val _step = new QName("", "step")
  val _test = new QName("", "test")
  val _type = new QName("", "type")
  val _undeclare_prefixes = new QName("", "undeclare-prefixes")
  val _value = new QName("", "value")
  val _values = new QName("", "values")
  val _version = new QName("", "version")
  val _visibility = new QName("", "visibility")
  val _xpath_version = new QName("", "xpath-version")

  val _base_uri = new QName("", "base-uri")
  val _content_type = new QName("", "content-type")
  val _content_length = new QName("", "content-length")
  val _content_types = new QName("", "content-types")
  val _collection = new QName("", "collection")
  val _dtd_validate = new QName("", "dtd-validate")
  val _last_modified = new QName("", "", "last-modified")
  val _message = new QName("", "message")
  val _condition = new QName("", "condition")

  // Related to errors
  val _uri = new QName("", "uri")
  val _line = new QName("", "line-number")
  val _column = new QName("", "column-number")
  val _source_uri = new QName("", "source-uri")
  val _source_line = new QName("", "source-line-number")
  val _source_column = new QName("", "source-column-number")
  val _path = new QName("", "path")
  val _constraint_name = new QName("", "constraint-name")
  val _constraint_cause = new QName("", "constraint-clause")
  val _schema_type = new QName("", "schema-type")
  val _schema_part = new QName("", "schema-part")

  val sx_attribute_order = new QName("http://saxon.sf.net/", "attribute-order")
  val sx_character_representation = new QName("http://saxon.sf.net/", "character-representation")
  val sx_double_space = new QName("http://saxon.sf.net/", "double-space")
  val sx_implicit_result_document = new QName("http://saxon.sf.net/", "implicit-result-document")
  val sx_indent_spaces = new QName("http://saxon.sf.net/", "indent-spaces")
  val sx_line_length = new QName("http://saxon.sf.net/", "line-length")
  val sx_newline = new QName("http://saxon.sf.net/", "newline")
  val sx_recognize_binary = new QName("http://saxon.sf.net/", "recognize-binary")
  val sx_suppress_indentation = new QName("http://saxon.sf.net/", "suppress-indentation")
  val sx_wrap = new QName("http://saxon.sf.net/", "wrap")
  val sx_canonical = new QName("http://saxon.sf.net/", "canonical")

  // For non-XProc namespaced places
  val p_expand_text = new QName("p", ns_p, "expand-text")
  val p_inline_expand_text = new QName("p", ns_p, "inline-expand-text")
  val p_message = new QName("p", ns_p, "message")
  val p_exclude_inline_prefixes = new QName( "p", ns_p,"exclude-inline-prefixes")
}
