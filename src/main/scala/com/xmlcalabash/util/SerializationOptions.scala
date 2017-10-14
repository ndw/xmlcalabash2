package com.xmlcalabash.util

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.s9api.{QName, Serializer, XdmAtomicValue}

class SerializationOptions(config: XMLCalabash, opts: Map[QName, XdmAtomicValue]) {
  private val propertyMap: Map[QName, Serializer.Property] = Map(
    XProcConstants._byte_order_mark -> Serializer.Property.BYTE_ORDER_MARK,
    XProcConstants._cdata_section_elements -> Serializer.Property.CDATA_SECTION_ELEMENTS,
    XProcConstants._doctype_public -> Serializer.Property.DOCTYPE_PUBLIC,
    XProcConstants._doctype_system -> Serializer.Property.DOCTYPE_SYSTEM,
    XProcConstants._encoding -> Serializer.Property.ENCODING,
    XProcConstants._escape_uri_attributes -> Serializer.Property.ESCAPE_URI_ATTRIBUTES,
    XProcConstants._html_version -> Serializer.Property.HTML_VERSION,
    XProcConstants._include_content_type -> Serializer.Property.INCLUDE_CONTENT_TYPE,
    XProcConstants._indent -> Serializer.Property.INDENT,
    XProcConstants._item_separator -> Serializer.Property.ITEM_SEPARATOR,
    //XProcConstants._media_type -> Serializer.Property.MEDIA_TYPE,
    XProcConstants._method -> Serializer.Property.METHOD,
    XProcConstants._normalization_form -> Serializer.Property.NORMALIZATION_FORM,
    XProcConstants._omit_xml_declaration -> Serializer.Property.OMIT_XML_DECLARATION,
    XProcConstants._saxon_attribute_order -> Serializer.Property.SAXON_ATTRIBUTE_ORDER,
    XProcConstants._saxon_character_representation -> Serializer.Property.SAXON_CHARACTER_REPRESENTATION,
    XProcConstants._saxon_double_space -> Serializer.Property.SAXON_DOUBLE_SPACE,
    XProcConstants._saxon_indent_spaces -> Serializer.Property.SAXON_INDENT_SPACES,
    XProcConstants._saxon_line_length -> Serializer.Property.SAXON_LINE_LENGTH,
    XProcConstants._saxon_newline -> Serializer.Property.SAXON_NEWLINE,
    XProcConstants._saxon_recognize_binary -> Serializer.Property.SAXON_RECOGNIZE_BINARY,
    XProcConstants._saxon_supply_source_locator -> Serializer.Property.SAXON_SUPPLY_SOURCE_LOCATOR,
    XProcConstants._saxon_suppress_indentation -> Serializer.Property.SAXON_SUPPRESS_INDENTATION,
    XProcConstants._saxon_wrap -> Serializer.Property.SAXON_WRAP,
    XProcConstants._standalone -> Serializer.Property.STANDALONE,
    XProcConstants._undeclare_prefixes -> Serializer.Property.UNDECLARE_PREFIXES,
    //XProcConstants._use_character_maps -> Serializer.Property.USE_CHARACTER_MAPS,
    XProcConstants._version -> Serializer.Property.VERSION,
  )

  private val functionMap = Map(
    XProcConstants._byte_order_mark -> byte_order_mark,
    XProcConstants._cdata_section_elements -> cdata_section_elements,
    XProcConstants._doctype_public -> doctype_public,
    XProcConstants._doctype_system -> doctype_system,
    XProcConstants._encoding -> encoding,
    XProcConstants._escape_uri_attributes -> escape_uri_attributes,
    XProcConstants._html_version -> html_version,
    XProcConstants._include_content_type -> include_content_type,
    XProcConstants._indent -> indent,
    XProcConstants._item_separator -> item_separator,
    //XProcConstants._media_type -> media_type,
    XProcConstants._method -> method,
    XProcConstants._normalization_form -> normalization_form,
    XProcConstants._omit_xml_declaration -> omit_xml_declaration,
    XProcConstants._saxon_attribute_order -> saxon_attribute_order,
    XProcConstants._saxon_character_representation -> saxon_character_representation,
    XProcConstants._saxon_double_space -> saxon_double_space,
    XProcConstants._saxon_indent_spaces -> saxon_indent_spaces,
    XProcConstants._saxon_line_length -> saxon_line_length,
    XProcConstants._saxon_newline -> saxon_newline,
    XProcConstants._saxon_recognize_binary -> saxon_recognize_binary,
    XProcConstants._saxon_supply_source_locator -> saxon_supply_source_locator,
    XProcConstants._saxon_suppress_indentation -> saxon_suppress_indentation,
    XProcConstants._saxon_wrap -> saxon_wrap,
    XProcConstants._standalone -> standalone,
    XProcConstants._undeclare_prefixes -> undeclare_prefixes,
    //XProcConstants._use_character_maps -> ._use_character_maps,
    XProcConstants._version -> version,
  )

