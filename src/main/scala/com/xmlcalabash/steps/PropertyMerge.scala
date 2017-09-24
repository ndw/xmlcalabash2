package com.xmlcalabash.steps

import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.{Axis, QName, XdmAtomicValue, XdmItem, XdmNode, XdmNodeKind}

import scala.collection.mutable

class PropertyMerge extends DefaultXmlStep {
  private var doc = Option.empty[Any]
  private var meta = Option.empty[XProcMetadata]
  private var prop = Option.empty[Map[QName,XdmItem]]

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(Map("source" -> "1", "properties" -> "1"),
    Map("source" -> List("*"), "properties" -> List("application/xml")))
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    if (port == "source") {
      doc = Some(item)
      meta = Some(metadata)
    } else { // port="properties"
      item match {
        case node: XdmNode =>
          val piter = node.axisIterator(Axis.CHILD)
          while (piter.hasNext) {
            val pnode = piter.next().asInstanceOf[XdmNode]
            pnode.getNodeKind match {
              case XdmNodeKind.ELEMENT =>
                if (pnode.getNodeName == XProcConstants.c_document_properties) {
                  prop = Some(extractProperties(pnode))
                } else {
                  throw new RuntimeException("Document properties must be a c:document-properties document")
                }
              case XdmNodeKind.TEXT =>
                if (pnode.getStringValue.trim != "") {
                  throw new RuntimeException("Text nodes in a properties fragment must be just whitespace")
                }
              case _ => Unit
            }
          }

        case _ => throw new RuntimeException("properties must be xml")
      }
    }
  }

  override def run(staticContext: StaticContext) {
    val newmeta = new XProcMetadata(meta.get.contentType, prop.get)
    consumer.get.receive("result", doc.get, newmeta)
  }

  private def extractProperties(node: XdmNode): Map[QName,XdmItem] = {
    val prop = mutable.HashMap.empty[QName,XdmItem]

   val piter = node.axisIterator(Axis.CHILD)
    while (piter.hasNext) {
      val pnode = piter.next().asInstanceOf[XdmNode]
      pnode.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          val name = pnode.getNodeName
          if (prop.contains(name)) {
            throw new RuntimeException("Duplicate properties are not allowed")
          }

          val vtypestr = Option(pnode.getAttributeValue(XProcConstants.xsi_type))
          val vtype = if (vtypestr.isDefined) {
            val ns = S9Api.inScopeNamespaces(pnode)
            Some(ValueParser.parseQName(vtypestr.get, ns))
          } else {
            None
          }

          var strvalue = ""
          var count = 0
          var atomic = true
          var viter = pnode.axisIterator(Axis.CHILD)
          while (viter.hasNext) {
            val vnode = viter.next().asInstanceOf[XdmNode]
            if (vnode.getNodeKind == XdmNodeKind.TEXT) {
              strvalue = vnode.getStringValue
            } else {
              atomic = false
            }
            count += 1
            atomic = atomic && count == 1
          }

          if (!atomic && vtype.isDefined) {
            throw new RuntimeException("Cannot specify xsi:type on subtrees")
          }

          if (atomic) {
            if (vtype.isDefined) {
              vtype.get match {
                case XProcConstants.xs_string =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_boolean =>
                  prop.put(name, new XdmAtomicValue(strvalue.toBoolean))
                case XProcConstants.xs_duration =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_dateTime =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_date =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_time =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_gYearMonth =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_gMonth =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_gMonthDay =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_gYear =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_gDay =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_hexBinary =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_base64Binary =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_anyURI =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_QName =>
                  prop.put(name, new XdmAtomicValue(ValueParser.parseQName(strvalue, S9Api.inScopeNamespaces(node))))
                case XProcConstants.xs_notation =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_decimal =>
                  prop.put(name, new XdmAtomicValue(strvalue.toDouble)) // FIXME: xs:decimal isn't double...
                case XProcConstants.xs_float =>
                  prop.put(name, new XdmAtomicValue(strvalue.toDouble))
                case XProcConstants.xs_double =>
                  prop.put(name, new XdmAtomicValue(strvalue.toDouble))
                case XProcConstants.xs_integer =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_nonPositiveInteger =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_negativeInteger =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_long =>
                  prop.put(name, new XdmAtomicValue(strvalue.toLong))
                case XProcConstants.xs_int =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_short =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_byte =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_nonNegativeInteger =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_positiveInteger =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_unsignedLong =>
                  prop.put(name, new XdmAtomicValue(strvalue.toLong))
                case XProcConstants.xs_unsignedInt =>
                  prop.put(name, new XdmAtomicValue(strvalue.toLong))
                case XProcConstants.xs_unsignedShort =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_unsignedByte =>
                  prop.put(name, new XdmAtomicValue(strvalue.toInt))
                case XProcConstants.xs_yearMonthDuration =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_dayTimeDuration =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_normalizedString =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_token =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_name =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_NMTOKEN =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_NCName =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_ID =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_IDREF =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_ENTITY =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case XProcConstants.xs_dateTimeStamp =>
                  prop.put(name, new XdmAtomicValue(strvalue))
                case _ =>
                  throw new RuntimeException(s"Don't understand type: ${vtype.get}")
              }
            } else {
              val x = XProcConstants._code
              prop.put(name, new XdmAtomicValue(strvalue))
            }
          } else {
            val builder = new SaxonTreeBuilder(config)
            builder.startDocument(node.getBaseURI)
            viter = node.axisIterator(Axis.CHILD)
            while (viter.hasNext) {
              val vnode = viter.next().asInstanceOf[XdmNode]
              builder.addSubtree(vnode)
            }
            builder.endDocument()
            prop.put(name, builder.result)
          }

        case XdmNodeKind.TEXT =>
          if (pnode.getStringValue.trim != "") {
            throw new RuntimeException("Text nodes in a properties fragment must be just whitespace")
          }
        case _ => Unit
      }
    }

    prop.put(XProcConstants._content_type, meta.get.properties(XProcConstants._content_type))

    // FIXME: figure out how to standardize base-uri
    if (meta.get.properties.contains(XProcConstants._base_uri)) {
      prop.put(XProcConstants._base_uri, meta.get.properties(XProcConstants._base_uri))
    }

    prop.toMap
  }
}