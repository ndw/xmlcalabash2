package com.xmlcalabash.model.xml

import com.jafpl.graph.Node
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.runtime.params.SelectFilterParams
import com.xmlcalabash.util.MediaType
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable.ListBuffer

class DeclareInput(override val config: XMLCalabashConfig) extends Port(config) {
  private val _exclude_result_prefixes = List.empty[String]
  protected[model] var defaultInputs: ListBuffer[AtomicStep] = ListBuffer.empty[AtomicStep]

  def exclude_result_prefixes: List[String] = _exclude_result_prefixes

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    if (attributes.contains(XProcConstants._port)) {
      _port = staticContext.parseNCName(attr(XProcConstants._port)).get
    } else {
      throw XProcException.xsMissingRequiredAttribute(XProcConstants._port, location)
    }
    _sequence = staticContext.parseBoolean(attr(XProcConstants._sequence))
    _primary = staticContext.parseBoolean(attr(XProcConstants._primary))
    _select = attr(XProcConstants._select)

    _content_types = staticContext.parseContentTypes(attr(XProcConstants._content_types))
    if (_content_types.isEmpty) {
      _content_types = List(MediaType.OCTET_STREAM)
    }

    _href = attr(XProcConstants._href)

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def validateStructure(): Unit = {
    super.validateStructure()
    if (children[Pipe].nonEmpty) {
      throw new RuntimeException("Can't have pipe in p:input")
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    for (child <- allChildren) {
      child.graphEdges(runtime, parent)
    }
  }

  def disableDefaults(): Unit = {
    for (atomic <- defaultInputs) {
      atomic.disable()
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startInput(tumble_id, tumble_id, port)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endInput()
  }

  override def toString: String = {
    s"p:input $port $tumble_id"
  }
}