  def this(config: XMLCalabash) = {
    this(config, Map())
  }

  def setOutputProperties(serializer: Serializer): Unit = {
    serializer.setOutputProperty(Serializer.Property.METHOD, method.getOrElse("xml"))

    for (name <- List(XProcConstants._byte_order_mark,
      XProcConstants._cdata_section_elements,
      XProcConstants._doctype_public,
      XProcConstants._doctype_system,
      XProcConstants._encoding,
      XProcConstants._escape_uri_attributes,
      XProcConstants._html_version,
      XProcConstants._include_content_type,
      XProcConstants._indent,
      XProcConstants._item_separator,
      //XProcConstants._method,
      XProcConstants._normalization_form,
      XProcConstants._omit_xml_declaration,
      XProcConstants._saxon_attribute_order,
      XProcConstants._saxon_character_representation,
      XProcConstants._saxon_double_space,
      XProcConstants._saxon_indent_spaces,
      XProcConstants._saxon_line_length,
      XProcConstants._saxon_newline,
      XProcConstants._saxon_recognize_binary,
      XProcConstants._saxon_supply_source_locator,
      XProcConstants._saxon_suppress_indentation,
      XProcConstants._saxon_wrap,
      XProcConstants._standalone,
      XProcConstants._undeclare_prefixes,
      XProcConstants._version)) {
      val prop = propertyMap(name)
      val func = functionMap(name)

      if (opts.contains(name)) {
        serializer.setOutputProperty(prop, func.get)
      }
    }
  }

  def byte_order_mark: Option[String] = getBool(XProcConstants._byte_order_mark)
  def cdata_section_elements: Option[String] = getStr(XProcConstants._cdata_section_elements)
  def doctype_public: Option[String] = getStr(XProcConstants._doctype_public)
  def doctype_system: Option[String] = getStr(XProcConstants._doctype_system)
  def encoding: Option[String] = getStr(XProcConstants._encoding)
  def escape_uri_attributes: Option[String] = getBool(XProcConstants._escape_uri_attributes)
  def html_version: Option[String] = getStr(XProcConstants._html_version)
  def include_content_type: Option[String] = getBool(XProcConstants._include_content_type)
  def indent: Option[String] = getStr(XProcConstants._indent)
  def item_separator: Option[String] = getStr(XProcConstants._item_separator)
  // media_type is explicitly absent; it comes from the document properties
  def method: Option[String] = getStr(XProcConstants._method)
  def normalization_form: Option[String] = getStr(XProcConstants._normalization_form)
  def omit_xml_declaration: Option[String] = getBool(XProcConstants._omit_xml_declaration)
  def saxon_attribute_order: Option[String] = getStr(XProcConstants._saxon_attribute_order)
  def saxon_character_representation: Option[String] = getStr(XProcConstants._saxon_character_representation)
  def saxon_double_space: Option[String] = getStr(XProcConstants._saxon_double_space)
  def saxon_indent_spaces: Option[String] = getStr(XProcConstants._saxon_indent_spaces)
  def saxon_line_length: Option[String] = getStr(XProcConstants._saxon_line_length)
  def saxon_newline: Option[String] = getStr(XProcConstants._saxon_newline)
  def saxon_recognize_binary: Option[String] = getBool(XProcConstants._saxon_recognize_binary)
  def saxon_supply_source_locator: Option[String] = getBool(XProcConstants._saxon_supply_source_locator)
  def saxon_suppress_indentation: Option[String] = getStr(XProcConstants._saxon_suppress_indentation)
  def saxon_wrap: Option[String] = getBool(XProcConstants._saxon_wrap)
  def standalone: Option[String] = {
    if (opts.contains(XProcConstants._standalone)) {
      val sa = opts.getOrElse(XProcConstants._standalone, "omit").toString
      sa match {
        case "omit" => Some("omit")
        case "true" => Some("yes")
        case "yes" => Some("yes")
        case _ => Some("no")
      }
    } else {
      None
    }
  }
  def undeclare_prefixes: Option[String] = getStr(XProcConstants._undeclare_prefixes)
  def version: Option[String] = getStr(XProcConstants._version)

  // ========================================================================================================

  private def getStr(name: QName): Option[String] = {
    if (opts.contains(name)) {
      Some(opts(name).toString)
    } else {
      None
    }
  }

  private def getBool(name: QName): Option[String] = {
    if (opts.contains(name)) {
      val opt = opts(name).toString
      opt match {
        case "true" => Some("yes")
        case "yes" => Some("yes")
        case _ => Some("no")
      }
    } else {
      None
    }
  }
}
