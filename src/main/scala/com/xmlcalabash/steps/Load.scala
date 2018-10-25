package com.xmlcalabash.steps

import java.net.URI

import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmMap, XdmValue}

import scala.collection.JavaConverters._
import scala.collection.mutable

class Load() extends DefaultXmlStep {
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANY

  override def run(context: StaticContext): Unit = {
    val href = if (bindings.contains(XProcConstants._href)) {
      val _href = bindings(XProcConstants._href).getStringValue
      if (context.baseURI.isDefined) {
        context.baseURI.get.resolve(_href)
      } else {
        new URI(_href)
      }
    } else {
      // This can't actually happen, but ...
      throw XProcException.xsMissingRequiredOption(XProcConstants._href, location)
    }

    // FIXME: the key type conversions here should occur centrally based on map type.

    val params = mutable.HashMap.empty[QName, XdmValue]
    if (bindings.contains(XProcConstants._parameters)) {
      val _params = bindings(XProcConstants._parameters)
      _params.value match {
        case map: XdmMap =>
          for (key <- map.keySet.asScala) {
            val value = map.get(key)
            val qname = key.getPrimitiveTypeName match {
              case XProcConstants.xs_string => new QName("", "", key.getStringValue)
              case XProcConstants.xs_QName => key.getQNameValue
              case _ => throw new IllegalArgumentException("Unexpected key type: " + key.getTypeName)
            }
            params.put(qname, value)
          }
        case _ => throw new IllegalArgumentException("Map was expected")
      }
    }

    val docprops = mutable.HashMap.empty[QName, XdmValue]
    if (bindings.contains(XProcConstants._document_properties)) {
      val _props = bindings(XProcConstants._document_properties)
      _props.value match {
        case map: XdmMap =>
          for (key <- map.keySet.asScala) {
            val value = map.get(key)
            val qname = key.getPrimitiveTypeName match {
              case XProcConstants.xs_string => new QName("", "", key.getStringValue)
              case XProcConstants.xs_QName => key.getQNameValue
              case _ => throw new IllegalArgumentException("Unexpected key type: " + key.getTypeName)
            }
            docprops.put(qname, value)
          }
        case _ => throw new IllegalArgumentException("Map was expected")
      }
    }

    val declContentType = if (bindings.contains(XProcConstants._content_type)) {
      Some(MediaType.parse(bindings(XProcConstants._content_type).getStringValue))
    } else {
      None
    }

    val dtdValidate = if (params.contains(XProcConstants._dtd_validate)) {
      if (params(XProcConstants._dtd_validate).size > 1) {
        throw new IllegalArgumentException("dtd validate parameter is not a singleton")
      } else {
        params(XProcConstants._dtd_validate).itemAt(0).getStringValue == "true"
      }
    } else {
      false
    }

    val request = new DocumentRequest(href, declContentType, location, dtdValidate)
    request.params = params.toMap
    request.docprops = docprops.toMap

    val result = config.documentManager.parse(request)

    consumer.get.receive("result", result.value, new XProcMetadata(result.contentType, result.props))
  }
}
