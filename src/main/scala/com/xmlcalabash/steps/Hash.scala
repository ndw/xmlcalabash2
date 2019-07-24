package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{HashUtils, TypeUtils}
import net.sf.saxon.s9api.{QName, XdmNode}
import net.sf.saxon.value.QNameValue

/*
<p:declare-step type="p:hash">
  <p:input port="source" primary="true" content-types="xml html"/>
  <p:output port="result" content-types="text xml html"/>
  <p:option name="parameters" as="map(xs:QName,item()*)?"/>
  <p:option name="value" required="true" as="xs:string"/>
  <p:option name="algorithm" required="true" as="xs:QName"/>
  <p:option name="match" as="xs:string" select="'//node()'" cx:as="XSLTSelectionPattern"/>
  <p:option name="version" as="xs:string?"/>
</p:declare-step>

 */

class Hash() extends DefaultXmlStep  with ProcessMatchingNodes {
  private val _value = new QName("", "value")
  private val _algorithm = new QName("", "algorithm")
  private val _version = new QName("", "version")
  private val _crc = new QName("", "crc")
  private val _md = new QName("", "md")
  private val _sha = new QName("", "sha")
  private val cx_hmac = new QName("cx", XProcConstants.ns_cx, "hmac")

  private var source: XdmNode = _
  private var metadata: XProcMetadata = _
  private var pattern: String = _
  private var matcher: ProcessMatch = _
  private var hash: String = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    this.metadata = metadata
  }

  override def run(context: StaticContext): Unit = {
    val value = stringBinding(_value).getBytes("UTF-8")
    val algorithm = qnameBinding(_algorithm).get

    val version = if (definedBinding(_version)) {
      stringBinding(_version)
    } else {
      algorithm match {
        case `_crc` => "32"
        case `_md`  =>  "5"
        case `_sha` =>  "1"
        case _      =>   ""
      }
    }

    algorithm match {
      case `_crc` =>
        hash = HashUtils.crc(value, version, location)
      case `_md` =>
        hash = HashUtils.md(value, version, location)
      case `_sha` =>
        hash = HashUtils.sha(value, version, location)
      case `cx_hmac` =>
        if (definedBinding(XProcConstants._parameters)) {
          val key = bindings(XProcConstants._parameters)
          val map = TypeUtils.castAsScala(key).asInstanceOf[Map[Any,Any]]
          if (map.contains("accessKey")) {
            hash = HashUtils.hmac(value, version, map("accessKey").toString, location)
          } else {
            throw XProcException.xcMissingHmacKey(location)
          }
        } else {
          throw XProcException.xcMissingHmacKey(location)
        }
      case _ =>
        throw XProcException.xcBadHashAlgorithm(algorithm.toString, location)
    }

    pattern = stringBinding(XProcConstants._match)
    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    consumer.get.receive("result", matcher.result, metadata)
  }

  override def startDocument(node: XdmNode): Boolean = {
    metadata = XProcMetadata.TEXT
    matcher.addText(hash)
    false
  }

  override def startElement(node: XdmNode): Boolean = {
    matcher.addText(hash)
    false
  }

  override def endElement(node: XdmNode): Unit = {
    matcher.addEndElement()
  }

  override def endDocument(node: XdmNode): Unit = {
    matcher.endDocument()
  }

  override def allAttributes(node: XdmNode, matching: List[XdmNode]): Boolean = true

  override def attribute(node: XdmNode): Unit = {
    matcher.addAttribute(node, hash)
  }

  override def text(node: XdmNode): Unit = {
    matcher.addText(hash)
  }

  override def comment(node: XdmNode): Unit = {
    matcher.addText(hash)
  }

  override def pi(node: XdmNode): Unit = {
    matcher.addText(hash)
  }
}
