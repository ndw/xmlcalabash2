package com.xmlcalabash.core

import net.sf.saxon.s9api.QName
import net.sf.saxon.`type`.{SimpleType, BuiltInType}
import net.sf.saxon.om.StandardNames

/**
  * Created by ndw on 10/1/16.
  */
object XProcConstants {
  val NS_XPROC = "http://www.w3.org/ns/xproc"
  val NS_XPROC_ERROR = "http://www.w3.org/ns/xproc-error"
  val NS_XPROC_STEP = "http://www.w3.org/ns/xproc-step"
  val NS_CALABASH_EX = "http://xmlcalabash.com/ns/extensions"
  val NS_CALABASH_CP = "http://xmlcalabash.com/ns/compiled"
  val NS_CALABASH_PX = "http://xmlcalabash.com/ns/parsed"
  val NS_XMLSCHEMA = "http://www.w3.org/2001/XMLSchema"
  val NS_XML = "http://www.w3.org/XML/1998/namespace"

  val _name = new QName("", "name")
  val _port = new QName("", "port")
  val _step = new QName("", "step")
  val _href = new QName("", "href")
  val _select = new QName("", "select")
  val _sequence = new QName("", "sequence")
  val _primary = new QName("", "primary")
  val _required = new QName("", "required")
  val _as = new QName("", "as")
  val _uid = new QName("", "uid")
  val _type = new QName("", "type")
  val _content_types = new QName("", "content-types")
  val _exclude_result_prefixes = new QName("", "exclude-result-prefixes")
  val _version = new QName("", "version")
  val _use_when = new QName("", "use-when")

  val p_use_when = new QName("p", NS_XPROC, "use-when")
  val p_catch = new QName("p", NS_XPROC, "catch")
  val p_choose = new QName("p", NS_XPROC, "choose")
  val p_data = new QName("p", NS_XPROC, "data")
  val p_declare_step = new QName("p", NS_XPROC, "declare-step")
  val p_document = new QName("p", NS_XPROC, "document")
  val p_documentation = new QName("p", NS_XPROC, "documentation")
  val p_empty = new QName("p", NS_XPROC, "empty")
  val p_for_each = new QName("p", NS_XPROC, "for-each")
  val p_group = new QName("p", NS_XPROC, "group")
  val p_import = new QName("p", NS_XPROC, "import")
  val p_inline = new QName("p", NS_XPROC, "inline")
  val p_input = new QName("p", NS_XPROC, "input")
  val p_iteration_source = new QName("p", NS_XPROC, "iteration-source")
  val p_library = new QName("p", NS_XPROC, "library")
  val p_log = new QName("p", NS_XPROC, "log")
  val p_namespaces = new QName("p", NS_XPROC, "namespaces")
  val p_option = new QName("p", NS_XPROC, "option")
  val p_otherwise = new QName("p", NS_XPROC, "otherwise")
  val p_output = new QName("p", NS_XPROC, "output")
  val p_pipe = new QName("p", NS_XPROC, "pipe")
  val p_pipeinfo = new QName("p", NS_XPROC, "pipeinfo")
  val p_pipeline = new QName("p", NS_XPROC, "pipeline")
  val p_serialization = new QName("p", NS_XPROC, "serialization")
  val p_try = new QName("p", NS_XPROC, "try")
  val p_variable = new QName("p", NS_XPROC, "variable")
  val p_viewport = new QName("p", NS_XPROC, "viewport")
  val p_viewport_source = new QName("p", NS_XPROC, "viewport-source")
  val p_when = new QName("p", NS_XPROC, "when")
  val p_with_option = new QName("p", NS_XPROC, "with-option")
  val p_with_param = new QName("p", NS_XPROC, "with-param")
  val p_xpath_context = new QName("p", NS_XPROC, "xpath-context")

