package com.xmlcalabash.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, FileNotFoundException, FileOutputStream, OutputStream, Writer}

import com.xmlcalabash.config.XMLCalabash
import net.sf.saxon.Configuration
import net.sf.saxon.s9api.{Axis, SAXDestination, Serializer, XdmNode, XdmNodeKind}
import nu.validator.htmlparser.sax.HtmlSerializer
import org.xml.sax.{ContentHandler, InputSource}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object S9Api {
  def documentElement(doc: XdmNode): Option[XdmNode] = {
    doc.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        val iter = doc.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val node = iter.next.asInstanceOf[XdmNode]
          if (node.getNodeKind == XdmNodeKind.ELEMENT) {
            return Some(node)
          }
        }
        None
      case XdmNodeKind.ELEMENT =>
        Some(doc)
      case _ =>
        None
    }
  }

  def inScopeNamespaces(node: XdmNode): Map[String,String] = {
    val nsiter = node.axisIterator(Axis.NAMESPACE)
    val ns = mutable.HashMap.empty[String,String]
    while (nsiter.hasNext) {
      val attr = nsiter.next().asInstanceOf[XdmNode]
      val prefix = if (attr.getNodeName == null) {
        ""
      } else {
        attr.getNodeName.toString
      }
      val uri = attr.getStringValue
      ns.put(prefix, uri)
    }
    ns.toMap
  }

  // FIXME: THIS METHOD IS A GROTESQUE HACK!
  def xdmToInputSource(config: XMLCalabash, node: XdmNode): InputSource = {
    val out = new ByteArrayOutputStream()
    val serializer = config.processor.newSerializer
    serializer.setOutputStream(out)
    serialize(config, node, serializer)
    val isource = new InputSource(new ByteArrayInputStream(out.toByteArray))
    if (node.getBaseURI != null) {
      isource.setSystemId(node.getBaseURI.toASCIIString)
    }
    isource
  }

  def serialize(config: XMLCalabash, node: XdmNode, serializer: Serializer): Unit = {
    val nodes = ListBuffer.empty[XdmNode]
    nodes += node
    serialize(config, nodes.toList, serializer)
  }

  def serialize(xproc: XMLCalabash, nodes: List[XdmNode], serializer: Serializer): Unit = {
    val qtproc = xproc.processor
    val xqcomp = qtproc.newXQueryCompiler
    xqcomp.setModuleURIResolver(xproc.moduleURIResolver)
    // Patch suggested by oXygen to avoid errors that result from attempting to serialize
    // a schema-valid document with a schema-naive query
    xqcomp.getUnderlyingStaticContext.setSchemaAware(xqcomp.getProcessor.getUnderlyingConfiguration.isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XQUERY))
    val xqexec = xqcomp.compile(".")
    val xqeval = xqexec.load
    if (xproc.htmlSerializer && "html" == serializer.getOutputProperty(Serializer.Property.METHOD)) {
      var ch: ContentHandler = null
      val outputDest = serializer.getOutputDestination
      if (outputDest == null) {
        // ???
        xqeval.setDestination(serializer)
      } else {
        outputDest match {
          case out: OutputStream =>
            ch = new HtmlSerializer(out)
            xqeval.setDestination(new SAXDestination(ch))
          case out: Writer =>
            ch = new HtmlSerializer(out)
            xqeval.setDestination(new SAXDestination(ch))
          case out: File =>
            try {
              val fos = new FileOutputStream(out)
              ch = new HtmlSerializer(fos)
              xqeval.setDestination(new SAXDestination(ch))
            } catch {
              case fnfe: FileNotFoundException =>
                xqeval.setDestination(serializer)
              case t: Throwable => throw t
            }
          case _ =>
            xqeval.setDestination(serializer)
        }
      }
    }

    for (node <- nodes) {
      xqeval.setContextItem(node)
      xqeval.run()
      // Even if we output an XML decl before the first node, we must not do it before any others!
      serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes")
    }
  }
}
