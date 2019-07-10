package com.xmlcalabash.model.xml

import com.jafpl.graph.Node
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.MediaType
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{QName, XdmNode}

class DeclareOutput(override val config: XMLCalabashConfig) extends Port(config) {
  private var _content_types = List.empty[MediaType]

  def serialization: Map[QName,String] = Map.empty[QName,String]

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    if (attributes.contains(XProcConstants._port)) {
      _port = staticContext.parseNCName(attr(XProcConstants._port)).get
    } else {
      throw new RuntimeException("Port is required")
    }
    _sequence = staticContext.parseBoolean(attr(XProcConstants._sequence))
    _primary = staticContext.parseBoolean(attr(XProcConstants._primary))

    _content_types = staticContext.parseContentTypes(attr(XProcConstants._content_types))
    if (_content_types.isEmpty) {
      _content_types = List(MediaType.OCTET_STREAM)
    }

    _href = attr(XProcConstants._href)
    _pipe = attr(XProcConstants._pipe)

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node) {
    for (child <- allChildren) {
      child.graphEdges(runtime, parent)
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startOutput(tumble_id, tumble_id, port)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endOutput()
  }

  override def toString: String = {
    s"p:output $port${if (sequence) "*" else ""} $tumble_id"
  }
}