  val p_add_attribute = new QName("p", NS_XPROC, "add-attribute")
  val p_add_xml_base = new QName("p", NS_XPROC, "add-xml-base")
  val p_compare = new QName("p", NS_XPROC, "compare")
  val p_count = new QName("p", NS_XPROC, "count")
  val p_delete = new QName("p", NS_XPROC, "delete")
  val p_directory_list = new QName("p", NS_XPROC, "directory-list")
  val p_error = new QName("p", NS_XPROC, "error")
  val p_escape_markup = new QName("p", NS_XPROC, "escape-markup")
  val p_exec = new QName("p", NS_XPROC, "exec")
  val p_http_request = new QName("p", NS_XPROC, "http-request")
  val p_identity = new QName("p", NS_XPROC, "identity")
  val p_filter = new QName("p", NS_XPROC, "filter")
  val p_hash = new QName("p", NS_XPROC, "hash")
  val p_uuid = new QName("p", NS_XPROC, "uuid")
  val p_insert = new QName("p", NS_XPROC, "insert")
  val p_label_elements = new QName("p", NS_XPROC, "label-elements")
  val p_load = new QName("p", NS_XPROC, "load")
  val p_make_absolute_uris = new QName("p", NS_XPROC, "make-absolute-uris")
  val p_namespace_rename = new QName("p", NS_XPROC, "namespace-rename")
  val p_pack = new QName("p", NS_XPROC, "pack")
  val p_parameters = new QName("p", NS_XPROC, "parameters")
  val p_rename = new QName("p", NS_XPROC, "rename")
  val p_replace = new QName("p", NS_XPROC, "replace")
  val p_serialize = new QName("p", NS_XPROC, "serialize")
  val p_set_attributes = new QName("p", NS_XPROC, "set-attributes")
  val p_sink = new QName("p", NS_XPROC, "sink")
  val p_split_sequence = new QName("p", NS_XPROC, "split-sequence")
  val p_store = new QName("p", NS_XPROC, "store")
  val p_string_replace = new QName("p", NS_XPROC, "string-replace")
  val p_unescape_markup = new QName("p", NS_XPROC, "unescape-markup")
  val p_unwrap = new QName("p", NS_XPROC, "unwrap")
  val p_validate_with_relax_ng = new QName("p", NS_XPROC, "validate-with-relax-ng")
  val p_validate_with_xml_schema = new QName("p", NS_XPROC, "validate-with-xml-schema")
  val p_validate_with_schematron = new QName("p", NS_XPROC, "validate-with-schematron")
  val p_wrap = new QName("p", NS_XPROC, "wrap")
  val p_wrap_sequence = new QName("p", NS_XPROC, "wrap-sequence")
  val p_www_form_urldecode = new QName("p", NS_XPROC, "www-form-urldecode")
  val p_www_form_urlencode = new QName("p", NS_XPROC, "www-form-urlencode")
  val p_xquery = new QName("p", NS_XPROC, "xquery")
  val p_xslt = new QName("p", NS_XPROC, "xslt")
  val p_xsl_formatter = new QName("p", NS_XPROC, "xsl-formatter")
  val p_xinclude = new QName("p", NS_XPROC, "xinclude")
  val p_in_scope_names = new QName("p", NS_XPROC, "in-scope-names")
  val p_template = new QName("p", NS_XPROC, "template")

  val c_data = new QName(NS_XPROC_STEP, "data")
  val xs_QName = new QName("xs", NS_XMLSCHEMA, "QName")
  val xs_untypedAtomic = new QName("xs", NS_XMLSCHEMA, "untypedAtomic")
  val xs_string = new QName("xs", NS_XMLSCHEMA, "string")
  val xs_NCName = new QName("xs", NS_XMLSCHEMA, "NCName")
  val xs_boolean = new QName("xs", NS_XMLSCHEMA, "boolean")
  val xs_decimal = new QName("xs", NS_XMLSCHEMA, "decimal")
  val cx_readers = new QName("cx", XProcConstants.NS_CALABASH_EX, "readers")
  val cx_splitter = new QName("cx", XProcConstants.NS_CALABASH_EX, "splitter")
  val cx_buffer = new QName("cx", XProcConstants.NS_CALABASH_EX, "buffer")

  val untyped = BuiltInType.getSchemaType(StandardNames.XS_UNTYPED)
  val untypedAtomic = BuiltInType.getSchemaType(StandardNames.XS_UNTYPED_ATOMIC).asInstanceOf[SimpleType]

  def px(localName: String): QName = {
    new QName("px", NS_CALABASH_PX, localName)
  }

  def staticError(errno: Int) = {
    new QName("err", NS_XPROC_ERROR, "XS%04d".format(errno))
  }

  def dynamicError(errno: Int) = {
    new QName("err", NS_XPROC_ERROR, "XD%04d".format(errno))
  }

  def stepError(errno: Int): QName = {
    new QName("err", NS_XPROC_ERROR, "XC%04d".format(errno))
  }

}
